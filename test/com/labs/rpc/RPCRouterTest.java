package com.labs.rpc;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.io.IOException;
import junit.framework.*;
import org.junit.Test;
import com.labs.rpc.util.Queue;
import com.labs.rpc.util.RPCMethod;
import com.labs.rpc.util.RPCObject;
import com.labs.rpc.util.RemoteException;

/**
 * Test the basic functions of RPCRouter
 * @author Benjamin Dezile
 */
public class RPCRouterTest extends TestCase {

	private RPCRouter router;
	private static final Object[] callArgs = new Object[]{1, true, null, 3.56, 6L, new String[]{"1","4fk"}, new ArrayList<String>(0)};
	private static final int N_TEST = 100; 
	
	public void setUp() {
		RPCObject obj = new TestRPCObject();
		TestTransport transp = new TestTransport();
		router = new RPCRouter(obj, transp);
	}
	
	public void tearDown() {
		router.stop();
	}

	@Test
	public void testPerformance() {
		router.start();
		RemoteCall rc;
		long dt,t,tt = 0;
		/* Many sequential calls */
		for (int i=0;i<N_TEST;i++) {
			t = System.currentTimeMillis();
			rc = new RemoteCall("testMethod", true);
			router.push(rc);
			try {
				assertTrue((Boolean)router.getReturnBlocking(rc));
			} catch (IllegalArgumentException e) {
				fail("The call should exist");
			} catch (RemoteException e) {
				fail("There should not be any remote error");
			} catch (TimeoutException e) {
				fail("It should not timeout here");
			}
			dt = System.currentTimeMillis() - t;
			System.out.println("Made perf test call #" + rc.getSeq() + " and returned in " + dt + " ms");
			tt += dt;
		}
		System.out.println("Test call avg time = " + (tt / N_TEST) + " ms");
		/* Many calls in bulk */
		tt = 0;
		Set<Long> seqNums = new HashSet<Long>(N_TEST);
		for (int i=0;i<N_TEST;i++) {
			t = System.currentTimeMillis();
			rc = new RemoteCall("testMethod", true);
			router.push(rc);
			dt = System.currentTimeMillis() - t;
			System.out.println("Made perf test call #" + rc.getSeq() + " in " + dt + " ms");
			seqNums.add(rc.getSeq());
			tt += dt;
		}
		System.out.println("Push call avg time = " + (tt / N_TEST) + " ms");
		tt = 0;
		for (Long seq:seqNums) {
			t = System.currentTimeMillis();
			try {
				assertTrue((Boolean)router.getReturnBlocking(seq));
			} catch (IllegalArgumentException e) {
				fail("The call should exist");
			} catch (RemoteException e) {
				fail("There should not be any remote error");
			} catch (TimeoutException e) {
				// Timeouts are expected here...
				continue;
			}
			dt = System.currentTimeMillis() - t;
			System.out.println("Got perf test call return #" + seq + " in " + dt + " ms");
			tt += dt;
		}
		System.out.println("Get return avg time = " + (tt / N_TEST) + " ms");
	}
	
	@Test
	public void testOneCallBlocking() throws InterruptedException {
		router.start();
		RemoteCall rc = new RemoteCall("testMethod", true);
		router.push(rc);
		try {
			router.getReturnBlocking(rc);
		} catch (Exception e) {
			fail("There should not be any error here: " + e.getMessage());
		}
	}

	@Test
	public void testOneCallTimeout() throws InterruptedException {
		router.start();
		RemoteCall rc = new RemoteCall("testMethod", "timeout");
		router.push(rc);
		try {
			router.getReturnBlocking(rc);
			fail("It should have timed out");
		} catch(TimeoutException e) {
		} catch (Exception e) {
			fail("There should not be any error here: " + e.getMessage());
		}
	}
	
	@Test
	public void testRemoteException() throws InterruptedException {
		router.start();
		RemoteCall rc = new RemoteCall("testMethod", "failure");
		router.push(rc);
		try {
			router.getReturnBlocking(rc);
			fail("There should have been a remote exception");
		} catch (RemoteException e) {
		} catch (TimeoutException e) {
			fail("It should not have timed out");
		}
		
	}
	
	@Test
	public void testOneCall() throws InterruptedException {
		
		assertFalse(router.isAlive());
		router.start();
		assertTrue(router.isAlive());
		
		long t;
		
		/* Test one call for various type of call argument */
		for (Object arg:callArgs) {
			
			RemoteCall rc = new RemoteCall("testMethod", arg);
			
			/* Failure test */
			try {
				router.getReturn(rc);
				fail("It should have complained that this call does not exist");
			} catch (IllegalArgumentException e) {
			} catch (RemoteException e) {
				fail("This can't be, there's not matching call");
			} catch (TimeoutException e) {
				fail("This can't be, there's not matching call");
			}
			
			t = System.currentTimeMillis();
			System.out.print("Testing remote call for arg = " + arg + "... ");
			
			/* Make a remote call */
			router.push(rc);
			
			/* Wait for the call return */
			Object ret;
			while (true) {
				try {
					ret = router.getReturn(rc);
					if (ret != null && ret.getClass().isArray()) {
						assertTrue(Arrays.equals((Object[])ret, (Object[])arg));
					} else {
						assertEquals(ret, arg);
					}
					break;
				} catch (IllegalStateException e) {
					Thread.sleep(10);
				} catch (IllegalArgumentException e) {
					fail("The call should be registered by now!");
				} catch (RemoteException e) {
					fail("There should not be any remote error here: " + e.getMessage());
				} catch (TimeoutException e) {
					fail("This should not have timed out");
				}
			}
			
			System.out.println("Done [" + (System.currentTimeMillis() - t) + "]");
			
			/* Make sure that the call has been removed */
			try {
				router.getReturn(rc);
				fail("This should have raised an exception");
			} catch (IllegalArgumentException e) {
			} catch (Exception e) {
				fail("It should not have ended up here");
			}
			
		}
 
	}
	
	/**
	 * Test RPC object
	 * @author Benjamin Dezile
	 */
	protected class TestRPCObject implements RPCObject {
		
		@RPCMethod
		public Object testMethod(Object arg) throws Exception {
			if ("failure".equals(arg)) {
				throw new IOException("fake IO exception");
			} else if ("timeout".equals(arg)) {
				try {
					Thread.sleep(2*RPCRouter.TIMEOUT*1000);
				} catch (InterruptedException e) {}
			}
			return arg;
		}
		
		@RPCMethod
		public void testMethod2(Object arg1, Object arg2) {
			return;
		}
	
	}
	
	/**
	 * Test transport
	 * @author Benjamin Dezile
	 */
	protected static class TestTransport implements Transport {

		private Queue<byte[]> q;
		
		public TestTransport() {
			q = new Queue<byte[]>();
		}
		
		@Override
		public DataPacket recv() throws IOException {
			byte[] data = q.get();
			try {
				if (data != null) {
					try {
						/* Simulate network delay */
						Thread.sleep(10);
						return DataPacket.fromBytes(data);
					} catch (InterruptedException e) {}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void send(DataPacket dp) throws IOException {
			q.offer(dp.getBytes());
			try {
				/* Simulate network delay */
				Thread.sleep(10);
			} catch (InterruptedException e) {}
		}

		@Override
		public void shutdown() {
			q.clear();
		}
		
	}
	
}
