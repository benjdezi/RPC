package com.labs.rpc;

import java.util.*;
import org.json.*;

/**
 * Remote call packet
 * @author ben
 */
public class RemoteCall extends DataPacket {
	
	public static final byte TYPE = 0;								// Packet type
	protected static final String SEP = "::";						// Separator
	
	private String meth;											// Method being called (package.class.method)
	private Object[] args;											// Method argument
	
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
	 * @param method {@link String} - Method to call
	 * @param params {@link Object}... - Call parameters
	 */
	public RemoteCall(String method, Object... params) {
		super(TYPE);
		meth = method;
		args = params;
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
	 * Get the packet bytes to send over
	 * @return byte[]
	 */
	public byte[] getBytes() {
		StringBuffer buf = new StringBuffer();
		buf.append(meth);
		for (Object arg:args) {
			buf.append(SEP);
			buf.append(new String(packObject(arg)));
		}
		byte[] header = makeHeaderBytes(buf.length());
		byte[] payload = buf.toString().getBytes();
		return makePacketBytes(header, payload);
	}
	
	/**
	 * Make a remote call from a raw packet
	 * @param dp {@link DataPacket} - Data packet
	 * @return {@link RemoteCall}
	 * @throws Exception 
	 */
	public static RemoteCall fromPacket(DataPacket dp) throws Exception {
		if (dp.type != TYPE) {
			throw new IllegalArgumentException("Wrong type of packet: " + dp.type);
		}
		RemoteCall rc = new RemoteCall();
		String[] parts = new String(dp.payload).split(SEP);
		if (parts.length == 0) {
			throw new IllegalArgumentException("Invalid packet (no valid call data)");
		}
		rc.seq = dp.seq;
		rc.time = dp.time;
		rc.meth = parts[0];
		rc.args = new Object[parts.length-1];
		for (int i=1;i<parts.length;i++) {
			rc.args[i-1] = unpackObject(parts[i]);
		}
		return rc;
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
		if ((meth != null && rc.meth == null) || (meth == null && rc.meth != null) || (!meth.equals(rc.meth))) {
			return false;
		}
		if ((args == null && rc.args != null) || (args != null && rc.args == null) || (args.length != rc.args.length)) {
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
