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

package fc.xml.diff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import fc.util.log.Log;
import fc.util.log.StreamLogger;
import fc.xml.diff.encode.XmlDiffEncoder;
import fc.xml.xas.AttributeNode;
import fc.xml.xas.IdentityTransform;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.ParserSource;
import fc.xml.xas.StartTag;
import fc.xml.xas.TransformSource;
import fc.xml.xas.TransformTarget;
import fc.xml.xas.XasDebug;
import fc.xml.xas.XasUtil;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.transform.CoalesceContent;
import fc.xml.xas.transform.DataItems;
import fc.xml.xas.transform.NsPrefixFixer;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.NodeReference;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.TreeReference;
import fc.xml.xmlr.model.StringKey;
import fc.xml.xmlr.model.TransientKey;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.model.TreeModels;
import fc.xml.xmlr.tdm.Diff;
import fc.xml.xmlr.xas.DeweyKey;
import fc.xml.xmlr.xas.DeweyRefNode;
import fc.xml.xmlr.xas.MutableDeweyRefTree;
import fc.xml.xmlr.xas.PeekableItemSource;
import fc.xml.xmlr.xas.RefItem;
import fc.xml.xmlr.xas.RefNodeItem;
import fc.xml.xmlr.xas.RefTreeItem;
import fc.xml.xmlr.xas.ReferenceItemTransform;
import fc.xml.xmlr.xas.XasSerialization;

public class Patch {
  public Patch() {
  }

  public static void patch(File basef, File patchf, OutputStream pout ) {
    InputStream vBaseIn =null, vDiffIn=null;
    TreeModel tm = TreeModels.xasItemTree();
    try {
      //try { Thread.sleep(10000); } catch ( InterruptedException ex){}
      // Verify
      vBaseIn = new FileInputStream(basef);
      vDiffIn = new FileInputStream(patchf);
      // Read base using xpath ids
      ParserSource bpr = IoUtil.getXmlParser(vBaseIn);
      ItemSource  bes = makeInputFilter(bpr);
      TreeModel baseXpCC = tm.swapNodeModel(DeweyRefNode.NODE_MODEL_ALT);
      RefTree bbT = XasSerialization.readTree(bes, baseXpCC);
      //Main.structDump(bbT.getRoot(),0);
      IdAddressableRefTree bT = RefTrees.getAddressableTree(bbT);
      //Log.debug("base tree:");
      //XmlrDebug.dumpTree(bT,Object.class);
      // Load diff
      //Log.log("============LOADING DIFF=============",Log.INFO);
      ParserSource dpr = IoUtil.getXmlParser(vDiffIn);
      // FIXME-20061113-4: Concatenation on T() by filter
/*      ItemSource des1 = 
        new TransformSource(new TransformSource(dpr,new DataItems(true,true)),
            new CoalesceContent(false));
      XasUtil.copy(des1, XasDebug.itemDump());
      // XasUtil.copy(des1, XasUtil.SINK_TARGET );
      System.exit(0);*/

      ItemSource des1 = new TransformSource(
          new TransformSource(makeInputFilter(dpr),new ReferenceItemTransform()),
          new Diff.RelativeDeweyKeyExpander());
      PeekableItemSource des = new PeekableItemSource(des1);
      boolean isRefTree = false;
      Item fi = des.peek();
      //Log.debug("First item is",fi);
      if( RefItem.isRefItem(fi) )
        isRefTree = true; // Detected reftree format
      //System.out.println(des);*/
      RefTree rT = null;
      if( isRefTree ) {
        Log.debug("Diff looks like a reftree, decoding as that");
        rT = XasSerialization.readTree(des, tm);
      } else {
        Diff dT = 
          Diff.readDiff(des,tm, bT.getRoot().getId());
        //XmlrDebug.dumpTree(dT);
        rT = dT.decode(bT);
      }
      //XmlrDebug.dumpTree(rT,Object.class);
      // We could do all sorts of memory saving fanciness here, but instead we
      // just use good ole reftrees expand
      // KLUDGE BUG-20070621-1: decodeDiff should allow key mode for decoded
      // tree.
      // Here we hack around a tree with a single treeref keyed by a transient
      // key, as .apply will try to use that key as the tree root
      if( rT.getRoot().getId() instanceof TransientKey ) { 
        MutableDeweyRefTree mrT = new MutableDeweyRefTree();
        mrT.insert(null, DeweyKey.createKey("/0"), 
            rT.getRoot().getReference()  );
        rT=mrT;
      }
      
      MutableDeweyRefTree patched = new MutableDeweyRefTree();
      patched.apply(bT);
      patched.apply(rT);
      
      /* BUG-20070621-2: expandRefs doesn't work with Dewey keys
      RefTree patched = RefTrees.expandRefs(rT,Collections.EMPTY_SET,
                        Collections.EMPTY_SET,bT);*/
      ItemTarget wr = new TransformTarget(new XmlOutput(pout, "UTF-8"),
          new NsPrefixFixer());
      XasSerialization.writeTree(patched, wr, baseXpCC);
      // End verify
    } catch ( NodeNotFoundException ex ) {
      Log.log("Patch references illegal node "+ex.getId(),Log.ERROR,ex);
    } catch ( FileNotFoundException ex ) {
      Log.log(ex.getMessage(),Log.ERROR);
    } catch (IOException ex) {
      Log.log("Patch I/O exception",Log.FATALERROR,ex);
    } finally {
      try {
        for (InputStream in : new InputStream[] {vBaseIn,  vDiffIn})
          if (in != null) {
          in.close();
        }
      } catch (IOException ex1) {
        Log.log("Can't close an input file stream",Log.FATALERROR,ex1);
      }
    }
  }

  private static TransformSource makeInputFilter(ItemSource s) {
    // NOTE: DO NOT change the order of the filters -- think what will
    // happen to C(x)COMMENT(y)C(z) in the other order
    return new TransformSource(new TransformSource(s,new DataItems(true,true)),
        new CoalesceContent(false));
  }
  
  public static void main(String[] args) throws IOException {
    Log.setLogger(new StreamLogger(System.err));
    if( args.length < 2 ) {
      Log.log("Usage base.xml diff.xml [patched.xml]", Log.ERROR);
      System.exit(1);
    }
    OutputStream dout = System.out;
    try {
      if (args.length > 2 && !"-".equals(args[2]))
        dout = new FileOutputStream(args[2]);
      patch(new File(args[0]), new File(args[1]), dout);
    } catch (IOException ex) {
      Log.log("IO error while patching", Log.ERROR, ex);
    } finally {
      if (dout != System.out)
        dout.close();
    }
  }

}
