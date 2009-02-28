/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.tests;

import junit.framework.TestCase;

import fuegocore.util.Queue;

/**
 * Test the queue implementation. This class is a collection of JUnit test cases for the
 * {@link Queue} class.
 */
public class QueueTest extends TestCase {

    private Queue queue;


    protected void setUp() {
        queue = new Queue();
    }


    public QueueTest(String name) {
        super(name);
    }


    /**
     * Test with separated enqueuing and dequeuing. First, insert a few items into the queue and
     * then remove them all. Finally, ensure that the queue is empty.
     */
    public void testStraight() {
        assertTrue("Queue not empty", queue.empty());
        queue.enqueue(new Integer(0));
        assertTrue("Queue still empty", !queue.empty());
        assertEquals("Inserted item not found", new Integer(0), queue.peek());
        queue.enqueue(new Integer(1));
        queue.enqueue(new Integer(2));
        for (int i = 0; i < 3; i++) {
            assertEquals("Queue not preserving order", new Integer(i), queue.dequeue());
        }
        assertTrue("Queue not yet empty", queue.empty());
    }


    /**
     * Test intermingled enqueuing and dequeuing. In a loop, do an insertion followed immediately by
     * a deletion. Check that the queue's emptiness remains correct at all times.
     */
    public void testMixed() {
        assertTrue("Queue not empty", queue.empty());
        for (int i = 0; i < 5; i++) {
            queue.enqueue(new Integer(i));
            assertTrue("Queue empty on round " + i, !queue.empty());
            assertEquals("Incorrect item received on round " + i, new Integer(i), queue.dequeue());
            assertTrue("Queue not empty on round " + i, queue.empty());
        }
    }

}
