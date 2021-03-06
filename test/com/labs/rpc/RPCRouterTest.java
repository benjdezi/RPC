package com.labs.rpc;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.io.IOException;
import java.net.Socket;
import junit.framework.*;
import org.junit.Test;
import com.labs.rpc.transport.DataPacket;
import com.labs.rpc.transport.Transport;
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
	private static final String TEST_TARGET = "testRPCObj";
	private static final String TEST_METHOD = "testMethod";
	
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
		System.out.println("\nTEST PERFORMANCE");
		router.start();
		RemoteCall rc;
		long dt,t,tt = 0;
		/* Many sequential calls */
		for (int i=0;i<N_TEST;i++) {
			t = System.currentTimeMillis();
			rc = new RemoteCall(TEST_TARGET, TEST_METHOD, true);
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
			rc = new RemoteCall(TEST_TARGET, TEST_METHOD, true);
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
		System.out.println("\nTEST ONE CALL BLOCKING");
		router.start();
		RemoteCall rc = new RemoteCall(TEST_TARGET, TEST_METHOD, true);
		router.push(rc);
		try {
			router.getReturnBlocking(rc);
		} catch (Exception e) {
			fail("There should not be any error here: " + e.getMessage());
		}
	}

	@Test
	public void testOneCallTimeout() throws InterruptedException {
		System.out.println("\nTEST ONE CALL TIMEOUT");
		router.start();
		RemoteCall rc = new RemoteCall(TEST_TARGET, TEST_METHOD, "timeout");
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
		System.out.println("\nTEST REMOTE EXCEPTIONS");
		router.start();
		
		RemoteCall rc = new RemoteCall(TEST_TARGET, TEST_METHOD, "failure");
		router.push(rc);
		try {
			router.getReturnBlocking(rc);
			fail("There should have been a remote exception");
		} catch (RemoteException e) {
		} catch (TimeoutException e) {
			fail("It should not have timed out");
		}
		
		rc = new RemoteCall(TEST_TARGET, TEST_METHOD, true, false);
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
		
		System.out.println("\nTEST ONE CALL");
		
		assertFalse(router.isAlive());
		router.start();
		assertTrue(router.isAlive());
		
		long t;
		
		/* Test one call for various type of call argument */
		for (Object arg:callArgs) {
			
			RemoteCall rc = new RemoteCall(TEST_TARGET, TEST_METHOD, arg);
			
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
		
	@Test
	public void testVoidReturn() {
		System.out.println("\nTEST VOID RETURN");
		router.start();
		RemoteCall rc = new RemoteCall(TEST_TARGET, "voidTestMethod");
		router.push(rc);
		try {
			Object ret = router.getReturnBlocking(rc);
			assertNotNull(ret);
			assertEquals(ret, RPCRouter.VOID);
		} catch (TimeoutException e) {
			fail("This should not have timed out");
		} catch (Exception e) {
			fail("There should not be any error here: " + e.getMessage());
		}
	}
	
	@Test
	public void testStartStop() {
		RemoteCall rc1 = new RemoteCall(TEST_TARGET, TEST_METHOD, 1);
		RemoteCall rc2 = new RemoteCall(TEST_TARGET, TEST_METHOD, 2);
		/* Restart with flushing */
		router.start();
		assertTrue(router.isAlive());
		router.stop();
		assertFalse(router.isAlive());
		router.start();
		assertTrue(router.isAlive());
		/* Restart witht no flushing */
		router.push(rc1);
		router.stop(false);
		assertTrue(router.hasCall(rc1.getSeq()));
		assertFalse(router.isAlive());
		router.push(rc2);
		router.start();
		assertTrue(router.isAlive());
		assertTrue(router.hasCall(rc2.getSeq()));
		try {
			Object ret = router.getReturnBlocking(rc1);
			assertNotNull(ret);
			assertEquals(ret, 1);
			ret = router.getReturnBlocking(rc2);
			assertNotNull(ret);
			assertEquals(ret, 2);
		} catch (TimeoutException e) {
			fail("This should not have timed out");
		} catch (Exception e) {
			e.printStackTrace();
			fail("There should not be any error here: " + e.getMessage());
		}
		/* Double start */
		router.start();
		router.start();
		assertTrue(router.isAlive());
		/* Double stop */
		router.stop();
		router.stop();
		assertFalse(router.isAlive());		
	}
	
	@Test
	public void testMultiTarget() {
		router.start();
		router.registerTargetObject("obj1", new TestRPCObject());
		router.registerTargetObject("obj2", new TestRPCObject());
		RemoteCall rc1 = new RemoteCall("obj1", "testMethod", 1);
		router.push(rc1);
		try {
			Object ret = router.getReturnBlocking(rc1);
			assertNotNull(ret);
			assertEquals(ret, 1);
		} catch (TimeoutException e) {
			fail("This should not have timed out");
		} catch (Exception e) {
			fail("There should not be any error here: " + e.getMessage());
		}
		RemoteCall rc2 = new RemoteCall("obj2", "testMethod", 2);
		router.push(rc2);
		try {
			Object ret = router.getReturnBlocking(rc2);
			assertNotNull(ret);
			assertEquals(ret, 2);
		} catch (TimeoutException e) {
			fail("This should not have timed out");
		} catch (Exception e) {
			fail("There should not be any error here: " + e.getMessage());
		}
	}
	
	
	/**
	 * Test RPC object
	 * @author Benjamin Dezile
	 */
	protected class TestRPCObject implements RPCObject {
		
		@Override
		public String getRPCName() {
			return TEST_TARGET;
		}
		
		@RPCMethod
		public void voidTestMethod() {
			return;
		}
		
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
			if (q != null) {
				q.clear();
			}
			q = null;
		}

		@Override
		public boolean recover() {
			if (q == null) {
				q = new Queue<byte[]>();
			}
			return true;
		}
		
		@Override
		public Socket getSocket() {
			throw new IllegalStateException("Not applicable in this context");
		}
		
	}
	
}
