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

package fc.xml.diff.benchmark;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import fc.util.log.Log;
import junit.framework.Assert;

public class UseCases extends BenchMark {

  int ok=0,improv=0,worse=0,fail=0;

  public UseCases() {
    this(new String[] {});
  }

  public UseCases(String[] args) {
    super(args,"usecases");
  }

  public void testUseCases() throws IOException {
    start();
  }

  public void start() throws IOException {
    Map dirs = new TreeMap();
    File dbase = new File(workDir,"b.xml");
    File dnew = new File(workDir,"n.xml");
    File ddelta = new File(workDir, getSetProperty("deltafile","d.xml"));
    String initer = getSetProperty("initer",null);
    System.out.println("Running "+getClass().getName()+" test...");
    scanDataSets(dirs, new File(getSetProperty("root", ".")),
                 getSetProperty("dirfilter", ".*(\\.|\\./tests).*"),
                           getSetProperty("conffile","faxma\\.benchmark\\.UseCases"));
    for (Iterator i = dirs.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry e = (Map.Entry) i.next();
      List l = (List) e.getValue();
      if (l.size() < 1)
        continue;
      File dir = (File) e.getKey();
      System.setProperty("caseroot",dir.toString());
      File conffile = new File(dir, (String) l.get(0));
      Properties p = new Properties();
      FileInputStream cin = new FileInputStream(conffile);
      p.load(cin);
      cin.close();
      //Log.log("Running "+conffile,Log.INFO);
      for (int test = 0; test < Integer.MAX_VALUE; test++) {
        String pfx = test == 0 ? "" : String.valueOf(test) + ".";
        if (p.getProperty(pfx + "basec") == null &&
            p.getProperty(pfx + "base") == null)
          break;
        if (p.getProperty(pfx + "base") == null) {
          // Need to write unzipper for these cases!
          Log.warning("Skipping compressed case " + (test + 1) + " in " + dir);
          continue;
        }

        System.setProperty("name",p.getProperty(pfx+"name"));
        int laps=getSetProperty("laps",1);

        getFile(p,pfx+"basec",dir,new File(""),"basecfile");
        getFile(p,pfx+"newc",dir,new File(""),"newcfile");
        File basef = getFile(p,pfx+"base",dir,dbase,"basefile");
        File newf = getFile(p,pfx+"new",dir,dnew,"newfile");
        File deltaf = getFile(p,pfx+"delta",dir,ddelta,"deltafile");
        if( initer != null ) {
          System.setProperty("mode","init");
          exec(initer, new String[] {},false,null,Long.MAX_VALUE);
        }
        for( int lap=0;lap<laps;lap++) {
          System.setProperty("lap",""+lap);
          doDiff( basef, newf, deltaf);
        }
      }
    }
    System.out.println("--------------------------------------------------");
    System.out.println("Better\tOK\tWorse\tFail");
    System.out.println("" + improv + "\t" + ok + "\t" + worse + "\t" + fail);
    if (worse > 0 || fail > 0)
      System.out.println("Result became worse, please do not check in.");
    if (fail > 0)
      Assert.fail("Test failed due to new failures.");
  }

  protected File getFile(Properties p, String prop, File propRoot,
                         File defaultFile, String sysprop) {
    String name = p.getProperty(prop);
    File f= name == null ? defaultFile : new File( propRoot, name.trim() );
    if( f != null && sysprop != null )
      System.setProperty(sysprop,f.toString());
    return f;
  }

  protected void doDiff(File basef, File newf, File deltaf ) throws
    IOException {
    //Log.log("Test", Log.INFO,new String[] {root.toString(),basen,newn,deltan,newcn,basecn});
    String reporter = getSetProperty("reporter",null);
    long now = System.currentTimeMillis();
    try {
      exec(getSetProperty("differ", "!fc.xml.diff.Diff"), new String[]
           {basef.toString(), newf.toString(), deltaf.toString() }, true,
           getSetProperty("logfile", null),
           (long) (1000*getSetProperty("timeout", 1000000000.0)));
    } catch (Exception ex) {
      fail++;
      Log.log("FAILED: ", Log.ERROR, ex);
      return;
    }
    System.setProperty("lap.time",String.valueOf(System.currentTimeMillis()-now));
    // Scrape results
    if( reporter != null ) {
      System.setProperty("mode","report");
      exec(reporter, new String[] {}, false, null, Long.MAX_VALUE);
    }
    ok++;
  }

  public static void scanDataSets(Map dirs, File root, String df, String ff) {
    //Log.log("Scanning "+root,Log.INFO);
    if( root.isDirectory() ) {
      File e[] = root.listFiles();
      for( int i=0;i<e.length;i++) {
        if(e[i].getPath().matches(df) )
           scanDataSets(dirs, e[i],df,ff);
      }
    } else if (root.getName().matches(ff ) ) {
      File dir = root.getParentFile();
      List l = (List) dirs.get(dir);
      if( l== null ) {
        l= new LinkedList();
        dirs.put(dir,l);
      }
      l.add(root.getName());
      Collections.sort(l); // Well, a little overkill to do it every time
    }
  }

  public static void main(String[] args) throws IOException {
    new UseCases(args).start();
  }

}
