package com.labs.rpc.util;

import java.util.List;
import java.util.ArrayList;

/**
 * Thread-safe fifo queue
 * @author Benjamin Dezile
 */
public class Queue<T> {

	private static final long serialVersionUID = 1L;
	private Object gotItem = new Object();				// Sync object
	private List<T> items;								// Queue items
	
	/**
	 * Create an empty queue
	 */
	public Queue() {
		items = new ArrayList<T>(0);
	}
	
	/**
	 * Block until an item becomes available 
	 * @return T
	 */
	public T poll() {
		try {
			return get(-1);
		} catch (InterruptedException e) {
			return null;
		}
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
	 * @param timeout double - Maximum number of seconds to wait for an element
	 * @return T The next available item or null if none found
	 * @throws InterruptedException
	 */
	public T get(double timeout) throws InterruptedException {
		long timeoutMilli = (long)(timeout * 1000);
		long exitTime = System.currentTimeMillis() + timeoutMilli;
		do {
			synchronized(items) {
				if (items.size() > 0) {
					/* Got an item */
					return items.remove(0);
				} else if (timeout == 0) {
					/* No timeout, we're done here */
					break;
				}
			}
			/* No item available, let's wait */
			try {
				synchronized(gotItem) {
					if (timeout > 0) {
						/* Wait up to timeout */
						gotItem.wait(exitTime - System.currentTimeMillis() + 1);
					} else {
						/* Wait a little while */
						gotItem.wait(1000);
					}
				}
			} catch (InterruptedException e) {
				return null;
			}
		} while (timeout < 0 || System.currentTimeMillis() < exitTime);
		/* Got nothing */
		return null;
	}
	
	/**
	 * Return the head without removing it from the queue
	 * @return T Null if empty
	 */
	public T peek() {
		synchronized(items) {
			if (items.size() == 0) {
				return null;
			}
			return items.get(0);
		}
	}
	
	/**
	 * Put an item back at the top of the queue
	 * @param o T - Item to put back
	 */
	public void putBack(T o) {
		synchronized(items) {
			List<T> newItems = new ArrayList<T>(0);
			newItems.add(o);
			newItems.addAll(items);
			items = newItems;
		}
	}
	
	/**
	 * Put an element into the list
	 * @param o T - Element to put in the queue
	 * @return boolean True upon success
	 */
	public boolean offer(T o) {
		if (o != null) {
			boolean res;
			synchronized(items) {
				res = items.add(o);
			}
			synchronized(gotItem) {
				gotItem.notifyAll();
			}
			return res;
		}
		throw new IllegalArgumentException("invalid item");
	}
	
	/**
	 * Return the current size of the queue
	 * @return int 
	 */
	public int size() {
		synchronized(items) {
			return items.size();
		}
	}
		
	/**
	 * Return true if there are no items in the queue
	 * @return boolean
	 */
	public boolean isEmpty() {
		synchronized(items) {
			return (items.size() == 0);
		}
	}
	
	/**
	 * Discard all items currently in queue
	 */
	public void clear() {
		synchronized(items) {
			items.clear();
		}
	}	
	
}
