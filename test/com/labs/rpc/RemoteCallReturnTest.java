package com.labs.rpc;

import org.json.*;
import java.util.*;
import junit.framework.*;
import org.junit.Test;
import com.labs.rpc.transport.DataPacket;
import com.labs.rpc.util.RemoteException;

/**
 * Test that a remote call return can be transmitted as bytes
 * and rebuilt identically on the other side
 * @author Benjamin Dezile
 */
public class RemoteCallReturnTest extends TestCase {

	private static final String TEST_TARGET = "testObject";
	private static final String TEST_METHOD = "testMethod";
	private static final String TEST_DATA = "1234567890qwertyuiopasdfghjklzxcvbnm~`!@#$%^&*()_+=-{}[]\"':;<>?/.,\\|";
	
	@Test
	public void testFromPacket() {
		RemoteCall rc = new RemoteCall(TEST_TARGET, TEST_METHOD);
		assertEquals(rc.getSeq(), 1);
		Object[] vals = new Object[]{2, 4.5, true, null, new Object[]{"1",1,false}, new JSONObject(), new JSONArray(), new ArrayList<String>(0), new RemoteException("test")};
		RemoteCallReturn rcr1 = null, rcr2 = null;
		for (Object val:vals) {
			try {
				rcr1 = new RemoteCallReturn(rc, val);
				assertEquals(rcr1.getSeq(), rc.getSeq());
				DataPacket dp = RemoteCall.fromBytes(rcr1.getBytes());
				rcr2 = RemoteCallReturn.fromPacket(dp);
			} catch (Exception e) {
				fail("There should not have been any exception: " + e.getMessage());
			}
			assertNotNull(rcr2);
			assertEquals(rcr2.getSeq(), rc.getSeq());
			assertTrue(rcr1.equals(rcr2));
		}
	}
	
	@Test
	public void testEquals() {
		RemoteCall rc = new RemoteCall(TEST_TARGET, TEST_METHOD);
		RemoteCallReturn rcr0 = new RemoteCallReturn(null, null);
		RemoteCallReturn rcr1 = new RemoteCallReturn(rc, "0");
		RemoteCallReturn rcr2 = new RemoteCallReturn(rc, "0");
		assertTrue(rcr0.equals(rcr0));
		assertTrue(rcr1.equals(rcr1));
		assertTrue(rcr1.equals(rcr2));
		assertFalse(rcr1.equals(rcr0));
	}
	
	@Test 
	public void testSpeed() throws Exception {
		benchmark(1);
		benchmark(10);
		benchmark(100);
		benchmark(1000);
		benchmark(10000);
		benchmark(100000);
	}
	
	public void benchmark(int n) throws Exception {
		StringBuffer data = new StringBuffer();
		for (int i=0;i<n;i++) {
			data.append(TEST_DATA);
		}
		System.out.println("Benchmark for fromPacket with " + (data.length() / 1000.0) + " KB");
		long dt = 0;
		RemoteCall rc = new RemoteCall(TEST_TARGET, TEST_METHOD);
		long t = System.currentTimeMillis();
		RemoteCallReturn rcr = new RemoteCallReturn(rc, data.toString());
		System.out.println("Create RemoteCallReturn = " + (System.currentTimeMillis() - t) + " ms");
		DataPacket dp = RemoteCall.fromBytes(rcr.getBytes());
		for (int i=0;i<100;i++) {
			t = System.currentTimeMillis();
			RemoteCallReturn.fromPacket(dp);
			dt += (System.currentTimeMillis() - t);
		}
		System.out.println("Avg time = " + (dt/100.0) + " ms");
		System.out.println();
	}
	
}
