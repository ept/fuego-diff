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

// $Id: BenchMark.java,v 1.3 2006/03/22 08:29:41 ctl Exp $

package fc.xml.diff.benchmark;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;
import fc.util.IOUtil;
import fc.util.log.Log;

public class BenchMark extends TestCase {

  protected Properties setup = new Properties(System.getProperties());

  protected File workDir;

  public BenchMark(String[] settings, String name) {
   super(name);
   try {
      for (String f :settings) {
        FileInputStream fin = new FileInputStream(f);
        setup.load(fin);
        fin.close();
      }
      System.setProperties(setup);
    } catch (IOException ex) {
      Log.log("Can't load settings",Log.FATALERROR);
    }
    workDir = new File(setup.getProperty("workdir","+tmp-test"));
    if( !workDir.exists() && !workDir.mkdir() )
      Log.log("Can't create workdir "+workDir,Log.FATALERROR);
  }

  protected void exec(final String program, final String[] args, final boolean stderr,
    final String logFile, final long timeout ) throws IOException{
     //Log.log("exec "+program,Log.INFO,args);
     if( program.startsWith("!") ) {
      //Log.log("Javastart: "+program,Log.INFO);
      try {
        Class pc = Class.forName(program.substring(1));
        final Method main = pc.getMethod("main", new Class[] {String[].class});
        OutputStream lout = System.out;
        PrintStream ps = null;
        PrintStream old = null;
        try {
          if( logFile != null ) {
            lout = new FileOutputStream(logFile);
            ps = new PrintStream(lout);
            ps.flush();
            if( stderr ) {
              old = System.err;
              System.setErr(ps);
            } else {
              old = System.out;
              System.setOut(ps);
            }
          }
          final Thread runner = new Thread() {
            public void run() {
              try {
                main.invoke(null, new Object[] {args});
              } catch (InvocationTargetException ex) {
                if( ex.getCause() instanceof ThreadDeath )
                  Log.log("Killed by timeout",Log.WARNING);
                else
                  Log.log("Exception executing "+program,Log.ERROR,ex);
              } catch (IllegalArgumentException ex) {
                Log.log("Exception executing "+program,Log.ERROR,ex);
              } catch (IllegalAccessException ex) {
                Log.log("Exception executing "+program,Log.ERROR,ex);
              } catch ( ThreadDeath ex ) {
                Log.log("Killed by timeout",Log.WARNING);
              } catch ( Throwable ex ) {
                Log.log("Exception executing "+program,Log.ERROR,ex);
              }
            }
          };
          runner.start();
          Thread.yield();
          /*final Thread mainThread = Thread.currentThread();
          Thread watchDog = new Thread() {
            public void run() {
              try {
                Thread.sleep(timeout);
                mainThread.interrupt();
              } catch (InterruptedException ex){}
            }
          };
          watchDog.start();*/
          long now = System.currentTimeMillis();
          try {
            runner.join(timeout);
          } catch (InterruptedException ex1) {
          }
          if( runner.isAlive() && (System.currentTimeMillis()-now) >=
             timeout ) {
            runner.stop(); // This really doesn't work very well!
          }
        } finally {
          if( logFile != null ) {
            ps.flush();
            lout.close();
            if( stderr ) {
              System.setErr(old);
            } else {
              System.setOut(old);
            }
          }
        }
      } catch (SecurityException ex) {
        Log.log("Exception executing "+program,Log.ERROR,ex);
      } catch (NoSuchMethodException ex) {
        Log.log("Exception executing "+program,Log.ERROR,ex);
      } catch (ClassNotFoundException ex) {
        Log.log("Exception executing "+program,Log.ERROR,ex);
      }
    } else {
      //Log.log("SPAWN: "+program+" to "+timeout,Log.INFO);
      String[] args2=new String[args.length+1];
      args2[0]=program;
      List<String> env = new LinkedList<String>();
      for (Enumeration en = System.getProperties().propertyNames();
                            en.hasMoreElements(); ) {
        String key = (String) en.nextElement();
        if( !key.startsWith("java.") && !key.startsWith("sun."))
          env.add(key.replace('.','_')+"="+System.getProperty(key));
      }
      System.arraycopy(args,0,args2,1,args.length);
      final Process p = Runtime.getRuntime().exec(args2,
                                            env.toArray(new String[env.size()]));
      final Thread mainThread = Thread.currentThread();
      Thread watchDog = new Thread() {
        public void run() {
          try {
            //Log.log("Watchdog sleeping " + timeout, Log.INFO);
            Thread.sleep(timeout);
            Log.log("Interrupting process.", Log.WARNING);
            mainThread.interrupt();// mainThread.interrupt();
          } catch (InterruptedException ex) {
            ; //Log.log("Watchdog done " + timeout, Log.INFO);
          }
        }
      };
      watchDog.start();
      (new Thread() {
        public void run() {
          OutputStream lout = System.out;
          try {
            if (logFile != null)
              lout = new FileOutputStream(logFile);
            IOUtil.copyStream(stderr ? p.getErrorStream() : p.getInputStream(),
                            lout);
          } catch (IOException ex) {
            Log.log("Error copying process out", Log.ERROR);
          } finally {
            if (logFile != null) {
              try {
                lout.flush();
                lout.close();
              } catch ( IOException ex) {
                Log.log("Cannot close log-out",Log.ERROR);
              }
            }
          }
        }
      }).start();
      try {
        //Log.log("Starting wait4",Log.INFO);
        p.waitFor();
        getSetProperty("killed",0);
        //Log.log("Ending wait4",Log.INFO);
      } catch (InterruptedException ex) {
        Log.log("Timeout of "+program+", now I kill it",Log.WARNING);
        p.destroy();
        getSetProperty("killed",1);
      }
      //Log.log("PROCESS DONE!",Log.INFO);
      watchDog.interrupt();
    }
  }

  protected static String getSetProperty(String name, String defaultVal ) {
    String val = System.getProperty(name,null);
    if( val == null && defaultVal != null ) {
      val = defaultVal;
      System.setProperty(name, val);
    }
    return val;
  }

  protected static long getSetProperty(String name, long defaultVal ) {
    return Long.parseLong(getSetProperty(name,String.valueOf(defaultVal)));
  }

  protected static int getSetProperty(String name, int defaultVal ) {
    return Integer.parseInt(getSetProperty(name,String.valueOf(defaultVal)));
  }

  protected static boolean getSetProperty(String name, boolean defaultVal ) {
    return Boolean.parseBoolean(getSetProperty(name,String.valueOf(defaultVal)));
  }

  protected static double getSetProperty(String name, double defaultVal ) {
    return Double.parseDouble(getSetProperty(name,String.valueOf(defaultVal)));
  }

}
