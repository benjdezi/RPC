package com.labs.rpc.transport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Basic transport implementation
 * @author Benjamin Dezile
 */
public abstract class BaseTransport implements Transport {

	protected static final int MAX_CONNECT = 4;
	
	protected Socket sock;				// Socket
	protected BufferedInputStream bis;	// Socket input stream
	protected BufferedOutputStream bos;	// Socket output stream
	protected InetAddress address;		// Remote address
	protected int port;					// Remote port
	protected AtomicBoolean on;			// Whether it is active
	
	/**
	 * Create a new transport instance
	 * @param address {@link InetAddress} - Address to connect to
	 * @param port int - Port to connect to
	 */
	public BaseTransport(InetAddress address, int port) {
		this.sock = null;
		this.bis = null;
		this.bos = null;
		this.address = address;
		this.port = port;
		on = new AtomicBoolean(false);
		connect();
	}

	/**
	 * Create a new transport instance
	 * @param sock {@link Socket} - Socket to use 
	 */
	public BaseTransport(Socket sock, int recoveryPort) {
		this.sock = sock;
		try {
			this.bis = new BufferedInputStream(sock.getInputStream());
			this.bos = new BufferedOutputStream(sock.getOutputStream());
		} catch(IOException e) {
			e.printStackTrace();
		}
		this.address = sock.getInetAddress();
		this.port = recoveryPort;
		on = new AtomicBoolean(true);
	}
	
	/**
	 * Create an empty transport instance.<br>
	 * <i>Only to be used in subclasses</i>
	 */
	protected BaseTransport() {
		this.sock = null;
		this.address = null;
		this.port = -1;
		this.on = null;
	}
	
	/**
	 * Change the associated prot
	 * @param port int - New port
	 */
	public void setPort(int port) {
		this.port = port;
	}
	
	/**
	 * Establish connection
	 * @return boolean True upon sucess, false otherwise
	 */
	protected boolean connect() {
		int attempts = 0;
		if (port <= 0) {
			throw new IllegalArgumentException("Invalid port: " + port);
		}
		while (true) {
			try {
				/* Trying to connect */
				attempts++;
				sock = new Socket(address, port);
				bis = new BufferedInputStream(sock.getInputStream());
				bos = new BufferedOutputStream(sock.getOutputStream());
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
		
	/**
	 * Called right after connection was established
	 * @param sock {@link Socket} - Socket
	 * @throws IOException
	 */
	protected abstract void afterConnect(Socket sock) throws IOException;
	
	/**
	 * Attempt to recover this transport's connection
	 * @return boolean True upon success
	 */
	public boolean recover() {
		shutdown();
		return connect();
	}
	
	@Override
	public DataPacket recv() throws IOException {
		if (on.get()) {
			return DataPacket.fromStream(bis);
		}
		throw new IOException("Not connected");
	}

	/**
	 * Read raw data
	 * @param buffer byte[] - Buffer to fill
	 * @return
	 * @throws IOException
	 */
	public int recv(byte[] buffer) throws IOException {
		int n = buffer.length;
		int offset = 0;
		int b = 0;
		while (offset < n) {
			if ((b = bis.read(buffer, offset, n - offset)) < 0) {
				return offset;
			}
			offset += b;
		}
		return n;
	}
	
	@Override
	public void send(DataPacket dp) throws IOException {
		if (dp == null) {
			throw new NullPointerException("Invalid packet");
		}
		if (on.get()) {
			send(dp.getBytes());
		} else {
			throw new IOException("Not connected");
		}
	}

	/**
	 * Send raw data
	 * @param data byte[] - Data to be sent
	 * @throws IOException
	 */
	public void send(byte[] data) throws IOException {
		send(data, 0, data.length);
	}
	
	/**
	 * Send raw data
	 * @param data byte[] - Data to be sent
	 * @param offset int - Data offset
	 * @param length int - Amount to send
	 * @throws IOException
	 */
	public void send(byte[] data, int offset, int length) throws IOException {
		bos.write(data, offset, length);
		bos.flush();
	}
	
	@Override
	public void shutdown() {
		if (sock != null) {
			try { bis.close(); } catch (IOException e) {}
			try { bos.flush(); bos.close(); } catch (IOException e) {}
			try { sock.close(); } catch (IOException e) {}
		}
		on.set(false);
	}
	
	@Override
	public Socket getSocket() {
		return sock;
	}
	
}