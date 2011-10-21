package com.labs.rpc.transport;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import org.junit.Test;
import com.labs.rpc.DataPacket;
import com.labs.rpc.util.AsyncTask;
import junit.framework.TestCase;

/**
 * Basic transport tests
 * @author Benjamin Dezile
 */
public class BaseTransportTest extends TestCase {

	private static final String TEST_DATA = "1234567890";
	private static final String TEST_DATA2 = "abcdefghijklmnopqrstuvwxyz";
	private static final int TEST_PORT = 11113;
	private TransportImpl transp;
	private ServerSocket servSock;
	
	public void setUp() {
		/* Set server up */
		try {
			servSock = new ServerSocket(TEST_PORT);
		} catch (IOException e) {
			fail("Setup failure: " + e.getMessage());
		}
		new AsyncTask("ServerThread") {
			public void run() {
				try {
					
					Socket clientSock = servSock.accept();
					assertNotNull(clientSock);
					System.out.println();
					System.out.println("Got connection from " + clientSock.getLocalAddress() + ":" + clientSock.getPort());
					OutputStream out = clientSock.getOutputStream();
					
					DataPacket dp = new DataPacket((byte)0);
					dp.setPayload(TEST_DATA2.getBytes());
					out.write(dp.getBytes());
					System.out.println("Sent data to client");
					
					try {
						dp = DataPacket.fromStream(clientSock.getInputStream());
						System.out.println("Got data from client");
						assertNotNull(dp);
						assertEquals(new String(dp.getPayload()), TEST_DATA);
					} catch (IOException e) {
						if (!"Connection closed".equals(e.getMessage())) {
							throw e;
						}
					}
					
				} catch(IOException e) {
					e.printStackTrace();
					fail("Server error: " + e.getMessage());
				}
				
			}
		};
		/* Connect to server */
		try {
			transp = new TransportImpl(InetAddress.getLocalHost(), TEST_PORT);
			System.out.println("Connected transport to server from " + transp.sock.getLocalAddress() + ":" + transp.sock.getLocalPort());
		} catch (UnknownHostException e) {
			fail("Setup failure: " + e.getMessage());
		}
	}
	
	public void tearDown() {
		if (servSock != null && !servSock.isClosed()) {
			try { servSock.close(); } catch (IOException e) {}
		}
		if (transp != null && transp.on.get()) {
			transp.shutdown();
		}
	}
		
	@Test
	public void testSend() {
		DataPacket dp = new DataPacket((byte)0);
		dp.setPayload(TEST_DATA.getBytes());
		try {
			transp.send(dp);
			System.out.println("Sent data to server");
		} catch (IOException e) {
			fail("Failed to send data to server: " + e.getMessage());
		}
	}
	
	@Test
	public void testRecv() {
		try {
			System.out.println("Waiting to receive data from server ... ");
			DataPacket dp = transp.recv();
			System.out.println("Got data from server");
			assertNotNull(dp);
			assertEquals(new String(dp.getPayload()), TEST_DATA2);
		} catch (IOException e) {
			fail("Failed to receive data: " + e.getMessage());
		}
	}
	
	@Test 
	public void testShutdown() {
		assertTrue("Transport failed to start", transp.on.get());
		transp.shutdown();
		assertFalse("Shutdown failed", transp.on.get());
		try {
			transp.recv();
			fail("It should not allow receiving after shutdown");
		} catch (IOException e) {
			assertEquals(e.getMessage(), "Not connected");
		}
		try {
			transp.send(new DataPacket((byte)0));
			fail("It should not allow sending after shutdown");
		} catch (IOException e) {
			assertEquals(e.getMessage(), "Not connected");
		}
	}

	@Test 
	public void testRecover() {
		try {
			transp.sock.close();
		} catch (IOException e) {
			fail("Could not close socket: " + e.getMessage());
		}
		try {
			transp.recv();
			fail("This should have caused an IO exception");
		} catch (IOException e) {}
		assertTrue("Recovery failed", transp.recover());
		assertTrue("Recovery failed to restart the transport", transp.on.get());
	}
	
	private class TransportImpl extends BaseTransport {

		public TransportImpl(InetAddress address, int port) {
			super(address, port);
		}

		@Override
		protected void afterConnect(Socket sock) throws IOException {
			assertNotNull(sock);
			assertTrue(sock.isConnected());
		}
		
	}
	
}
