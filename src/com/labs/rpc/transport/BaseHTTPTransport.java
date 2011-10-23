package com.labs.rpc.transport;

import java.net.InetAddress;
import java.net.Socket;

/**
 * Basic SSL-enabled transport implementation
 * @author Benjamin Dezile
 */
public abstract class BaseHTTPTransport extends BaseTransport {

	public BaseHTTPTransport(InetAddress address, int port) {
		super(address, port);
	}

	public BaseHTTPTransport(Socket sock) {
		super(sock);
	}
	
}
