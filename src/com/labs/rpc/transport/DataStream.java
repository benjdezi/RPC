package com.labs.rpc.transport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

// TODO: Write unit test for DataStream

/**
 * Implements a stream of bytes
 * @author Benjamin Dezile
 */
public class DataStream {

	private static final int HEADER_SIZE = 4;
	
	protected int length;		// Length of the data being stream
	protected byte[] data;		// Stream data
	
	/**
	 * Make a new data stream
	 * @param bytes byte[] - Stream data
	 */
	public DataStream(byte[] bytes) {
		data = bytes;
	}
	
	/**
	 * Return the length of the stream
	 * @return int
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * 
	 * @return
	 */
	public byte[] getData() {
		return data;
	}
	
	/**
	 * Get the stream bytes (including header)
	 * @return byte[]
	 */
	public byte[] getBytes() {
		ByteBuffer buf = ByteBuffer.allocate(HEADER_SIZE + data.length);
		buf.putInt(length);
		buf.put(data);
		return buf.array();
	}
	
	/**
	 * Make a data stream object from bytes
	 * @param bytes byte[] - Bytes to read object from
	 * @return {@link DataStream}
	 */
	public static DataStream fromBytes(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		int length = buf.getInt(0);
		int diff = bytes.length - (HEADER_SIZE + length);
		if (diff > 0) {
			throw new IllegalArgumentException("Size does not match: " + diff + " bytes missing");
		}
		byte[] buffer = new byte[length];
		buf.get(buffer, HEADER_SIZE, length);
		return new DataStream(buffer);
	}
	
	/**
	 * Read a data stream object from a stream of bytes
	 * @param in {@link InputStream} - Stream to read from
	 * @return {@link DataStream}
	 * @throws IOException
	 */
	public static DataStream fromStream(InputStream in) throws IOException {
		byte[] header = new byte[HEADER_SIZE];
		int b,n = 0;
		while (n < HEADER_SIZE) {
			if ((b=in.read(header, n, HEADER_SIZE - n)) < 0) {
				throw new IOException("Connection closed");
			}
			n += b;
		}
		int length = ByteBuffer.wrap(header).getInt(0);
		n = 0;
		byte[] data = new byte[length];
		while (n < length) {
			if ((b=in.read(data, n, HEADER_SIZE - n)) < 0) {
				throw new IOException("Connection closed");
			}
			n += b;
		}
		return new DataStream(data);
	}
	
}
