package com.labs.rpc.util;

/**
 * When a remote call throws an exception on the remote side
 * @author ben
 */
public class RemoteException extends Exception {

	private static final long serialVersionUID = -8410931051078284713L;
	private String message;
	
	public RemoteException(Throwable e) {
		super(e);
		message = e != null ? e.getClass().getSimpleName() + " (" + e.getMessage() + ")" : "-";
	}
	
	public RemoteException(String msg) {
		super();
		message = msg;
	}
	
	public String getMessage() {
		return message;
	}
	
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof RemoteException)) {
			return false;
		}
		RemoteException re = (RemoteException)o;
		if ((re.message == null && message != null) || (re.message != null && message == null) || !re.message.equals(message)) {
			return false;
		}
		return true;
	}
	
}
