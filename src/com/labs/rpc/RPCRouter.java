package com.labs.rpc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Handle incoming and outgoing calls
 * @author ben
 */
public class RPCRouter {

	private Boolean killed;				// Whether this router is dead
	
	private Transport transp;			// Object transport
	private Queue<Call> outCalls;		// Outgoing calls waiting to be sent
	private Map<Long,Call> outWait;		// Outgoing calls waiting for returns
	private Queue<Call> inCalls;		// Incoming calls waiting for processing
	private Map<Long,Call> inWait;		// Incoming calls waiting for end of processing
	private RecvThread recvLoop;		// Receiving thread
	private XmitThread sendLoop;		// Sending thread
	private CallProcessor callProc;		// Processing thread for incoming calls
		
	/**
	 * Create a new router
	 * @param transport {@link Transport} - Transport to be used
	 */
	public RPCRouter(Transport transport) {
		recvLoop = new RecvThread(this);
		sendLoop = new XmitThread(this);
		callProc = new CallProcessor(this);
		transp = transport;
	}
	
	/**
	 * Start the processing loops and initialize internal states
	 */
	public void start() {
		outCalls = new Queue<Call>();
		outWait = new HashMap<Long,Call>(0);
		inCalls = new Queue<Call>();
		inWait = new HashMap<Long,Call>(0);
		recvLoop.start();
		sendLoop.start();
		callProc.start();
	}
	
	/**
	 * Stop all processing loops and flush internal states
	 */
	public void stop() {
		kill();
		try {
			outCalls.clear();
			outWait.clear();
			inCalls.clear();
			inWait.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Kill all processing loop
	 */
	private void kill() {
		synchronized(killed) {
			if (killed) {
				return;
			}
			killed = true;
		}
		try {
			callProc.interrupt();
			recvLoop.interrupt();
			sendLoop.interrupt();	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Push a remote call out
	 * @param rc {@link RemoteCall} - Call to send
	 */
	public void push(RemoteCall rc) {
		outCalls.put(new Call(rc));
	}
	
	
	/**
	 * Receiving thread
	 * @author ben
	 */
	private static class RecvThread extends Thread {
		
		private boolean on;
		private RPCRouter router;
		
		public RecvThread(RPCRouter r) {
			super("RPC receiving thread");
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
							router.inCalls.put(new Call(rc));
						} else if (dp.getType() == RemoteCallReturn.TYPE) {
							/* Process a call return */
							rcr = RemoteCallReturn.fromPacket(dp);
							if ((call = router.outWait.get(rcr.getSeq())) != null) {
								call.setReturned(rcr.getValue());
							} else {
								throw new Exception("Received return for unknown call: " + rcr.getSeq());
							}
						}
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
	 * Sending thread
	 * @author ben
	 */
	private static class XmitThread extends Thread {
		
		private boolean on;
		private RPCRouter router;
		
		public XmitThread(RPCRouter r) {
			super("RPC sending thread");
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
					if ((call = router.outCalls.get()) != null) {
						rc = call.getRemoteCall();
						/* Update call status */
						call.setPending();
						router.outWait.put(rc.getSeq(), call);
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
	 * @author ben
	 */
	private static class CallProcessor extends Thread {
		
		private boolean on;
		private RPCRouter router;
		
		public CallProcessor(RPCRouter r) {
			super("RPC call processor");
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
					if ((call = router.inCalls.get()) != null) {
						rc = call.getRemoteCall();
						/* Update call status to 'pending' */
						call.setPending();
						router.inWait.put(rc.getSeq(), call);
						/* Make the actual call */
						ret = makeCall(rc);
						/* Update call status to 'returned' */
						call.setReturned(ret);
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
			int p = rc.getMethod().lastIndexOf(".");
			if (p > 0) {
				Method method = null;
				String methodName = rc.getMethod().substring(p);
				Class<?> clazz = Class.forName(rc.getMethod().substring(0,p));
				Method instGetter = clazz.getMethod("getInstance");
				if (instGetter == null) {
					throw new Exception("Invalid call (" + clazz.getSimpleName() + " is not a RPC object)");
				}
				for (Method meth:clazz.getMethods()) {
					if (methodName.equals(meth.getName())) {
						method = meth;
						break;
					}
				}
				if (method != null) {
					if (isRPCMethod(method)) {
						Object inst = instGetter.invoke(null);
						callMethod(inst, method, rc.getArguments());
					}
					throw new Exception("Not a RPC method");
				}
				throw new Exception("Method not found");
			}
			throw new Exception("Invalid call (no class specified)");
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
		 * @return {@link Object} Return value upon success, an exception otherwise
		 */
		public static Object callMethod(Object obj, Method meth, Object[] args) {
			Object ret = null;
			try {
				ret = meth.invoke(obj,args);
			} catch (IllegalArgumentException e) {
				ret = e;
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				ret = e;
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				ret = new Exception(e.getTargetException());
				e.getTargetException().printStackTrace();
			}
			return ret;
		}
		
	}
	
}
