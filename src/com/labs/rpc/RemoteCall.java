package com.labs.rpc;

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
		this(null);
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
	
}
