/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util;

// import fuegocore.presence.tests.PresenceSuite;
// import fuegocore.imp.test.ImpSuite;
import fuegocore.message.tests.MessageSuite;
import fuegocore.util.tests.UtilSuite;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/*
 * $Id: RootSuite.java,v 1.1 2006/02/07 09:45:14 jkangash Exp $
 */

/**
 * This is both the root suite for Fuego Core regression tests and a small demonstration of the
 * usage of JUnite framework. JUnit has many interesting features and knobs, go find out more at
 * http://www.junit.org/ <br/>
 * Notice the definitions: a TEST is identified by a test method. A test SUITE is usually a class
 * declaring one or more test methods, or a custom collection of tests constructed separately.
 * @author Marko Saaresto (@hiit.fi)
 * @version $Revision: 1.1 $
 */
public class RootSuite extends TestCase {

    /**
     * Constructor for RootSuite. *THIS IS A MUST IN EVERY TESTCASE CLASS*
     * @param name
     */
    public RootSuite(String name) {
        super(name);
    }


    /* ============= begin test ========================= */

    /**
     * This method may be defined in any class and it is called before each test method is executed.
     * A useful place to set up resources needed by the tests. Once for each test. Also _the_ place
     * to initialize any instance variables or other data needed for tests.
     */
    @Override
    protected void setUp() {
    }


    /**
     * May be defined in any class and it is called after each test method has been executed. A
     * useful place to free the resources from method setUp(). Once for each test.
     */
    @Override
    protected void tearDown() {
    }


    /**
     * Tests are detected automatically through reflection. Remember that: 1) All test methods must
     * be named "testXXX" 2) methods return nothing and declare no exceptions 3) do not assume or
     * enforce _any_ persistent states between the execution of two tests. The order of execution is
     * usually arbitrary.
     */
    public void testSuccess() {
        /* assertTrue test any boolean expression. This test will not fail. */
        assertTrue("This message is delivered if a test fails", true);

        /*
         * Note that since tests are mundane methods, you can also construct a separate invariant
         * method that may be used for asserting the current state mid-test.
         */
    }


    /**
     * An example of a test that will fail.
     */
    /*
     * Commented out so that actual failures are more easily spotted public void testFailure() {
     * assertNotNull( "This test should fail, the parameter being a null", null);
     * 
     * assertEquals("This test will not be reached for the above one will " + "interrupt this test."
     * , "a", "a");
     * 
     * }
     */

    /* ============= end test ========================= */

    public static Test suite() {
        TestSuite tests = new TestSuite();

        /*
         * A preferred way to add a single test suite. JUnit will automatically search for all test
         * methods in the class.
         */
        tests.addTestSuite(RootSuite.class);

        /*
         * A preferred way to add a suite aggregate. Add your own top-level suite aggregates in a
         * similar fashion below. You may nest aggregates like this freely.
         */
        tests.addTest(UtilSuite.suite());

        // TEMPORARY! DO NOT COMMIT!
        tests.addTest(MessageSuite.suite());

        // tests.addTest(ImpSuite.suite());

        // tests.addTest(PresenceSuite.suite());

        return tests;
    }

}
