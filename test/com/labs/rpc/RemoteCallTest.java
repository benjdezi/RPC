package com.labs.rpc;

import org.json.*;
import java.util.*;
import junit.framework.*;
import org.junit.Test;
import com.labs.rpc.transport.DataPacket;
import com.labs.rpc.util.RemoteException;

/**
 * Test that a remote call can be transmitted as bytes
 * and rebuilt identically on the other side
 * @author Benjamin Dezile
 */
public class RemoteCallTest extends TestCase {

	private static final String TEST_TARGET = "testObject";
	private static final String TEST_METHOD = "testMethod";
	private static final String TEST_DATA = "1234567890qwertyuiopasdfghjklzxcvbnm~`!@#$%^&*()_+=-{}[]\"':;<>?/.,\\|";
	private static Object[] TEST_ARGS;
	
	public void setUp() {
		List<String> l = new ArrayList<String>();
		l.add("1");
		l.add("2");
		l.add("3");
		Set<String> s = new HashSet<String>();
		s.add("1");
		s.add("2");
		s.add("3");
		Map<String,String> m = new HashMap<String,String>();
		m.put("k1", "1");
		m.put("k2", "2");
		m.put("k3", "3");
		TEST_ARGS = new Object[] {
				2, true, null, 1.4, 
				new String[]{"1","ds"}, 
				new ArrayList<String>(0), 
				new JSONObject(), 
				new JSONArray(), 
				new RemoteException("test message"),
				new byte[]{1,2,3,4,5,6,7,8},
				l,s,m,
				TEST_DATA.getBytes()};
	}
	
	@Test 
	public void testFail() {
		try {
			new RemoteCall(TEST_TARGET, TEST_METHOD, new Object[] {new Object()}).getBytes();
			fail("This should have thrown an exception (unsupported type)");
		} catch (IllegalArgumentException e) {}
	}
	
	@Test
	public void testFromPacket() {
		RemoteCall rc1 = null, rc2 = null;
		try {
			rc1 = new RemoteCall(TEST_TARGET, TEST_METHOD, TEST_ARGS);
			DataPacket dp = RemoteCall.fromBytes(rc1.getBytes());
			rc2 = RemoteCall.fromPacket(dp);
		} catch (Exception e) {
			e.printStackTrace();
			fail("There should not have been any exception: " + e.getMessage());
		}
		assertNotNull(rc2);
		assertTrue(rc1.equals(rc2));
	}
	
	@Test
	public void testEquals() {
		RemoteCall rc0 = new RemoteCall(null, null);
		RemoteCall rc1 = new RemoteCall(TEST_TARGET, TEST_METHOD);
		assertTrue(rc1.equals(rc1));
		assertTrue(rc0.equals(rc0));
		assertFalse(rc1.equals(new RemoteCall(TEST_TARGET, TEST_METHOD)));
		assertFalse(rc1.equals(rc0));
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
		long t = System.currentTimeMillis();
		RemoteCall rc = new RemoteCall(TEST_TARGET, TEST_METHOD, data.toString());
		System.out.println("Create RemoteCall = " + (System.currentTimeMillis() - t) + " ms");
		DataPacket dp = RemoteCall.fromBytes(rc.getBytes());
		for (int i=0;i<100;i++) {
			t = System.currentTimeMillis();
			RemoteCall.fromPacket(dp);
			dt += (System.currentTimeMillis() - t);
		}
		System.out.println("Avg time = " + (dt/100.0) + " ms");
		System.out.println();
	}
	
}
