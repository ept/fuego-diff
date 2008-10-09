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

package fc.raxs;

/** Constants for measurement points. */

public class Measurements {
  
  public static final boolean TIMINGS = true;
    //Boolean.parseBoolean(System.getProperty("fc.raxs.measure.times", "false"));

  public static /*final*/ boolean MEMORY = 
    Boolean.parseBoolean(System.getProperty("fc.raxs.measure.memory", "false"));
  
  // Flags for enabling/disabling timings. Keep final for max performance 
  
  public static final boolean STORE_TIMINGS = TIMINGS;
  
  public static final boolean RAXS_TIMINGS = TIMINGS;

  // Timing handles
  public static Object H_STORE_INIT = new Object();
  
  public static Object H_STORE_INDEX_INIT = new Object();

  public static Object H_STORE_APPLY = new Object();

  public static Object H_STORE_APPLY_WRITETREE = new Object();

  public static Object H_STORE_APPLY_WRITETREE_END = new Object();

  public static Object H_RAXS_COMMIT_APPLY = new Object();

  public static Object H_RAXS_STOREREVERSEDELTA = new Object();

  public static Object H_RAXS_OPEN = new Object();

  public static Object H_RAXS_CLOSE = new Object();

  public static Object H_RAXS_COMMIT = new Object();

  public static Object M_STORE_PREOPEN = new Object();
  
  public static Object M_STORE_OPEN = new Object();
  
  public static Object M_STORE_PRECLOSE = new Object();

  public static Object M_STORE_CLOSE = new Object();

  // Memory handles
  
  
  
}
// arch-tag: 3c442d8e-dd0b-491e-810f-485f730cd012
//
