package com.labs.rpc.transport;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import javax.net.ssl.*;

/**
 * Basic SSL-enabled transport implementation
 * @author Benjamin Dezile
 */
public abstract class BaseSSLTransport extends BaseTransport {

	private static final SSLSocketFactory sslSocketFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
	
	private SSLSocket sock;
	
	/**
	 * Create a new transport instance
	 * @param address {@link InetAddress} - Address to connect to
	 * @param port int - Port to connect to
	 */
	public BaseSSLTransport(InetAddress address, int port) {
		super(address, port);
	}

	/**
	 * Create a new transport instance
	 * @param sock {@link SSLSocket} - Socket to use 
	 */
	public BaseSSLTransport(SSLSocket sock) {
		super(sock);
	}
	
	/**
	 * Create a new transport instance
	 * @param sock {@link Socket} - Socket to wrap around 
	 */
	public BaseSSLTransport(Socket sock) {
		super(sock);
	}
	
	@Override
	protected boolean connect() {
		int attempts = 0;
		while (true) {
			try {
				/* Trying to connect */
				attempts++;
				if (sock != null && sock instanceof Socket) {
					sslSocketFactory.createSocket(sock, address.getHostName(), port, true);
				} else {
					sock = (SSLSocket)sslSocketFactory.createSocket(address, port);
				}
				afterConnect(sock);
				on.set(true);
				return true;
			} catch (UnknownHostException e) {
				System.err.println("Unknown host: " + address);
			} catch (IOException e) {
				System.err.println("Could not connect: " + e.getMessage());
			}
			if (attempts == MAX_CONNECT) {
				/* Failed to connect */
				sock = null;
				break;
			}
			/* Exponential backoff */
			try {
				Thread.sleep((long)(1000*Math.pow(2, attempts-1)));
			} catch(InterruptedException e) {
				return false;
			}
		}
		return false;
	}
	
}
