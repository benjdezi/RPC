package com.labs.rpc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.labs.rpc.transport.DataPacket;
import com.labs.rpc.transport.Transport;
import com.labs.rpc.util.Call;
import com.labs.rpc.util.CallBack;
import com.labs.rpc.util.Queue;
import com.labs.rpc.util.RPCMethod;
import com.labs.rpc.util.RPCObject;
import com.labs.rpc.util.RemoteException;

/**
 * Handle incoming and outgoing calls
 * @author Benjamin Dezile
 */
public class RPCRouter {

	protected static final int DEFAULT_TIMEOUT = 5;	// Default timeout for calls (in seconds)
	protected static int TIMEOUT;					// Timeout value for calls
	
	private AtomicBoolean killed;			// Whether this router is dead
	
	private Transport transp;				// Object transport
	private Map<String,RPCObject> rpcObjs;	// RPC object map: RPC Name -> Object
	private Queue<Call> outCalls;			// Outgoing calls waiting to be sent
	private Map<Long,Call> outWait;			// Outgoing calls waiting for returns
	private Queue<Call> inCalls;			// Incoming calls waiting for processing
	private Map<Long,Call> inWait;			// Incoming calls waiting for end of processing
	private RecvThread recvLoop;			// Receiving thread
	private XmitThread sendLoop;			// Sending thread
	private CallProcessor callProc;			// Processing thread for incoming calls
	private CallTimeOuter timouter;			// Call timeouter
	private CallBack onExitCallback;		// Exit callback

	/**
	 * Create a new router
	 * @param obj {@link RPCObject} - Object this router will locally apply calls to
	 * @param transport {@link Transport} - Transport to be used
	 */
	public RPCRouter(RPCObject obj, Transport transport) {
		this(new RPCObject[] {obj}, transport, null);
	}
	
	/**
	 * Create a new router
	 * @param objs {@link RPCObject}[] - Objects this router will locally apply calls to
	 * @param transport {@link Transport} - Transport to be used
	 */
	public RPCRouter(RPCObject[] objs, Transport transport) {
		this(objs, transport, null);
	}

	/**
	 * Create a new router
	 * @param obj {@link RPCObject} - Object this router will locally apply calls to
	 * @param transport {@link Transport} - Transport to be used
	 * @param onExit {@link CallBack} - Method to call when exiting (null if none)
	 */
	public RPCRouter(RPCObject obj, Transport transport, CallBack onExit) {
		this(new RPCObject[]{obj}, transport, onExit);
	}
	
	/**
	 * Create a new router
	 * @param objs {@link RPCObject}[] - Objects this router will locally apply calls to
	 * @param transport {@link Transport} - Transport to be used
	 * @param onExit {@link CallBack} - Method to call when exiting (null if none)
	 */
	public RPCRouter(RPCObject[] objs, Transport transport, CallBack onExit) {
		killed = new AtomicBoolean(true);
		recvLoop = new RecvThread(this);
		sendLoop = new XmitThread(this);
		callProc = new CallProcessor(this);
		timouter = new CallTimeOuter(this);
		transp = transport;
		rpcObjs = new HashMap<String,RPCObject>(objs.length);
		for (RPCObject obj:objs) {
			rpcObjs.put(obj.getRPCName(), obj);
		}
		onExitCallback = onExit;
	}
	
	/**
	 * Register a new target
	 * @param name {@link String} - Associated target name
	 * @param obj {@link RPCObject} - Target object
	 */
	public void registerTargetObject(String name, RPCObject obj) {
		rpcObjs.put(name, obj);
	}
	
	/**
	 * Remove a given target 
	 * @param name {@link String} - Target name
	 */
	public void unregisterTargetObject(String name) {
		rpcObjs.remove(name);
	}
	
	/**
	 * Return a given target object
	 * @param name {@link String} - Target name
	 * @return {@link RPCObject} Null if no match
	 */
	protected RPCObject getTargetObject(String name) {
		return rpcObjs.get(name);
	}
	
	/**
	 * Start the processing loops and initialize internal states
	 */
	public void start() {
		start(0);
	}
	
	/**
	 * Start the processing loops and initialize internal states
	 * @param timeout int - General timeout to use (in seconds)
	 */
	public void start(int timeout) {
		killed.set(false);
		TIMEOUT = timeout > 0 ? timeout : DEFAULT_TIMEOUT;
		outCalls = new Queue<Call>();
		outWait = new HashMap<Long,Call>(0);
		inCalls = new Queue<Call>();
		inWait = new HashMap<Long,Call>(0);
		timouter.start();
		recvLoop.start();
		sendLoop.start();
		callProc.start();
	}
	
	/**
	 * Stop all processing loops and flush internal states
	 */
	public void stop() {
		kill();
		if (outCalls != null) {
			outCalls.clear();
		}
		if (outWait != null) {
			outWait.clear();
		}
		if (inCalls != null) {
			inCalls.clear();
		}
		if (inWait != null) {
			inWait.clear();
		}
	}
	
	/**
	 * Kill all processing loop
	 */
	private void kill() {
		synchronized(killed) {
			if (killed.get()) {
				return;
			}
			killed.set(true);
		}
		callProc.interrupt();
		recvLoop.interrupt();
		sendLoop.interrupt();
		timouter.interrupt();
		transp.shutdown();		
	}
	
	/**
	 * Return whether this router is still running
	 * @return boolean
	 */
	public boolean isAlive() {
		return !killed.get();
	}
	
	/**
	 * Push a remote call out
	 * @param rc {@link RemoteCall} - Call to send
	 */
	public void push(RemoteCall rc) {
		Call call = new Call(rc);
		outCalls.offer(call);
		synchronized(outWait) {
			outWait.put(rc.getSeq(), call);
		}
	}
	
	/**
	 * Get a call return value if available
	 * @param rc {@link RemoteCall} - Call to get return for
	 * @return {@link Object}
	 * @throws IllegalStateException If not available yet
	 * @throws IllegalArgumentException If not found
	 * @throws RemoteException When something went wrong on the remote side
	 * @throws TimeoutException When the call fails to return in time
	 */
	public Object getReturn(RemoteCall rc) throws IllegalArgumentException, IllegalStateException, RemoteException, TimeoutException {
		return getReturn(rc.getSeq());
	}
	
	/**
	 * Get a call return value if available
	 * @param seq long - Call sequence number
	 * @return {@link Object}
	 * @throws IllegalStateException If not available yet
	 * @throws IllegalArgumentException If not found
	 * @throws RemoteException When something went wrong on the remote side
	 * @throws TimeoutException When the call fails to return in time
	 */
	public Object getReturn(long seq) throws IllegalArgumentException, IllegalStateException, RemoteException, TimeoutException {
		Call call;
		synchronized(outWait) {
			call = outWait.remove(seq);
		}
		if (call == null) {
			throw new IllegalArgumentException("No such call: " + seq);
		}
		if (call.isReturned()) {
			/* Got return value */
			Object ret = call.getReturnValue();
			if (ret instanceof RemoteException) {
				throw (RemoteException)ret;
			}
			return ret;
		} else if (call.isTimedOut()) {
			/* Timed out */
			throw new TimeoutException();
		}
		/* Still pending so put it back and exit */
		synchronized(outWait) {
			outWait.put(seq, call);
		}
		throw new IllegalStateException("Not returned yet");
	}

	/**
	 * Wait until the given call returns
	 * @param rc {@link RemoteCall} - Initial call
	 * @return {@link Object} Returned value
	 * @throws IllegalArgumentException If not found
	 * @throws RemoteException When something went wrong on the remote side
	 * @throws TimeoutException When the call fails to return in time
	 */
	public Object getReturnBlocking(RemoteCall rc) throws IllegalArgumentException, RemoteException, TimeoutException {
		return getReturnBlocking(rc.getSeq());
	}
	
	/**
	 * Wait until the given call returns
	 * @param seq long - Call sequence number
	 * @return {@link Object} Returned value
	 * @throws IllegalArgumentException If not found
	 * @throws RemoteException When something went wrong on the remote side
	 * @throws TimeoutException When the call fails to return in time
	 */
	public Object getReturnBlocking(long seq) throws IllegalArgumentException, RemoteException, TimeoutException {
		try {
			Call call;
			synchronized(outWait) { call = outWait.get(seq); }
			call.waitForReturn();
			return getReturn(seq);
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	
	/**
	 * Receiving thread
	 * @author Benjamin Dezile
	 */
	private static class RecvThread extends Thread {
		
		private boolean on;
		private RPCRouter router;
		
		public RecvThread(RPCRouter r) {
			super("RPC receiving thread");
			setDaemon(false);
			on = true;
			router = r;
		}
		
		public void interrupt() {
			on = false;
			super.interrupt();
		}
		
		public void run() {
			DataPacket dp;
			RemoteCall rc;
			RemoteCallReturn rcr;
			Call call;
			while(on) {
				try {
					if ((dp = router.transp.recv()) != null) {
						if (dp.getType() == RemoteCall.TYPE) {
							/* Process an incoming call */
							rc = RemoteCall.fromPacket(dp);
							Call c = new Call(rc);
							router.inCalls.offer(c);
							synchronized(router.inWait) {
								router.inWait.put(rc.getSeq(), c);
							}
						} else if (dp.getType() == RemoteCallReturn.TYPE) {
							/* Process a call return */
							rcr = RemoteCallReturn.fromPacket(dp);
							synchronized(router.outWait) {
								call = router.outWait.get(rcr.getSeq());
							}
							if (call == null) {
								Thread.sleep(2);
								/* Retrying as it may have returned before being queued */
								synchronized(router.outWait) {
									call = router.outWait.get(rcr.getSeq());
								}
							}
							if (call == null) {
								throw new Exception("Received return for unknown call: " + rcr.getSeq() + ", outWait = " + router.outWait.toString());	
							}
							if (call.isPending()) {
								call.setReturned(rcr.getValue());
							}
						}
					}
				} catch (IOException e) {
					/* Connection error, abort all */
					router.kill();
					/* Call onExit callback */
					if (router.onExitCallback != null) {
						router.onExitCallback.call();
					}
					break;
				} catch (InterruptedException e) {
					break;
				} catch (Exception e) {
					System.err.println("Error while receiving rpc data");
					e.printStackTrace();
				}
			}
		}
		
	}
	
	
	/**
	 * Sending thread
	 * @author Benjamin Dezile
	 */
	private static class XmitThread extends Thread {
		
		private boolean on;
		private RPCRouter router;
		
		public XmitThread(RPCRouter r) {
			super("RPC sending thread");
			setDaemon(false);
			on = true;
			router = r;
		}
		
		public void interrupt() {
			on = false;
			super.interrupt();
		}
		
		public void run() {
			RemoteCall rc;
			Call call;
			while (on) {
				try {
					if ((call = router.outCalls.poll()) != null) {
						rc = call.getRemoteCall();
						/* Update call status */
						call.setPending();
						/* Send remote call */
						router.transp.send(rc);
					}
				} catch (IOException e) {
					/* Connection error, abort all */
					router.kill();
					break;
				} catch (Exception e) {
					System.err.println("Error while receiving rpc data");
					e.printStackTrace();
				}
			}
		}
		
	}
	
	
	/**
	 * Processor incoming calls
	 * @author Benjamin Dezile
	 */
	private static class CallProcessor extends Thread {
		
		private boolean on;
		private RPCRouter router;
		
		public CallProcessor(RPCRouter r) {
			super("RPC call processor");
			setDaemon(false);
			on = true;
			router = r;
		}
		
		public void interrupt() {
			on = false;
			super.interrupt();
		}
		
		public void run() {
			RemoteCall rc;
			Call call;
			Object ret;
			while (on) {
				try {
					if ((call = router.inCalls.poll()) != null) {
						rc = call.getRemoteCall();
						/* Update call status to 'pending' */
						call.setPending();
						/* Make the actual call */
						ret = makeCall(rc);
						/* Remove the call from the waiting list */
						router.inWait.remove(call.getRemoteCall().getSeq());
						/* Send return value back to caller */
						router.transp.send(new RemoteCallReturn(rc, ret));
					}
				} catch (IOException e) {
					/* Connection error, abort all */
					router.kill();
					break;
				} catch (Exception e) {
					System.err.println("Error while processing incoming call");
					e.printStackTrace();
				}
			}
		}
		
		private Object makeCall(RemoteCall rc) throws Exception {
			Method method = null;
			RPCObject target = router.getTargetObject(rc.getTarget());
			if (target == null) {
				throw new NullPointerException("Target not found: " + rc.getTarget());
			}
			Class<?> clazz = target.getClass();
			for (Method meth:clazz.getMethods()) {
				if (rc.getMethod().equals(meth.getName())) {
					method = meth;
					break;
				}
			}
			if (method != null) {
				if (isRPCMethod(method)) {
					return callMethod(target, method, rc.getArguments());
				}
				throw new Exception("Not a RPC method");
			}
			throw new Exception("Method not found");
		}
		
		/**
		 * Tell whether a method is rpc
		 * @param meth {@link Method} - Method to test
		 * @return boolean
		 */
		public static boolean isRPCMethod(Method meth) {
			Annotation[] annotations = meth.getAnnotations();
			Class<?> aclazz;
			for (Annotation annotation:annotations) {
				aclazz = annotation.annotationType();
				if (aclazz.equals(RPCMethod.class)) {
					return true;
				}
			}
			return false;
		}
		
		/**
		 * Call a given method with the provided arguments
		 * @param obj {@link Object} - Object to call onto
		 * @param meth {@link Method} - Method to call
		 * @param args {@link Object}[] - Call arguments
		 * @return {@link Object} Return value upon success, a remote exception otherwise
		 */
		public static Object callMethod(Object obj, Method meth, Object[] args) {
			Object ret = null;
			try {
				ret = meth.invoke(obj,args);
			} catch (IllegalArgumentException e) {
				ret = new RemoteException(e);
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				ret = new RemoteException(e);
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				ret = new RemoteException(e.getTargetException());
				e.getTargetException().printStackTrace();
			}
			return ret;
		}
		
	}
	
	
	/**
	 * Monitor pending calls for timeouts
	 * @author Benjamin Dezile
	 */
	private static class CallTimeOuter extends Thread {
		
		private boolean on;
		private RPCRouter router;
		
		public CallTimeOuter(RPCRouter r) {
			super("RPC call timeouter");
			setDaemon(false);
			on = true;
			router = r;
		}
		
		public void interrupt() {
			on = false;
			super.interrupt();
		}
		
		public void run() {
			Call call;
			Set<Long> callSeqs;
			while (on) {
				/* Get the calls to loop over */
				synchronized(router.outWait) {
					callSeqs = new HashSet<Long>(router.outWait.keySet());
				}
				if (callSeqs.size() > 0) {
					for (Long seq:callSeqs) {
						synchronized(router.outWait) {	
							call = router.outWait.get(seq); 
						}
						if (call != null && call.getStartTime() + RPCRouter.TIMEOUT * 1000 < System.currentTimeMillis()) {
							call.setTimedOut();
						}
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
		
	}
	
}
