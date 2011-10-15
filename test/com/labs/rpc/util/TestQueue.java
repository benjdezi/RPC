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
