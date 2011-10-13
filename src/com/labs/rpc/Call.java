package com.labs.rpc;

public class Call {
	
	private static final byte FAILED = -1;
	private static final byte UNPROCESSED = 0;
	private static final byte PENDING = 1;
	private static final byte RETURNED = 2;
	
	private RemoteCall rc;	// Initial call
	private Object ret;		// Returned value
	private byte status;	// Call status
	
	public Call(RemoteCall remoteCall) {
		rc = remoteCall;
		status = UNPROCESSED;
		ret = null;
	}
	
	public byte getStatus() {
		return status;
	}
	
	public boolean isPending() {
		return status == PENDING;
	}

	public boolean isReturned() {
		return status == RETURNED;
	}
	
	public boolean isFailed() {
		return status == FAILED;
	}
	
	public void setPending() {
		status = PENDING;
	}
	
	public void setReturned(Object val) {
		ret = val;
		status = RETURNED;
	}
	
	public void setFailed(Exception e) {
		ret = e;
		status = FAILED;
	}
	
	public RemoteCall getRemoteCall() {
		return rc;
	}
	
	public Object getReturnValue() {
		return ret;
	}
}
