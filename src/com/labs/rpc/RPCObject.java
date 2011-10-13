package com.labs.rpc;

public abstract class RPCObject {

	protected static Object instance;
	
	public static Object getInstance() {
		return instance;
	}
	
}
