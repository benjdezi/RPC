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
	 * Get the next available data packet.<br>
	 * This should block until data is received.
	 * @return {@link DataPacket}
	 * @throws IOException
	 */
	public DataPacket recv() throws IOException;
		
	/**
	 * Terminate all transport activities and clear internal states
	 */
	public void shutdown();
	
	/**
	 * Restart all activities and recover the connection
	 */
	public boolean recover();
	
}
