package com.labs.rpc.transport;

import java.net.InetAddress;
import java.net.Socket;

/**
 * Basic SSL-enabled transport implementation
 * @author Benjamin Dezile
 */
public abstract class BaseSSLTransport extends BaseTransport {

	public BaseSSLTransport(InetAddress address, int port) {
		super(address, port);
	}

	public BaseSSLTransport(Socket sock) {
		super(sock);
	}
	
}
