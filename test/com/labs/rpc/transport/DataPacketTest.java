package com.labs.rpc.transport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * Test sending and receiving data packets
 * @author Benjamin Dezile
 */
public class DataPacketTest extends TestCase {

	private static final byte TEST_TYPE = (byte)1;
	private static final byte[] TEST_DATA = "1234567890abcdefghijklmnopqrstuvwxyz".getBytes();
	
	@Test
	public void testFromBytes() {
		DataPacket dp1 = new DataPacket(TEST_TYPE, TEST_DATA);
		DataPacket dp2 = DataPacket.fromBytes(dp1.getBytes());
		assertNotNull(dp2);
		assertTrue(dp1.equals(dp2));
		
		dp1 = new DataPacket(TEST_TYPE, new byte[0]);
		dp2 = DataPacket.fromBytes(dp1.getBytes());
		assertNotNull(dp2);
		assertTrue(dp1.equals(dp2));
		
	}
	
	@Test
	public void testFromStream() throws IOException {
		DataPacket dp1 = new DataPacket(TEST_TYPE, TEST_DATA);
		ByteArrayInputStream in = new ByteArrayInputStream(dp1.getBytes());
		DataPacket dp2 = DataPacket.fromStream(in);
		assertNotNull(dp2);
		assertTrue(dp1.equals(dp2));
	}
	
}
