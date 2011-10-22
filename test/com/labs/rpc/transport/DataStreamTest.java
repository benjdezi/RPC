package com.labs.rpc.transport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.Test;
import junit.framework.TestCase;

/**
 * Test sending and receiving data streams
 * @author Benjamin Dezile
 */
public class DataStreamTest extends TestCase {

	private static final byte[] TEST_DATA = "1234567890abcdefghijklmnopqrstuvwxyz".getBytes(); 
	
	@Test
	public void testFromBytes() {
		DataStream ds1 = new DataStream(TEST_DATA);
		DataStream ds2 = DataStream.fromBytes(ds1.getBytes());
		assertNotNull(ds2);
		assertTrue(ds1.equals(ds2));
		
		ds1 = new DataStream(new byte[0]);
		ds2 = DataStream.fromBytes(ds1.getBytes());
		assertNotNull(ds2);
		assertTrue(ds1.equals(ds2));
		
		try {
			new DataStream(null);
			fail("This should have thrown an exception");
		} catch (NullPointerException e) {}
		
	}
	
	@Test
	public void testFromStream() throws IOException {
		
		DataStream ds1 = new DataStream(TEST_DATA);
		ByteArrayInputStream in = new ByteArrayInputStream(ds1.getBytes());
		
		DataStream ds2 = DataStream.fromStream(in);
		assertNotNull(ds2);
		assertTrue(ds1.equals(ds2));
	}
	
}
