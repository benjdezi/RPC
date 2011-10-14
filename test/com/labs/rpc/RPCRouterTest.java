package com.labs.rpc;

import java.util.*;
import java.io.IOException;
import junit.framework.*;
import org.junit.Test;
import com.labs.rpc.util.Queue;
import com.labs.rpc.util.RPCMethod;
import com.labs.rpc.util.RPCObject;
import com.labs.rpc.util.RemoteException;

/**
 * Test the basic functions of RPCRouter
 * @author ben
 */
public class RPCRouterTest extends TestCase {

	private RPCRouter router;
	private static final Object[] callArgs = new Object[]{1, true, null, 3.56, 6L, new String[]{"1","4fk"}, new ArrayList<String>(0)};
	
	public void setUp() {
		RPCObject obj = new TestRPCObject();
		TestTransport transp = new TestTransport();
		router = new RPCRouter(obj, transp);
	}
	
	public void tearDown() {
		router.stop();
	}

	@Test
	public void testOneCallBlocking() throws InterruptedException {
		// TODO: write test for getReturnBlocking
	}

	@Test
	public void testRemoteException() throws InterruptedException {
		// TODO: write test for a call that would cause a remote error
	}
	
	@Test
	public void testOneCall() throws InterruptedException {
		
		assertFalse(router.isAlive());
		router.start();
		assertTrue(router.isAlive());
		
		/* Test one call for various type of call argument */
		for (Object arg:callArgs) {
			
			RemoteCall rc = new RemoteCall("testMethod", arg);
			
			/* Failure test */
			try {
				router.getReturn(rc);
				fail("It should have complained that this call does not exist");
			} catch (IllegalArgumentException e) {
			} catch (RemoteException e) {}
			
			/* Make a remote call */
			router.push(rc);
			
			/* Wait so that we're shure the call is in outWait */
			Thread.sleep(500);
			
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
					Thread.sleep(100);
				} catch (IllegalArgumentException e) {
					fail("The call should be registered by now!");
				} catch (RemoteException e) {
					fail("There should not be any remote error here: " + e.getMessage());
				}
			}
		
		}
 
	}
	
	/**
	 * Test RPC object
	 * @author ben
	 */
	protected class TestRPCObject implements RPCObject {
		
		@RPCMethod
		public Object testMethod(Object arg) throws Exception {
			if ("failure".equals(arg)) {
				throw new Exception("test failure");
			}
			return arg;
		}
	
	}
	
	/**
	 * Test transport
	 * @author ben
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
					return DataPacket.fromBytes(data);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void send(DataPacket dp) throws IOException {
			q.put(dp.getBytes());
		}

		@Override
		public void shutdown() {
			q.clear();
		}
		
	}
	
}
