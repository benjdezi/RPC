package com.labs.rpc;

import java.io.InputStream;
import java.util.Arrays;

/**
 * Native data packet implementation
 * @author Benjamin Dezile
 */
public class CDataPacket {

	static {
		System.loadLibrary("cdatapacket");
	}
	
	private static Long seqCounter = 0L;	// Sequence counter
	protected byte type;					// Packet type
	protected long seq;						// Sequence number
	protected long time;					// Creation timestamp
	protected byte[] payload;				// Encapsulated data
	
	/**
	 * Create a new data packet
	 * @param t byte - Packet type
	 */
	public CDataPacket(byte t) {
		this(t, getNextSeq());
	}
	
	/**
	 * Create an empty data packet
	 */
	protected CDataPacket() {
		this((byte)0,0);
	}
	
	/**
	 * Create a new data packet
	 * @param seqNum long - Sequence number
	 * @param t byte - Packet type
	 */
	protected CDataPacket(byte t, long seqNum) {
		type = t;
		seq = seqNum;
		time = System.currentTimeMillis();
	}
	
	/**
	 * Get the next sequence number
	 * @return long
	 */
	private static final long getNextSeq() {
		synchronized(seqCounter) {
			return ++seqCounter;
		}
	}
	
	/**
	 * Get the associated sequence number
	 * @return long
	 */
	public long getSeq() {
		return seq;
	}
	
	/**
	 * Get the associated timestamp
	 * @return long
	 */
	public long getTime() {
		return time;
	}
	
	/**
	 * Get the packet type
	 * @return byte
	 */
	public byte getType() {
		return type;
	}
	
	/**
	 * Pack an object so that it can be transported
	 * @param arg {@link Object} - Argument
	 * @return byte[]
	 */
	native protected static byte[] packObject(Object arg);

	/**
	 * Unpack an object
	 * @param argData {@link String} - Data
	 * @return {@link Object}
	 */
	native protected static Object unpackObject(String argData) throws Exception;

	/**
	 * Make the header bytes
	 * @param pl int - Payload size 
	 * @return byte[]
	 */
	native protected byte[] makeHeaderBytes(int pl);
	
	/**
	 * Make the packet bytes
	 * @param header byte[] - Header bytes
	 * @param payload byte[] - Payload bytes
	 * @return byte[]
	 */
	native protected byte[] makePacketBytes(byte[] header, byte[] payload);
		
	/**
	 * Get the packet bytes to send over
	 * @return byte[]
	 */
	native public byte[] getBytes();
	
	/**
	 * Build a new packet object from raw data
	 * @param bytes byte[] - Bytes
	 * @return {@link CDataPacket}
	 * @throws Exception
	 */
	native public static CDataPacket fromBytes(byte[] bytes) throws Exception;
	
	/**
	 * Read a new packet object from a byte stream
	 * @param in {@link InputStream} - Input stream
	 * @return {@link CDataPacket}
	 * @throws Exception
	 */
	native public CDataPacket fromStream(InputStream in) throws Exception;

	/**
	 * Assert that the given object is the same as this packet
	 */
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		CDataPacket dp = (CDataPacket)o;
		if (type != dp.type) {
			return false;
		}
		if (time != dp.time) {
			return false;
		}
		if (seq != dp.seq) {
			return false;
		}
		if (!Arrays.equals(payload, dp.payload)) {
			return false;
		}
		return true;
	}
	
	public static void main(String[] args) {}
	
}
