package com.labs.rpc.util;

import com.labs.rpc.RemoteCall;

public class Call {
	
	private static final byte TIMEOUT = -1;
	private static final byte UNPROCESSED = 0;
	private static final byte PENDING = 1;
	private static final byte RETURNED = 2;
	
	private RemoteCall rc;		// Initial call
	private Object ret;			// Returned value
	private byte status;		// Call status
	private long startTime;		// Start time
	
	public Call(RemoteCall remoteCall) {
		rc = remoteCall;
		status = UNPROCESSED;
		ret = null;
		startTime = System.currentTimeMillis();
	}
	
	public synchronized long getStartTime() {
		return startTime;
	}
	
	public synchronized byte getStatus() {
		return status;
	}
	
	public synchronized boolean isPending() {
		return status == PENDING;
	}

	public synchronized boolean isReturned() {
		return status == RETURNED;
	}

	public synchronized boolean isTimedOut() {
		return status == TIMEOUT;
	}
		
	public synchronized void setPending() {
		status = PENDING;
	}
	
	public synchronized void setReturned(Object val) {
		ret = val;
		status = RETURNED;
	}
	
	public synchronized void setTimedOut() {
		status = TIMEOUT;
	}
	
	public synchronized RemoteCall getRemoteCall() {
		return rc;
	}
	
	public synchronized Object getReturnValue() {
		return ret;
	}
}
