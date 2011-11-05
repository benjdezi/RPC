package com.labs.rpc;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

import com.labs.rpc.transport.DataPacket;

/**
 * Remote call return packet
 * @author Benjamin Dezile
 */
public class RemoteCallReturn extends DataPacket {
	
	public static final byte TYPE = 1;
	
	private Object val;		// Return value
	
	/**
	 * Create a new call return
	 * @param call {@link RemoteCall} - Associated remote call
	 * @param value {@link Object} - Returned value
	 */
	public RemoteCallReturn(RemoteCall call, Object value) {
		super(TYPE, call != null ? call.getSeq() : 0);
		val = value;
	}
	
	/**
	 * Create an empty remote call return object
	 */
	protected RemoteCallReturn() {
		this(null,null);
	}
	
	/**
	 * Get the return value
	 * @return {@link Object}
	 */
	public Object getValue() {
		return val;
	}
	
	/**
	 * Return the packet bytes to be sent
	 * @return byte[]
	 */
	public byte[] getBytes() {
		byte[] payload = packObject(val);
		byte[] header = makeHeaderBytes(payload.length);
		return makePacketBytes(header, payload);
	}

	/**
	 * Build a remote call return from a raw packet
	 * @param dp {@link DataPacket} - Data packet
	 * @return {@link RemoteCallReturn}
	 * @throws Exception 
	 */
	public static RemoteCallReturn fromPacket(DataPacket dp) throws Exception {
		if (dp.getType() != TYPE) {
			throw new IllegalArgumentException("Wrong type of packet: " + dp.getType());
		}
		RemoteCallReturn rcr = new RemoteCallReturn();
		rcr.seq = dp.getSeq();
		rcr.time = dp.getTime();
		rcr.val = unpackObject(dp.getPayload());
		return rcr;
	}
	
	/**
	 * Assert if the given object is the same as this remote call return
	 */
	public boolean equals(Object o) {
		if (!super.equals(o)) {
			return false;
		}
		RemoteCallReturn rcr = (RemoteCallReturn)o;
		if (val == rcr.val) {
			return true;
		}
		if ((val == null && rcr.val != null) || (val != null && rcr.val == null)) {
			return false;
		}
		if (val.getClass().isArray() && rcr.val.getClass().isArray()) {
			if (!Arrays.equals((Object[])val, (Object[])rcr.val)) {
				return false;
			}
		} else if (val instanceof JSONObject && rcr.val instanceof JSONObject) {
			// TODO: JSONObject equals
		} else if (val instanceof JSONArray && rcr.val instanceof JSONArray) {
			// TODO: JSONArray equals
		} else if (!val.equals(rcr.val)) {
			return false;
		}
		return true;
	}
	
}
