package com.labs.rpc.transport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyStore;
import javax.net.ssl.*;

/**
 * Basic SSL-enabled transport implementation
 * @author Benjamin Dezile
 */
public abstract class BaseSSLTransport extends BaseTransport {

	private static SSLSocketFactory sslSocketFactory;	
	
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
	 * @param recoveryPort int - Port to use when reconnecting 
	 */
	public BaseSSLTransport(SSLSocket sock, int recoveryPort) {
		super(sock, recoveryPort);
	}
	
	/**
	 * Create a new transport instance
	 * @param sock {@link Socket} - Socket to wrap around 
	 * @param recoveryPort int - Port to use when reconnecting
	 */
	public BaseSSLTransport(Socket sock, int recoveryPort) {
		super(sock, recoveryPort);
	}
	
	/**
	 * Return the appropriate client socket factory for the given key store
	 * @param ksPath {@link String} - Path to the key store
	 * @param pwd {@link String} - Keystore password
	 * @return {@link SSLSocketFactory}
	 * @throws Exception
	 */
	protected SSLSocketFactory _getSSLSocketFactory(String ksPath, String pwd) throws Exception {
		SSLContext ctx = SSLContext.getInstance("TLS");
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		KeyStore ks = KeyStore.getInstance("JKS");
		char[] passphrase = pwd.toCharArray();
		
		ks.load(new FileInputStream(new File(ksPath)), passphrase);
		tmf.init(ks);
		ctx.init(null, tmf.getTrustManagers(), null);
		
		return ctx.getSocketFactory();
	}
	
	/**
	 * Get the appropriate client socket factory
	 * @return {@link SSLSocketFactory}
	 * @throws Exception
	 */
	protected abstract SSLSocketFactory getSSLSocketFactory() throws Exception;
	
	/**
	 * Get the appropriate server socket factory
	 * @param ksPath {@link String} - Path to the key store
	 * @param pwd {@link String} - Keystore password
	 * @return {@link SSLServerSocketFactory}
	 * @throws Exception
	 */
	protected static SSLServerSocketFactory _getSSLServerSocketFactory(String ksPath, String pwd) throws Exception {
		SSLContext ctx = SSLContext.getInstance("TLS");
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		KeyStore ks = KeyStore.getInstance("JKS");
		char[] passphrase = pwd.toCharArray();
		ks.load(new FileInputStream(new File(ksPath)), passphrase);
		kmf.init(ks, passphrase);
		ctx.init(kmf.getKeyManagers(), null, null);
		return ctx.getServerSocketFactory();
	}
	
	/**
	 * Return the appropriate server socket factory for the given key store
	 * @return {@link SSLServerSocketFactory}
	 * @throws Exception
	 */
	public static SSLServerSocketFactory getSSLServerSocketFactory() throws Exception {
		throw new Exception("Not implemented");
	};
	
	@Override
	protected boolean connect() {
		int attempts = 0;
		/* Get socket factory */
		try {
			if (sslSocketFactory == null) {
				sslSocketFactory = getSSLSocketFactory();
			}
		} catch (Exception e) {
			System.err.println("Failed to get ssl socket factory");
			e.printStackTrace();
			return false;
		}
		while (true) {
			try {
				/* Trying to connect */
				attempts++;
				if (sock != null && sock instanceof Socket) {
					sock = sslSocketFactory.createSocket(sock, address.getHostName(), port, true);
				} else {
					sock = (SSLSocket)sslSocketFactory.createSocket(address, port);
				}
				((SSLSocket)sock).startHandshake();
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
