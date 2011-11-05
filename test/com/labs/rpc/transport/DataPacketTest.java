package com.labs.rpc.transport;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import org.junit.Test;
import com.labs.rpc.util.AsyncTask;
import junit.framework.TestCase;

/**
 * Test sending and receiving data packets
 * @author Benjamin Dezile
 */
public class DataPacketTest extends TestCase {

	private static final int TEST_PORT = 11111;
	private static final byte TEST_TYPE = (byte)1;
	private static final byte[] TEST_DATA = "1234567890abcdefghijklmnopqrstuvwxyz".getBytes();
		
	@Test
	public void testFromBytes() {
		DataPacket dp1 = new DataPacket(TEST_TYPE, TEST_DATA);
		DataPacket dp2 = DataPacket.fromBytes(dp1.getBytes());
		assertNotNull(dp2);
		assertTrue(dp1.equals(dp2));
		dp1 = new DataPacket(TEST_TYPE, new byte[0]);
		
		long t = System.nanoTime();
		dp2 = DataPacket.fromBytes(dp1.getBytes());
		System.out.println("fromBytes = " + ((System.nanoTime() - t)/(1000*1000.0)) + " ms");
		
		assertNotNull(dp2);
		assertTrue(dp1.equals(dp2));
		
	}
	
	@Test
	public void testFromStream() throws IOException {
		DataPacket dp1 = new DataPacket(TEST_TYPE, TEST_DATA);
		ByteArrayInputStream in = new ByteArrayInputStream(dp1.getBytes());
		
		long t = System.nanoTime();
		DataPacket dp2 = DataPacket.fromStream(in);
		System.out.println("fromStream = " + ((System.nanoTime() - t)/(1000*1000.0)) + " ms");
		
		assertNotNull(dp2);
		assertTrue(dp1.equals(dp2));
	}

	@Test 
	public void testFromChannel() throws IOException {
		
		DataPacket dp1 = new DataPacket(TEST_TYPE, TEST_DATA);
		
		new AsyncTask("Client Thread", dp1) {

			@Override
			public void run() {
				try {
					Thread.sleep(500);
					SocketChannel channel = SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), TEST_PORT));
					System.out.println("Connected to server socket");
					byte[] bytes = ((DataPacket)params[0]).getBytes();
					channel.write(ByteBuffer.wrap(bytes));
					System.out.println("Sent data to server");
				} catch (IOException e) {
					e.printStackTrace();
					fail("Server side exception");
				} catch (InterruptedException e) {
					return;
				}	
			}
			
		};
		
		ServerSocketChannel servChannel = ServerSocketChannel.open();
		servChannel.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(), TEST_PORT));
		SocketChannel channel = servChannel.accept();
		System.out.println("Got client connection");
		
		long t = System.nanoTime();
		DataPacket dp2 = DataPacket.fromChannel(channel);
		System.out.println("fromChannel = " + ((System.nanoTime() - t)/(1000*1000.0)) + " ms");
		System.out.println("Got data from client");
		
		assertNotNull(dp2);
		assertTrue(dp1.equals(dp2));
	}
		
	
	@Test 
	public void testSpeed() throws Exception {
		benchmark(1);
		benchmark(10);
		benchmark(100);
		benchmark(1000);
		benchmark(10000);
		benchmark(100000);
		benchmark(500000);
	}
	
	public void benchmark(int n) throws Exception {
		StringBuffer buffer = new StringBuffer();
		for (int i=0;i<n;i++) {
			buffer.append(new String(TEST_DATA));
		}
		byte[] data = buffer.toString().getBytes();
		System.out.println();
		System.out.println("Benchmark for DataPacket with " + (buffer.length() / 1000.0) + " KB");
		long t = System.currentTimeMillis();
		DataPacket dp = new DataPacket(TEST_TYPE, data);
		System.out.println("Create DataPacket = " + (System.currentTimeMillis() - t) + " ms");
		t = System.currentTimeMillis();
		byte[] bytes = dp.getBytes();
		System.out.println("getBytes = " + (System.currentTimeMillis() - t) + " ms");
		long dt1 = 0, dt2 = 0;
		ByteArrayInputStream in;
		for (int i=0;i<100;i++) {
			t = System.currentTimeMillis();
			DataPacket.fromBytes(bytes);
			dt1 += (System.currentTimeMillis() - t);
			in = new ByteArrayInputStream(bytes);
			t = System.currentTimeMillis();
			DataPacket.fromStream(in);
			dt2 += (System.currentTimeMillis() - t);
		}
		System.out.println("fromBytes  = " + (dt1/100.0) + " ms");
	}
	
}
