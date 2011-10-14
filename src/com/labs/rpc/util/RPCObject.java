package com.labs.rpc.util;

public abstract class RPCObject {

	protected static Object instance;
	
	public static Object getInstance() {
		return instance;
	}
	
}
