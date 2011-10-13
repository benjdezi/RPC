package com.labs.rpc;

/**
 * Remote call return packet
 * @author ben
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
		super(TYPE, call.getSeq());
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
		if (dp.type != TYPE) {
			throw new IllegalArgumentException("Wrong type of packet: " + dp.type);
		}
		RemoteCallReturn rcr = new RemoteCallReturn();
		rcr.seq = dp.seq;
		rcr.time = dp.time;
		rcr.val = unpackObject(new String(dp.payload));
		return rcr;
	}
	
}
