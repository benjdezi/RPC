package com.labs.rpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import org.json.*;
import com.labs.rpc.transport.DataPacket;

/**
 * Remote call packet
 * @author Benjamin Dezile
 */
public class RemoteCall extends DataPacket {
	
	public static final byte TYPE = 0;			// Packet type
	protected static final String SEP = "::";	// Separator
	
	private String target;						// Target object
	private String meth;						// Name of the method to call on the target
	private Object[] args;						// Call arguments
	
	/**
	 * Create a new data packet
	 */
	private RemoteCall() {
		super();
		meth = null;
		args = null;
	}
	
	/**
	 * Create a new data packet
	 * @param obj {@link String} - Target object
	 * @param method {@link String} - Method to call
	 * @param params {@link Object}... - Call parameters
	 */
	public RemoteCall(String obj, String method, Object... params) {
		super(TYPE);
		target = obj;
		meth = method;
		args = params;
	}
	
	/**
	 * Return the name of the target object
	 * @return {@link String}
	 */
	public String getTarget() {
		return target;
	}
	
	/**
	 * Get the method name
	 * @return {@link String}
	 */
	public String getMethod() {
		return meth;
	}
	
	/**
	 * Get the list of call parameters
	 * @return {@link Object}[]
	 */
	public Object[] getArguments() {
		return args;
	}
	
	/**
	 * Get the packet bytes to send over.<br>
	 * Formatted as target|method|#args+argInfo1+...+argInfoN,
	 * where argInfo is argLen + argData
	 * @return byte[]
	 */
	public byte[] getBytes() {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			buffer.write(encode(target));
			buffer.write(encode(meth));
			buffer.write(IntToBytes(args.length));
			for (Object arg:args) {
				buffer.write(encode(arg));
			}
		} catch(IOException e) {
			return null;
		}
		byte[] header = makeHeaderBytes(buffer.size());
		return makePacketBytes(header, buffer.toByteArray());
	}
	
	/**
	 * Encode a given object
	 * @param obj {@link Object} - Object to encode
	 * @return byte[] Encoded bytes representing the object
	 */
	private byte[] encode(Object obj) {
		byte[] bytes = packObject(obj);
		ByteBuffer buffer = ByteBuffer.allocate(4 + bytes.length);
		buffer.putInt(bytes.length);
		buffer.put(bytes);
		return buffer.array();
	}
	
	/**
	 * Convert an integer to bytes
	 * @param i int - Integer value
	 * @return byte[]
	 */
	private byte[] IntToBytes(int i) {
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.putInt(i);
		return buf.array();
	}
	
	/**
	 * Make a remote call from a raw packet
	 * @param dp {@link DataPacket} - Data packet
	 * @return {@link RemoteCall}
	 * @throws Exception 
	 */	
	public static RemoteCall fromPacket(DataPacket dp) throws Exception {
		if (dp.getType() != TYPE) {
			throw new IllegalArgumentException("Wrong type of packet: " + dp.getType());
		}
		ByteBuffer buffer = ByteBuffer.wrap(dp.getPayload());
		RemoteCall rc = new RemoteCall();
		rc.seq = dp.getSeq();
		rc.time = dp.getTime();
		rc.target = (String)decodeNext(buffer);
		rc.meth = (String)decodeNext(buffer);
		int nArgs = buffer.getInt();
		rc.args = new Object[nArgs];
		for (int i=0;i<nArgs;i++) {
			rc.args[i] = decodeNext(buffer);
		}
		return rc;
	}
	
	/**
	 * Decode the next available object from the given buffer
	 * @param buffer {@link ByteBuffer} - Data buffer
	 * @return {@link Object} Decoded object
	 * @throws Exception
	 */
	private static Object decodeNext(ByteBuffer buffer) throws Exception {
		int size = buffer.getInt();
		byte[] bytes = new byte[size];
		buffer.get(bytes);
		return unpackObject(bytes);
	}
	
	/**
	 * Assert if this object is the same as the given one
	 * @return boolean
	 */
	public boolean equals(Object o) {
		if (!super.equals(o)) {
			return false;
		}
		RemoteCall rc = (RemoteCall)o;
		if (target != rc.target && ((target != null && rc.target == null) || (target == null && rc.target != null) || (!target.equals(rc.target)))) {
			return false;
		}
		if (meth != rc.meth && ((meth != null && rc.meth == null) || (meth == null && rc.meth != null) || (!meth.equals(rc.meth)))) {
			return false;
		}
		if (args != rc.args && ((args == null && rc.args != null) || (args != null && rc.args == null) || (args.length != rc.args.length))) {
			return false;
		}
		Object arg1, arg2;
		for (int i=0;i<args.length;i++) {
			arg1 = args[i];
			arg2 = rc.args[i];
			if (arg1 == arg2) {
				continue;
			}
			if ((arg1 == null && arg2 != null) || (arg1 != null && arg2 == null)) {
				return false;
			}
			if (arg1.getClass().isArray() && arg2.getClass().isArray()) {
				if (!Arrays.equals((Object[])arg1, (Object[])arg2)) {
					return false;
				}
			} else if (arg1 instanceof JSONObject && arg2 instanceof JSONObject) {
				// TODO: JSONObject equals
			} else if (arg1 instanceof JSONArray && arg2 instanceof JSONArray) {
				// TODO: JSONArray equals
			} else if (!arg1.equals(arg2)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Return a string representation of this object
	 * @return {@link String}
	 */
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("RemoteCall #");
		buf.append(seq);
		buf.append(": ");
		buf.append(target);
		buf.append(" -> ");
		buf.append(meth);
		buf.append("(");
		for (Object arg:args) {
			if (arg instanceof String) {
				buf.append("'");
				buf.append(arg);
				buf.append("'");
			} else {
				buf.append(arg);
			}
			buf.append(",");
		}
		buf.append(")");
		return buf.toString();
	}
	
}
