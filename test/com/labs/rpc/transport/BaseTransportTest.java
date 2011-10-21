package com.labs.rpc.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.Test;
import com.labs.rpc.util.CallBack;
import com.labs.rpc.util.AsyncTask;
import junit.framework.TestCase;

/**
 * Basic transport tests
 * @author Benjamin Dezile
 */
public class BaseTransportTest extends TestCase {

	private static final String TEST_DATA = "1234567890";
	private static final int TEST_PORT = 11112;
	
	@Test
	public void testConnect() {
		CallBack onAccept = new CallBack(){
			public void call(Object... params){
				assertNotNull(params);
				assertTrue(params.length > 0);
				Socket clientSock = (Socket)params[0];
				byte[] buffer = new byte[TEST_DATA.length()];
				try {
					System.out.println("Waiting for data...");
					clientSock.getInputStream().read(buffer);
					System.out.println("Got data");
					assertEquals(new String(buffer), TEST_DATA);
				} catch (IOException e) {
					fail("There should not have been an error here: " + e.getMessage());
				}
			}
		};
		new AsyncTask("waitForConnection", onAccept) {
			public void run() {
				ServerSocket servSock = null;
				try {
					servSock = new ServerSocket(TEST_PORT);
					System.out.println("Created server socket");
				} catch (IOException e) {
					fail("Humm! Could not create server socket: " + e.getMessage());
				}
				try {
					System.out.println("Waiting for connection");
					Socket clientSock = servSock.accept();
					assertNotNull(clientSock);
					((CallBack)params[0]).call(clientSock);
				} catch (IOException e) {
					fail("Error while waiting for connection: " + e.getMessage());
				}
			}
		};
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e1) {
			fail("This should not be interrupted, what the heck is going on!!");
		}
		Socket sock = null;
		try {
			sock = new Socket(InetAddress.getLocalHost(), TEST_PORT);
			sock.getOutputStream().write(TEST_DATA.getBytes());
			System.out.println("Sent data");
		} catch (Exception e) {
			fail("Could not send data: " + e.getMessage());
		}
		
	}
	
}
