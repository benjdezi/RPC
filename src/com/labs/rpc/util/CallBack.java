package com.labs.rpc.util;

/**
 * Callback interface
 * @author Benjamin Dezile
 */
public abstract class CallBack {
	
	protected Object param;
	
	public CallBack() {
		this(null);
	}
	
	public CallBack(Object param) {
		this.param = param;
	}
	
	public abstract void call(Object... params);
	
}
