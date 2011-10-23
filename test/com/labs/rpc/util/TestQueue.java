package com.labs.rpc.util;

import junit.framework.*;
import org.junit.Test;
import com.labs.rpc.util.Queue;

public class TestQueue extends TestCase {
	
	@Test
	public void testPut() {
		Queue<Object> q = new Queue<Object>();
		assertEquals(q.size(),0);
		q.offer(new Object());
		assertEquals(q.size(),1);
	}
	
	@Test
	public void testGet() {
		Object o = new Object();
		Queue<Object> q = new Queue<Object>(); 
		q.offer(o);
		assertEquals(q.poll(),o);
		assertEquals(q.size(),0);
	}
	
	@Test 
	public void testPutBack() {
		Queue<Integer> q = new Queue<Integer>();
		q.offer(1);
		q.offer(2);
		q.offer(3);
		int head = q.poll();
		assertEquals(q.size(), 2);
		assertEquals((int)q.peek(), 2);
		q.putBack(head);
		assertEquals(q.size(), 3);
		assertEquals((int)q.peek(), head);		
	}
	
	@Test 
	public void testPeek() {
		Queue<Integer> q = new Queue<Integer>();
		q.offer(1);
		q.offer(2);
		q.offer(3);
		int peek = q.peek();
		assertEquals(q.size(), 3);
		assertEquals(peek, 1);
		assertEquals((int)q.poll(), peek);
	}
	
	@Test
	public void testGetWithTimeout() {
		int timeout = 1;
		Object o = new Object();
		Queue<Object> q = new Queue<Object>(); 
		long t = System.currentTimeMillis();
		try {
			assertNull(q.get(timeout));
			long dt = System.currentTimeMillis() - t;
			assertTrue("Did not wait the given amount of time ("+dt+","+(timeout*1000)+")", dt >= timeout*1000);
		} catch (InterruptedException e) {
			fail("There is no reason to be interrupted here");
		}
		try {
			q.offer(o);
			assertEquals(q.get(timeout), o);
		} catch (Exception e) {
			fail("Should not have failed here");
		}
	}
		
}
