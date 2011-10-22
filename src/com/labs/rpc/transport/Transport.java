package com.labs.rpc.transport;

import java.io.IOException;


/**
 * Abstract transport interface
 * @author Benjamin Dezile
 */
public interface Transport {

	/**
	 * Send a packet
	 * @param dp {@link DataPacket} - Packet to be sent
	 * @throws IOException
	 */
	public void send(DataPacket dp) throws IOException;
	
	/**
	 * Stream a series of bytes
	 * @param stream {@link DataStream} - Stream to write
	 * @throws IOException
	 */
	public void write(DataStream stream) throws IOException;
	
	/**
	 * Get the next available data packet.<br>
	 * This should block until data is received.
	 * @return {@link DataPacket}
	 * @throws IOException
	 */
	public DataPacket recv() throws IOException;
	
	/**
	 * Read a stream of data
	 * @return {@link DataStream}
	 * @throws IOException
	 */
	public DataStream read() throws IOException;
	
	/**
	 * Terminate all transport activities and release internal data
	 */
	public void shutdown();
	
}
