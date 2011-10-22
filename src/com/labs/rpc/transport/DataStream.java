package com.labs.rpc.transport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Implements a stream of bytes
 * @author Benjamin Dezile
 */
public class DataStream {

	private static final int HEADER_SIZE = 4;
	
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
		return data != null ? data.length : 0;
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
		buf.putInt(getLength());
		if (data != null) {
			buf.put(data);
		}
		return buf.array();
	}
	
	/**
	 * Make a data stream object from bytes
	 * @param bytes byte[] - Bytes to read object from
	 * @return {@link DataStream}
	 */
	public static DataStream fromBytes(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		if (bytes.length < HEADER_SIZE) {
			throw new IllegalArgumentException("Incomplete data");
		}
		int length = buf.getInt(0);
		byte[] buffer;
		if (length > 0) {
			int diff = bytes.length - (HEADER_SIZE + length);
			if (diff > 0) {
				throw new IllegalArgumentException("Size does not match: " + diff + " bytes missing");
			}
			buffer = Arrays.copyOfRange(bytes, HEADER_SIZE, HEADER_SIZE + length);
		} else {
			buffer = new byte[length];
		}
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
			if ((b=in.read(data, n, length - n)) < 0) {
				throw new IOException("Connection closed");
			}
			n += b;
		}
		return new DataStream(data);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		DataStream ds = (DataStream)o;
		if (!Arrays.equals(data, ds.data)) {
			return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "DataStream: " + (data != null ? data.length : 0) + " bytes";
	}
	
}
