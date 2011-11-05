package com.labs.rpc.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.labs.rpc.util.RemoteException;

/**
 * Base for all transport packets
 * @author Benjamin Dezile
 */
public class DataPacket {

	protected static final String NULL = "null";					// Null value
	protected static final int HEADER_SIZE = 21;					// Size of the header
	
	protected static final byte FORMAT_NULL = 0x40;					// Null
	protected static final byte FORMAT_BOOL = 0x41;					// Boolean
	protected static final byte FORMAT_BYTE = 0x42;					// Byte (0-255)
	protected static final byte FORMAT_CHAR = 0x43;					// Character
	protected static final byte FORMAT_SHORT = 0x44;				// Short integer
	protected static final byte FORMAT_INT = 0x45;					// Integer
	protected static final byte FORMAT_FLOAT = 0x46;				// Float
	protected static final byte FORMAT_DOUBLE = 0x47;				// Double
	protected static final byte FORMAT_LONG = 0x48;					// Long
	protected static final byte FORMAT_STRING = 0x49;				// String
	protected static final byte FORMAT_ARRAY = 0x50;				// Array of objects
	protected static final byte FORMAT_LIST = 0x51;					// List of objects
	protected static final byte FORMAT_JSON = 0x52;					// JSON object
	protected static final byte FORMAT_JSON_ARRAY = 0x53;			// JSON array
	protected static final byte FORMAT_REMOTE_EX = 0x54;			// Remote exception
	
	private static Long seqCounter = 0L;							// Sequence counter
	protected byte type;											// Packet type
	protected long seq;												// Sequence number
	protected long time;											// Creation timestamp
	protected byte[] payload;										// Encapsulated data
		
	/**
	 * Create a new data packet
	 * @param t byte - Packet type
	 */
	public DataPacket(byte t) {
		this(t, getNextSeq());
	}

	/**
	 * Create a new data packet
	 * @param t byte - Packet type
	 * @param data byte[] - Payload bytes
	 */
	public DataPacket(byte t, byte[] data) {
		this(t, getNextSeq());
		payload = data;
	}
	
	/**
	 * Create an empty data packet
	 */
	protected DataPacket() {
		this((byte)0,0);
	}
	
	/**
	 * Create a new data packet
	 * @param seqNum long - Sequence number
	 * @param t byte - Packet type
	 */
	protected DataPacket(byte t, long seqNum) {
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
	 * Set the payload bytes
	 * @param data byte[] - Payload
	 */
	public void setPayload(byte[] data) {
		payload = data;
	}
	
	/**
	 * Get the payload bytes
	 * @return byte[]
	 */
	public byte[] getPayload() {
		return payload;
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
	@SuppressWarnings("unchecked")
	protected static byte[] packObject(Object arg) {
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			if (arg == null) {
				buffer.write(FORMAT_NULL);
			} else if (arg instanceof Byte) {
				buffer.write(FORMAT_BYTE);
				buffer.write((Byte)arg);
			} else if (arg instanceof Character) {
				buffer.write(FORMAT_CHAR);
				ByteBuffer buf = ByteBuffer.allocate(2);
				buf.putChar((Character)arg);
				buffer.write(buf.array());
			} else if (arg instanceof Short) {
				buffer.write(FORMAT_SHORT);
				ByteBuffer buf = ByteBuffer.allocate(2);
				buf.putShort((Short)arg);
				buffer.write(buf.array());
			} else if (arg instanceof Boolean) {
				buffer.write(FORMAT_BOOL);
				ByteBuffer buf = ByteBuffer.allocate(2);
				buf.put((byte)(((Boolean)arg).booleanValue() ? 1 : 0));
				buffer.write(buf.array());
			} else if (arg instanceof Integer) {
				buffer.write(FORMAT_INT);
				ByteBuffer buf = ByteBuffer.allocate(4);
				buf.putInt((Integer)arg);
				buffer.write(buf.array());
			} else if (arg instanceof Float) {
				buffer.write(FORMAT_FLOAT);
				ByteBuffer buf = ByteBuffer.allocate(4);
				buf.putFloat((Float)arg);
				buffer.write(buf.array());
			} else if (arg instanceof Double) {
				buffer.write(FORMAT_DOUBLE);
				ByteBuffer buf = ByteBuffer.allocate(8);
				buf.putDouble((Double)arg);
				buffer.write(buf.array());
			} else if (arg instanceof Long) {
				buffer.write(FORMAT_LONG);
				ByteBuffer buf = ByteBuffer.allocate(8);
				buf.putLong((Long)arg);
				buffer.write(buf.array());
			} else if (arg instanceof String) {
				buffer.write(FORMAT_STRING);
				buffer.write(((String)arg).getBytes());
			} else if (arg.getClass().isArray()) {
				JSONArray a = new JSONArray();
				for (Object o:(Object[])arg) {
					a.put(o);
				}
				buffer.write(FORMAT_ARRAY);
				buffer.write(a.toString().getBytes());
			} else if (arg instanceof ArrayList<?>) {
				JSONArray a = new JSONArray();
				for (Object o:(ArrayList)arg) {
					a.put(o);
				}
				buffer.write(FORMAT_LIST);
				buffer.write(a.toString().getBytes());		
			} else if (arg instanceof JSONObject) {
				buffer.write(FORMAT_JSON);
				buffer.write(arg.toString().getBytes());
			} else if (arg instanceof JSONArray) {
				buffer.write(FORMAT_JSON_ARRAY);
				buffer.write(arg.toString().getBytes());
			} else if (arg instanceof Exception) {
				buffer.write(FORMAT_REMOTE_EX);
				RemoteException re = (RemoteException)arg;
				String msg = re.getMessage();
				buffer.write((msg != null ? msg : "").getBytes());
			} else {
				throw new IllegalArgumentException("Unsupported data type for " + arg);
			}
			return buffer.toByteArray();
		} catch(IOException e) {
			return null;
		}
	}

	/**
	 * Unpack an object
	 * @param argBytes byte[] - Packed object bytes
	 * @return {@link Object}
	 */
	protected static Object unpackObject(byte[] argBytes) throws Exception {
		ByteBuffer buf = ByteBuffer.wrap(argBytes);
		byte type = buf.get();
		if (type == FORMAT_NULL) {
			return null;
		} else if (type == FORMAT_BOOL) {
			return buf.get() == (byte)1;
		} else if (type == FORMAT_BYTE) {
			return buf.get();
		} else if (type == FORMAT_CHAR) {
			return buf.getChar();
		} else if (type == FORMAT_SHORT) {
			return buf.getShort();
		} else if (type == FORMAT_INT) {
			return buf.getInt();
		} else if (type == FORMAT_FLOAT) {
			return buf.getFloat();
		} else if (type == FORMAT_DOUBLE) {
			return buf.getDouble();
		} else if (type == FORMAT_LONG) {
			return buf.getLong();
		} else if (type == FORMAT_STRING) { 
			return new String(argBytes).substring(1);
		} else if (type == FORMAT_ARRAY) {
			String data = new String(argBytes).substring(1);
			JSONArray a = new JSONArray(new JSONTokener(data));
			Object[] array = new Object[a.length()];
			for (int i=0;i<a.length();i++) {
				array[i] = a.get(i);
			}
			return array;			
		} else if (type == FORMAT_LIST) {
			String data = new String(argBytes).substring(1);
			JSONArray a = new JSONArray(new JSONTokener(data));
			List<Object> l = new ArrayList<Object>(a.length());
			for (int i=0;i<a.length();i++) {
				l.add(a.get(i));
			}
			return l;
		} else if (type == FORMAT_JSON) {
			String data = new String(argBytes).substring(1);
			return new JSONObject(new JSONTokener(data));
		} else if (type == FORMAT_JSON_ARRAY) {
			String data = new String(argBytes).substring(1);
			return new JSONArray(new JSONTokener(data));
		} else if (type == FORMAT_REMOTE_EX) {
			return new RemoteException(new String(argBytes).substring(1));
		} else {
			throw new IllegalArgumentException("Invalid data type: " + type);
		}
	}

	/**
	 * Make the header bytes
	 * @param pl int - Payload size 
	 * @return byte[]
	 */
	protected byte[] makeHeaderBytes(int pl) {
		ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
		header.put(type);
		header.putInt(pl);
		header.putLong(time);
		header.putLong(seq);
		return header.array();
	}
	
	/**
	 * Make the packet bytes
	 * @param header byte[] - Header bytes
	 * @param payload byte[] - Payload bytes
	 * @return byte[]
	 */
	protected byte[] makePacketBytes(byte[] header, byte[] payload) {
		ByteBuffer data = ByteBuffer.allocate(HEADER_SIZE + payload.length);
		data.put(header);
		data.put(payload);
		return data.array();
	}
		
	/**
	 * Get the packet bytes to send over
	 * @return byte[]
	 */
	public byte[] getBytes() {
		byte[] header = makeHeaderBytes(payload.length);
		return makePacketBytes(header, payload);
	}
	
	/**
	 * Build a new packet object from raw data
	 * @param bytes byte[] - Bytes
	 * @return {@link DataPacket}
	 */
	public static DataPacket fromBytes(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		DataPacket dp = new DataPacket();
		dp.type = buf.get(0);
		dp.time = buf.getLong(5);
		dp.seq = buf.getLong(13);
		int l = buf.getInt(1);
		dp.payload = Arrays.copyOfRange(bytes, HEADER_SIZE, HEADER_SIZE + l);
		return dp;
	}
	
	/**
	 * Read a new packet object from a byte stream
	 * @param in {@link InputStream} - Input stream
	 * @return {@link DataPacket}
	 * @throws IOException
	 */
	public static DataPacket fromStream(InputStream in) throws IOException {
		byte[] headerBytes = new byte[HEADER_SIZE];
		int b,n = 0;
		while (n < HEADER_SIZE) {
			if ((b=in.read(headerBytes, n, HEADER_SIZE - n)) < 0) {
				throw new IOException("Connection closed");
			}
			n += b;
		}
		DataPacket dp = new DataPacket();
		ByteBuffer header = ByteBuffer.wrap(headerBytes);
		dp.type = header.get(0);
		dp.time = header.getLong(5);
		dp.seq = header.getLong(13);
		int l = header.getInt(1);
		dp.payload = new byte[l];
		n = 0;
		while (n < l) {
			if ((b=in.read(dp.payload, n, l - n)) < 0) {
				throw new IOException("Connection closed");
			}
			n += b;
		}
		return dp;
	}

	/**
	 * Read a new packet object from a socket channel.<br>
	 * This is a much-better-IO version of DataPacket.fromStream().
	 * @param sc {@link SocketChannel} - Socket channel
	 * @return {@link DataPacket}
	 * @throws IOException
	 */
	public static DataPacket fromChannel(SocketChannel sc) throws IOException {
		ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
		int b,n = 0;
		while (n < HEADER_SIZE) {
			if ((b=sc.read(header)) < 0) {
				throw new IOException("Connection closed");
			}
			n += b;
		}
		DataPacket dp = new DataPacket();
		dp.type = header.get(0);
		dp.time = header.getLong(5);
		dp.seq = header.getLong(13);
		int l = header.getInt(1);
		ByteBuffer payload = ByteBuffer.allocateDirect(l);
		n = 0;
		while (n < l) {
			if ((b=sc.read(payload)) < 0) {
				throw new IOException("Connection closed");
			}
			n += b;
		}
		dp.payload = new byte[l];
		payload.rewind();
		payload.get(dp.payload, 0, l);
		return dp;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		DataPacket dp = (DataPacket)o;
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
	
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("DataPacket: type=");
		buf.append(type);
		buf.append(", seq=");
		buf.append(seq);
		buf.append(", time=");
		buf.append(time);
		buf.append(", payload: ");
		buf.append(payload != null ? payload.length : 0);
		buf.append(" bytes");
		return buf.toString();
	}
	
}
