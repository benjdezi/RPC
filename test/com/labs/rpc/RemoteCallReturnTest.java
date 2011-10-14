package com.labs.rpc;

import org.json.*;
import java.util.*;
import junit.framework.*;
import org.junit.Test;

/**
 * Test that a remote call return can be transmitted as bytes
 * and rebuilt identically on the other side
 * @author ben
 */
public class RemoteCallReturnTest extends TestCase {

	@Test
	public void testFromBytes() {
		RemoteCall rc = new RemoteCall("methodName");
		Object[] vals = new Object[]{2, 4.5, true, null, new Object[]{"1",1,false}, new JSONObject(), new JSONArray(), new ArrayList<String>(0), new RemoteException("test")};
		for (Object val:vals) {
			try {
				RemoteCallReturn rcr1 = new RemoteCallReturn(rc, val);
				DataPacket dp = RemoteCall.fromBytes(rcr1.getBytes());
				RemoteCallReturn rcr2 = RemoteCallReturn.fromPacket(dp);
				assertNotNull(rcr2);
				assertTrue(rcr1.equals(rcr2));
			} catch (Exception e) {
				fail("There should not have been any exception: " + e.getMessage());
			}
		}
	}
	
}
