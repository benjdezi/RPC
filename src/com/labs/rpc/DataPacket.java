package com.labs.rpc;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.*;
import org.json.*;

public class DataPacket {

	private static final String NULL = "null";						// Null value
	private static final String SEP = "::";							// Separator
	private static final int HEADER_SIZE = 20;						// Size of the header
	
	private static final byte FORMAT_NULL = 0x40;					// Null
	private static final byte FORMAT_BOOL = 0x41;					// Boolean
	private static final byte FORMAT_BYTE = 0x42;					// Byte (0-255)
	private static final byte FORMAT_CHAR = 0x43;					// Character
	private static final byte FORMAT_SHORT = 0x44;					// Short integer
	private static final byte FORMAT_INT = 0x45;					// Integer
	private static final byte FORMAT_FLOAT = 0x46;					// Float
	private static final byte FORMAT_DOUBLE = 0x47;					// Double
	private static final byte FORMAT_LONG = 0x48;					// Long
	private static final byte FORMAT_STRING = 0x49;					// String
	private static final byte FORMAT_ARRAY = 0x50;					// Array of objects
	private static final byte FORMAT_LIST = 0x51;					// List of objects
	private static final byte FORMAT_JSON = 0x52;					// JSON object
	private static final byte FORMAT_JSON_ARRAY = 0x53;				// JSON array
	private static final byte FORMAT_EXCEPTION = 0x54;				// Exception
	private static final byte FORMAT_OTHER = 0x55;					// Unknown type
	
	private static Long seqCounter = 0L;							// Sequence counter
	
	protected long seq;												// Sequence number
	private long time;												// Creation timestamp
	private String meth;											// Method being called
	private Object[] args;											// Method argument
	
	/**
	 * Create a new data packet
	 */
	private DataPacket() {
		seq = 0;
		time = 0;
		meth = null;
		args = null;
	}
	
	/**
	 * Create a new data packet
	 * @param method {@link String} - Method to call
	 * @param params {@link Object}... - Call parameters
	 */
	public DataPacket(String method, Object... params) {
		seq = getSeqNumber();
		time = System.currentTimeMillis();
		meth = method;
		args = params;
	}
	
	private static final long getSeqNumber() {
		synchronized(seqCounter) {
			return ++seqCounter;
		}
	}
	
	public long getSeq() {
		return seq;
	}
	
	public long getTime() {
		return time;
	}
	
	public String getMethod() {
		return meth;
	}
	
	public Object[] getArguments() {
		return args;
	}
	
	public byte[] getBytes() {
		StringBuffer buf = new StringBuffer();
		buf.append(meth);
		for (Object arg:args) {
			buf.append(SEP);
			buf.append(new String(packObject(arg)));
		}
		ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
		header.putInt(buf.length());
		header.putLong(seq);
		header.putLong(time);
		buf.insert(0, new String(header.array()));
		return buf.toString().getBytes();
	}
	
	public DataPacket fromBytes(byte[] bytes) throws Exception {
		ByteBuffer buf = ByteBuffer.wrap(bytes);
		DataPacket dp = new DataPacket();
		int l = buf.getInt(0);
		dp.time = buf.getLong(4);
		dp.seq = buf.getLong(12);
		String[] parts = new String(Arrays.copyOfRange(bytes, HEADER_SIZE, HEADER_SIZE + l)).split(SEP);
		if (parts.length == 0) {
			throw new IllegalArgumentException("Invalid packet: no call data");
		}
		dp.meth = parts[0];
		Object[] params = new Object[parts.length - 1];
		for (int i=1;i<parts.length;i++) {
			params[i-1] = unpackObject(parts[i]);
		}
		dp.args = params;
		return dp;
	}
	
	public DataPacket fromStream(InputStream in) throws Exception {
		byte[] headerBytes = new byte[HEADER_SIZE];
		int n = 0;
		while (n < HEADER_SIZE) {
			in.read(headerBytes, n, HEADER_SIZE - n);
		}
		DataPacket dp = new DataPacket();
		ByteBuffer header = ByteBuffer.wrap(headerBytes);
		int l = header.getInt(0);
		dp.time = header.getLong(4);
		dp.seq = header.getLong(12);
		byte[] payloadBytes = new byte[l];
		n = 0;
		while (n < l) {
			in.read(payloadBytes, n, l - n);
		}
		String[] parts = new String(payloadBytes).split(SEP);
		if (parts.length == 0) {
			throw new IllegalArgumentException("Invalid packet: no call data");
		}
		dp.meth = parts[0];
		Object[] params = new Object[parts.length - 1];
		for (int i=1;i<parts.length;i++) {
			params[i-1] = unpackObject(parts[i]);
		}
		dp.args = params;
		return dp;
	}
	
	/**
	 * Pack an object so that it can be transported
	 * @param arg {@link Object} - Argument
	 * @return byte[]
	 */
	@SuppressWarnings("unchecked")
	protected static byte[] packObject(Object arg) {
		ByteBuffer buf;
		byte type;
		String data;
		if (arg == null) {
			type = FORMAT_NULL;
			data = NULL;
		} else if (arg instanceof Byte) {
			type = FORMAT_BYTE;
			data = arg.toString();
		} else if (arg instanceof Character) {
			type = FORMAT_CHAR;
			data = arg.toString();
		} else if (arg instanceof Short) {
			type = FORMAT_SHORT;
			data = arg.toString();
		} else if (arg instanceof Boolean) {
			type = FORMAT_BOOL;
			data = arg.toString();
		} else if (arg instanceof Integer) {
			type = FORMAT_INT;
			data = arg.toString();
		} else if (arg instanceof Float) {
			type = FORMAT_FLOAT;
			data = arg.toString();
		} else if (arg instanceof Double) {
			type = FORMAT_DOUBLE;
			data = arg.toString();
		} else if (arg instanceof Long) {
			type = FORMAT_LONG;
			data = arg.toString();
		} else if (arg instanceof String) {
			type = FORMAT_STRING;
			data = arg.toString();
		} else if (arg.getClass().isArray()) {
			JSONArray a = new JSONArray();
			for (Object o:(Object[])arg) {
				a.put(o);
			}
			type = FORMAT_ARRAY;
			data = a.toString();
		} else if (arg instanceof ArrayList<?>) {
			JSONArray a = new JSONArray();
			for (Object o:(ArrayList)arg) {
				a.put(o);
			}
			type = FORMAT_LIST;
			data = a.toString();		
		} else if (arg instanceof JSONObject) {
			type = FORMAT_JSON;
			data = arg.toString();
		} else if (arg instanceof JSONArray) {
			type = FORMAT_JSON_ARRAY;
			data = arg.toString();
		} else if (arg instanceof Exception) {
			type = FORMAT_EXCEPTION;
			Exception e = (Exception)arg; 
			JSONObject json = new JSONObject();
			try {
				json.put("class", e.getClass().getSimpleName());
				json.put("message", e.getMessage());
			} catch (JSONException ex) {
				e.printStackTrace();
			}
			data = json.toString();
		} else {
			type = FORMAT_OTHER;
			data = arg.toString();
		}
		byte[] bytes = data.getBytes();
		buf = ByteBuffer.allocate(1 + bytes.length);
		buf.put(type);
		buf.put(bytes);
		return buf.array();
	}
	
	/**
	 * Unpack an object
	 * @param argData {@link String} - Data
	 * @return {@link Object}
	 */
	protected static Object unpackObject(String argData) throws Exception {
		ByteBuffer buf = ByteBuffer.wrap(argData.substring(0,1).getBytes());
		byte type = buf.get(0);
		String data = argData.substring(1);
		if (type == FORMAT_NULL) {
			return null;
		} else if (type == FORMAT_BOOL) {
			return "true".equals(data);
		} else if (type == FORMAT_BYTE) {
			return Byte.parseByte(data);
		} else if (type == FORMAT_CHAR) {
			return data.charAt(0);
		} else if (type == FORMAT_SHORT) {
			return Short.parseShort(data);
		} else if (type == FORMAT_INT) {
			return Integer.parseInt(data);
		} else if (type == FORMAT_FLOAT) {
			return Float.parseFloat(data);
		} else if (type == FORMAT_DOUBLE) {
			return Double.parseDouble(data);
		} else if (type == FORMAT_LONG) {
			return Long.parseLong(data);
		} else if (type == FORMAT_STRING) { 
			return data;
		} else if (type == FORMAT_ARRAY) {
			JSONArray a = new JSONArray(new JSONTokener((String)data));
			Object[] array = new Object[a.length()];
			for (int i=0;i<a.length();i++) {
				array[i] = a.get(i);
			}
			return array;			
		} else if (type == FORMAT_LIST) {
			JSONArray a = new JSONArray(new JSONTokener((String)data));
			List<Object> l = new ArrayList<Object>(a.length());
			for (int i=0;i<a.length();i++) {
				l.add(a.get(i));
			}
			return l;
		} else if (type == FORMAT_JSON) {
			return new JSONObject(new JSONTokener((String)data));
		} else if (type == FORMAT_JSON_ARRAY) {
			return new JSONArray(new JSONTokener((String)data));
		} else if (type == FORMAT_EXCEPTION) {
			JSONObject ex = new JSONObject(new JSONTokener((String)data));
			return new Exception(ex.optString("class") + ": " + ex.optString("message"));
		} else if (type == FORMAT_OTHER) {
			return data;
		} else {
			throw new IllegalArgumentException("Invalid arg type: " + type);
		}
	}
	
}
