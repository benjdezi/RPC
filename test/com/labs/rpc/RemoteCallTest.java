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
	private static final Object[] TEST_ARGS = new Object[] {2, true, null, 1.4, new String[]{"1","ds"}, new ArrayList<String>(0), new JSONObject(), new JSONArray(), new RemoteException("test message")};
	
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
	
}
