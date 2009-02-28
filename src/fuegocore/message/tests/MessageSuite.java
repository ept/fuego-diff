/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.message.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * The collection of tests for the messaging subsystem. This class adds all the known messaging
 * system unit tests into the tests to be run.
 */
public class MessageSuite extends TestCase {

    public MessageSuite(String name) {
        super(name);
    }


    public static Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTestSuite(CacheTest.class);

        return suite;
    }

}
