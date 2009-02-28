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

public class RaxsAndXmlrSuite extends TestCase {
  public static Test suite() throws IOException {
    SysoutLogger l = new SysoutLogger();
    Log.setLogger(l);
    TestSuite tests = new TestSuite();
    RootSuite.fillSuite( tests );
    fc.xml.xmlr.test.RootSuite.fillSuite(tests);
    return tests;
  }

}

// arch-tag:  df1535f7-5012-4871-8cac-9b60f707bb7c
