/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.message.tests;

import junit.framework.TestCase;

import fuegocore.message.encoding.OutCache;

/**
 * Test the message encoding caches. This class is a set of JUnit test cases for the various caches
 * used in message encoding.
 */
public class CacheTest extends TestCase {

    public CacheTest(String name) {
        super(name);
    }


    /**
     * Test that values do not linger in output caches. An actual application discovered that values
     * were not evicted from output caches, which caused problems if an overwritten cached string
     * was used later. This test ensures that it does not happen again.
     */
    public void testCacheEviction() {
        OutCache cache = new OutCache();
        int initialSlot = 0x00;
        // Fill the cache with values and overflow a little
        for (int i = initialSlot; i < 300; i++) {
            assertEquals("Value " + i + " not inserted in expected place", i % 256,
                         cache.insert("Key" + i));
        }
        // Not found: Key0 - Key43, found: Key44 - Key299
        assertEquals("Old key 0 persists in cache", -1, cache.fetch("Key0"));
        assertEquals("Old key 43 persists in cache", -1, cache.fetch("Key43"));
        assertEquals("Key 44 in a wrong place or evicted prematurely", 44, cache.fetch("Key44"));
        assertEquals("Key 299 in a wrong place or evicted prematurely", 43, cache.fetch("Key299"));
    }

}
