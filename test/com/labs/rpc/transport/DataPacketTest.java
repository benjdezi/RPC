package com.labs.rpc.transport;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
	private static final String TEST_DATA_BASE = "1234567890abcdefghijklmnopqrstuvwxyz";
	private static final String LOCAL_IP = "192.168.1.107";
	private byte[] TEST_DATA;	
	
	public void setUp() {
		StringBuffer buffer = new StringBuffer();
		for (int i=0;i<1024*50;i++) {
			buffer.append(TEST_DATA_BASE);
		}
		TEST_DATA = buffer.toString().getBytes();
	}
	
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
		
		new AsyncTask("Client Thread", dp1) {

			@Override
			public void run() {
				try {
					Thread.sleep(500);
					Socket sock = new Socket(LOCAL_IP, TEST_PORT);
					long t = System.currentTimeMillis();
					byte[] bytes = ((DataPacket)params[0]).getBytes();
					sock.getOutputStream().write(bytes);
					System.out.println("Sent packet = " + (System.currentTimeMillis() - t) + " ms");
				} catch (IOException e) {
					e.printStackTrace();
					fail("Server side exception");
				} catch (InterruptedException e) {
					return;
				}	
			}
			
		};
				
		ServerSocket servSock = new ServerSocket(TEST_PORT);
		Socket sock = servSock.accept();
		
		BufferedInputStream bis = new BufferedInputStream(sock.getInputStream());
		long t = System.nanoTime();
		DataPacket dp2 = DataPacket.fromStream(bis);
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
					SocketChannel channel = SocketChannel.open(new InetSocketAddress(LOCAL_IP, TEST_PORT));
					byte[] bytes = ((DataPacket)params[0]).getBytes();
					channel.write(ByteBuffer.wrap(bytes));
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
		
		long t = System.nanoTime();
		DataPacket dp2 = DataPacket.fromChannel(channel);
		System.out.println("fromChannel = " + ((System.nanoTime() - t)/(1000*1000.0)) + " ms");
		
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
			buffer.append(TEST_DATA_BASE);
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
			DataPacket.fromStream(new BufferedInputStream(in));
			dt2 += (System.currentTimeMillis() - t);
		}
		System.out.println("fromBytes  = " + (dt1/100.0) + " ms");
	}
	
}
