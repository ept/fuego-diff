/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.raxs.test;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import fc.util.log.Log;
import fc.util.log.SysoutLogger;

public class RootSuite extends TestCase {

  public static final Class[] suite = { VersioningTest.class };

  public void setUp() throws Exception {
  }

  public void tearDown() throws Exception {
    // 
  }
  
  public static Test suite() throws IOException {
    //System.out.println("Setting logger to System.out");
    //System.out.flush();
    SysoutLogger l = new SysoutLogger();
    Log.setLogger(l);
    TestSuite tests = new TestSuite();
    fillSuite( tests );
    return tests;
  }

  public static void fillSuite(TestSuite tests) {
    String filter = System.getProperty("fc.raxs.test.classes");
    for(Class c: suite ) {
      if(filter != null && !c.getName().matches(filter) ) {
        Log.debug("Filtered out class ",c);
        continue;
      }
      tests.addTestSuite(c);
    }
  } 
  
}

// arch-tag: d26603bb-0541-4d2b-afb0-404569c9c1b7
