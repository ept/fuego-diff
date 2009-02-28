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

// $Id: SynteticDirTree.java,v 1.11 2006/04/05 14:58:47 ctl Exp $

package fc.xml.diff.benchmark;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Random;

import fc.util.Util;
import fc.util.log.Log;
import fc.util.log.SysoutLogger;
import fc.xml.diff.IoUtil;
import fc.xml.diff.test.DirTreeGenerator;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ParserSource;
import fc.xml.xas.TransformSource;
import fc.xml.xas.transform.DataItems;
import fc.xml.xmlr.ChangeTree;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.model.TreeModels;
import fc.xml.xmlr.xas.UniformXasCodec;
import fc.xml.xmlr.xas.XasSerialization;

public class SynteticDirTree extends BenchMark {

    
  public SynteticDirTree() {
    this(new String[] {});
  }
    
  public SynteticDirTree(String[] args) {
    super(args,"synthetic dirtree");
  }

  public void testDirTree() {
    start();
  }
  
  public void start() {
    // Make base file
    Log.setLogger(new SysoutLogger());
    long forceStop = getSetProperty("forcestop", Long.MAX_VALUE);
    long rndSeed = getSetProperty("seed", 422);
    long DSIZE = getSetProperty("dirsize", 25);
    double DPROB = getSetProperty("dirp",.1);
    double DELTREEP = getSetProperty("deltreep",.05);
    double VAR = getSetProperty("var",5.0);
    double DVAR =getSetProperty("dvar",2.0);
    Random rnd = new Random(rndSeed);
    int step = getSetProperty("tree.step",1000);
    int tstart = getSetProperty("tree.min",1000);
    int tend = getSetProperty("tree.max",10000);
    int tlaps = getSetProperty("tree.laps",2);
    int estep = getSetProperty("edits.step",5);
    int estart = getSetProperty("edits.min",1);
    int eend = getSetProperty("edits.max",21);
    int eslice = getSetProperty("edits.sliceatsize",-1);
    int enoslice = getSetProperty("edits.notinslice",-1);
    String pdf = getSetProperty("pdf","dimu");
    final String differ = getSetProperty("differ","!fc.xml.diff.Diff");
    String patcher = getSetProperty("patcher","!fc.xml.diff.Patch");
    String harvester = getSetProperty("reporter",null);
    String baseName = getSetProperty("basefile","b.xml");
    Log.log("basename="+baseName,Log.INFO);
    String newName = getSetProperty("newfile","n.xml");
    String diffName = getSetProperty("deltafile","d.xml");
    String diffLogFile=getSetProperty("difflog",null);
    boolean verify = Boolean.parseBoolean(getSetProperty("verify","true"));
    final long timeout = (long) (1000*getSetProperty("timeout",1000000000.0));
    for (int tsize = tstart; tsize < tend; tsize += step) {
      for (int lap = 0; lap < tlaps; lap++) {
        int rtsize = tsize; // + rnd.nextInt(step);
        MutableRefTree baset =
          DirTreeGenerator.randomDirTree(rtsize,
                                         DSIZE, DPROB, VAR, DVAR,
                                         new Random(rndSeed));
        // Make variants
        ChangeTree dt = new ChangeTree(baset);
        int realestart = eslice > 0 && tsize!=eslice ?  enoslice :estart;
        int realeend = eslice > 0 && tsize!=eslice ?  enoslice+1 :eend;
        int realestep = eslice > 0 && tsize!=eslice ?  1 :estep;

        for (int edits = realestart; edits < realeend; edits += realestep) {
          try {
            DirTreeGenerator.permutateTree(dt, edits, pdf, DELTREEP, rnd);
          } catch ( Exception ex ) {
            Log.log("Dirtree generator bombed; not computing this lap",Log.ERROR,ex);
            continue;
          }
          // Flush trees to disk
          File basef = null;
          File newf = null;
          File deltaf = null;
          try {
            basef = new File(workDir,baseName);
            IoUtil.writeRefTree(baset, basef, new DirTreeGenerator.DirTreeModel());
            newf = new File(workDir, newName);
            IoUtil.writeRefTree(dt, newf, new DirTreeGenerator.DirTreeModel());
            deltaf = new File(workDir, diffName);
          } catch (IOException ex) {
            Log.log("Error writing test trees. WorkDir="+workDir,
                Log.FATALERROR,ex);
          }
          long dstart=0, dend=-1;
          try {
            // Do a match test
            // Calm down
            if( deltaf.exists() && !deltaf.delete() )
              Log.log("Cannot remove old "+deltaf,Log.ERROR);
            /*for( int i=0;i<10;i++)
              try { System.gc(); Thread.sleep(100); }
              catch( InterruptedException ex){};*/
            dstart = System.currentTimeMillis();
            //Log.log("Starting differ:"+differ,Log.INFO);
            exec(differ,new String[] {basef.toString(), newf.toString(),
                            deltaf.toString()},true,diffLogFile,timeout);
            dend = System.currentTimeMillis();
            if( --forceStop == 0) {
              Log.log("===FORCED TEST STOP===",Log.INFO);
              return;
            }
            Log.log("Done in "+(dend-dstart)+" msec",Log.INFO);
          } catch ( Exception ex) {
            Log.log("FAILED: ",Log.ERROR,ex);
          }
          if( verify)
            verifyDiff(patcher, basef, newf, deltaf);
          try {
            System.setProperty("lap.time",String.valueOf(dend-dstart));
            System.setProperty("lap.edits",String.valueOf(edits));
            System.setProperty("lap.lap",String.valueOf(lap));
            System.setProperty("lap.treesize",String.valueOf(tsize));
            if (harvester != null) {
              exec(harvester, new String[] {},false,null,Long.MAX_VALUE);
            }
          } catch (IOException ex) {
            Log.log("Reporter excepted",Log.ERROR,ex);
          }
          dt.reset(); // Forget edits in this lap
        } // endfor edits
      } // endfor treesize
    }
  }

  // NOTE: The reason for reparsing newf is that we want a more genric
  // comparison for other uses. FIXME: This is actually bollox, we should
  // just take a contentcodec and compare by that model; but this requires
  // the objects created by contentcodec to be properly comparable.

  private void verifyDiff(String patcher,File basef, File newf, File deltaf) {
    InputStream vPatchIn=null, vNewIn=null;
    TreeModel tm = TreeModels.xmlr1Model().swapCodec(UniformXasCodec.ITEM_CODEC);
    try {
      File patchf = new File(workDir, "p.xml");
      if( patchf.exists() && !patchf.delete() )
        Log.log("Cannot remove old "+patchf,Log.ERROR);
      exec(patcher,new String[] {basef.toString(),deltaf.toString(),
       patchf.toString()},true,null,Long.MAX_VALUE);
      vPatchIn = new FileInputStream(patchf);
      // Read patched
      ParserSource ppr = IoUtil.getXmlParser(vPatchIn);
      ItemSource pes = new TransformSource(ppr, new DataItems());
      IdAddressableRefTree pT = RefTrees.getAddressableTree(
        XasSerialization.readTree(pes, tm));

      // Read new
      vNewIn = new FileInputStream(newf);
      ParserSource npr = IoUtil.getXmlParser(vNewIn);
      ItemSource nes = new TransformSource(npr, new DataItems());
      IdAddressableRefTree nT = RefTrees.getAddressableTree(
        XasSerialization.readTree(nes, tm));

      refTreeCmp(pT.getRoot(), nT.getRoot(),nT,null);
        // End verify
    } catch (IOException ex) {
      Log.log("Verify IOExcepted",Log.FATALERROR,ex);
    } finally {
      try {
        for (InputStream in : new InputStream[] {vPatchIn,vNewIn})
          if (in != null) {
          in.close();
        }
      } catch (IOException ex1) {
        Log.log("Can't close a verify stream",Log.FATALERROR,ex1);
      }
    }
  }

  public static void refTreeCmp(RefTreeNode n1, RefTreeNode n2,
                                IdAddressableRefTree base, Key rtId) {
    Object c1 = n1.getContent();
    Object c2 = n2.getContent();
    if (n1.isNodeRef()) {
      RefTreeNode refd = base.getNode(n1.getId());
      if( refd == null )
        Log.log("Broken noderef "+n1.getId(),Log.FATALERROR);
      c1 = refd.getContent();
    } else if (n2.isNodeRef()) {
      Log.log("May not have refs",Log.FATALERROR);
      /*if (!n2.getId().equals(n1.getId()))
        Log.log("Mismatching n2 noderef: " + n1.getId() + "," + n2.getId(),
                Log.FATALERROR);*/
    }
    if (!Util.equals(c1, c2)) {
      if (rtId != null)
        Log.log("The following error occured when comparing in treeref " + rtId,
                Log.ERROR);
      String c1s =  n1.isNodeRef() ? "#node ref "+n1.getId() : c1.toString();
      Log.log("Mismatching content " +
              "\nP:" + c1s +
              "\nF:" +
              c2.toString(),
              Log.FATALERROR);
    }
    for (Iterator i = n1.getChildIterator(), j = n2.getChildIterator();
                                                 i.hasNext() && j.hasNext(); ) {
      RefTreeNode cn1=(RefTreeNode) i.next(), cn2=(RefTreeNode) j.next();
      if( cn1.isTreeRef() ) {
        RefTreeNode refd = base.getNode(cn1.getId());
        if( refd == null )
          Log.log("Broken treeref "+cn1.getId(),Log.FATALERROR);
        refTreeCmp( refd, cn2, base, cn1.getId() );
      } else if (cn2.isTreeRef() ) {
        Log.log("May not have refs",Log.FATALERROR);
      } else
        refTreeCmp( cn1, cn2, base, null );
      if (i.hasNext() != j.hasNext())
        Log.log("Differing child count", Log.FATALERROR);
    }
  }

  public static void main(String[] args) {
    new SynteticDirTree(args).start();
  }

}
