package com.labs.rpc;

import org.json.*;
import java.util.*;
import junit.framework.*;
import org.junit.Test;

import com.labs.rpc.util.RemoteException;

/**
 * Test that a remote call can be transmitted as bytes
 * and rebuilt identically on the other side
 * @author Benjamin Dezile
 */
public class RemoteCallTest extends TestCase {

	@Test
	public void testFromBytes() {
		String meth = "methodName";
		Object[] params = new Object[] {2, true, null, 1.4, new String[]{"1","ds"}, new ArrayList<String>(0), new JSONObject(), new JSONArray(), new RemoteException("test message")};
		try {
			RemoteCall rc1 = new RemoteCall(meth, params);
			DataPacket dp = RemoteCall.fromBytes(rc1.getBytes());
			RemoteCall rc2 = RemoteCall.fromPacket(dp);
			assertNotNull(rc2);
			assertTrue(rc1.equals(rc2));
		} catch (Exception e) {
			fail("There should not have been any exception: " + e.getMessage());
		}
		try {
			new RemoteCall(meth, new Object[] {new Object()}).getBytes();
			fail("This should have thrown an exception");
		} catch (IllegalArgumentException e) {}
	}
	
}
