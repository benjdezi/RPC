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
	
}
