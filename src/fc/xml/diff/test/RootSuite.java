/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xmldiff-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.diff.test;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import fc.util.log.Log;
import fc.util.log.SysoutLogger;
import fc.xml.diff.benchmark.SynteticDirTree;
import fc.xml.diff.benchmark.UseCases;

public class RootSuite extends TestCase {
    
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
      System.setProperty("verify", "true");
      tests.addTestSuite(UseCases.class);
      tests.addTestSuite(RootSuite.class);
      tests.addTestSuite(SynteticDirTree.class);
      return tests;
    }

    public void testFixTests() {
      Log.log("Fixing incompatible property setting between tests", 
          Log.WARNING);
      System.setProperty("basefile","b.xml");
      System.setProperty("newfile","n.xml");
      System.setProperty("deltafile","d.xml");
    }

}

// arch-tag: 3c223bb4-2088-445f-88c5-983229971072
