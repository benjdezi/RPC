package com.labs.rpc.util;

/**
 * Implements a basic asynchronous task
 * @author Benjamin Dezile
 */
public abstract class AsyncTask extends Thread {

	protected Object[] params;
		
	/**
	 * Create a new asynchronous task
	 * @param name {@link String - Associated name
	 * @param params {@link Object}... - List of parameters to be passed along
	 */
	public AsyncTask(String name, Object... params) {
		super(name);
		this.params = params;
		start();
	}
	
	@Override
	public abstract void run();
	
}
