package com.labs.rpc.util;

import java.util.List;
import java.util.ArrayList;

/**
 * Thread-safe fifo queue
 * @author ben
 */
public class Queue<T> {

	private static final long serialVersionUID = 1L;
	private static final int WAIT_DELAY = 100;			// Delay before retrying to get an item (in ms)
	private List<T> items;								// Queue items
	
	/**
	 * Create an empty queue
	 */
	public Queue() {
		items = new ArrayList<T>(0);
	}
	
	/**
	 * Get the next element in the queue without waiting
	 * @return T 
	 */
	public T get() {
		try {
			return get(0);
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	/**
	 * Get the next element in the queue
	 * @param timeout long - Maximum number of seconds to wait for an element
	 * @return T The next available item or null if none found
	 * @throws InterruptedException
	 */
	public T get(long timeout) throws InterruptedException {
		long t = timeout > 0 ? System.currentTimeMillis() : 0;
		do {
			synchronized(items) {
				if (items.size() > 0) {
					try {
						/* Trying to get an item */
						return items.remove(0);
					} catch (IndexOutOfBoundsException e) {
						if (t <= 0) {
							/* No wait */
							break;
						}
					}
				}
			}
			Thread.sleep(WAIT_DELAY);
		} while (System.currentTimeMillis()-t < timeout*1000);
		/* Got nothing */
		return null;
	}
	
	/**
	 * Put an element into the list
	 * @param o T - Element to put in the queue
	 * @return boolean True upon success
	 */
	public synchronized boolean put(T o) {
		if (o != null) {
			return items.add(o);
		}
		throw new IllegalArgumentException("invalid item");
	}
	
	/**
	 * Return the current size of the queue
	 * @return int 
	 */
	public synchronized int size() {
		return items.size();
	}
		
	/**
	 * Return true if there are no items in the queue
	 * @return boolean
	 */
	public synchronized boolean isEmpty() {
		return items.size() == 0;
	}
	
	/**
	 * Discard all items currently in queue
	 */
	public synchronized void clear() {
		items.clear();
	}
	
}
