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

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import fc.raxs.RandomAccessXmlStore;
import fc.raxs.Store;
import fc.raxs.XasStore;
import fc.util.IOUtil;
import fc.util.log.Log;
import fc.util.log.SysoutLogger;
import fc.xml.xas.Comment;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xmlr.ChangeBuffer;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.xas.DeweyKey;

/** Test code that should be useable as basis for editor.
 */

public class DemoTest extends TestCase {
  public void testDemo() throws IOException, NodeNotFoundException, 
    InterruptedException {
    Log.setLogger(new SysoutLogger());
    File sf = new File("+demo.xml");
    //if( !sf.exists() )
      IOUtil.copyFile(new File("test/simple/simple.xml"), sf);
    // 1- Open store 
    // We use a store, where nodes are XAS ITEMS, organized into a tree as
    // dictated by ST and ET items, and keyed by Dewey Keys, with SD = /
    Store ds = XasStore.getStoreForFile(sf, XasStore.XAS_ITEM_TREE);
    RandomAccessXmlStore raxs = RandomAccessXmlStore.open(ds);
    Log.debug("The tree is");
    XmlrDebug.dumpTree(raxs.getTree());
    // 2- Do some changes
    // NOTE: Working with DeweyKey reftrees is straightforward, but be careful
    // with object comparisons: It is not necessarily true that
    // t.getNode(p) == t.getNode(p), as the underlying implementation may return
    // new on-demand created proxy objects on each invocation.
    // I'm noting this, because with DeweyKeys it's sometimes a bit hard to
    // track nodes by key, due to the changing keys. Thus, one may be tempted
    // to track by object identity, which won't work either..:(
    // Also note that any node object obtained from editable tree becomes invalid
    // as soon as an edit is made (because the edit may have e.g. 
    // detached that node from the tree) (Enable assertions to get bugged if you
    // break this rule)
    MutableRefTree t = raxs.getEditableTree();
    Log.debug("The edit-1 tree is");
    XmlrDebug.dumpTree(t);
    t.move(DeweyKey.createKey("/0"), t.getRoot().getId(), 2);
    StartTag ctag = new StartTag(new Qname("","file"));
    ctag.addAttribute(new Qname("","name"), "CHANGED-c.a");
    t.update(DeweyKey.createKey("/0/1"), ctag );
    t.insert(DeweyKey.createKey("/0"), 1, MutableRefTree.AUTO_KEY, 
        new Comment("The file tag below was changed"));
    t.insert(DeweyKey.createKey("/0"), 2, MutableRefTree.AUTO_KEY, 
        new Comment("This is an comment that will be deleted"));
    // Some interesting debug
    Log.debug("The edited tree is");
    XmlrDebug.dumpTree(t);
    Log.debug("The change tree is");
    // The line below makes an assumption about the implementation, and may
    // thus not work. But if it does, it nicely shows what changes have
    // been made. Issue 20061009-10
    XmlrDebug.dumpTree(((ChangeBuffer) t).getChangeTree()); 
    // 3- Write back
    // NOTE: Deltas a currently just kept in memory, so they are lost on restart 
    int version = raxs.commit(t);
    Log.debug("Committed version "+version);
    //Thread.sleep(10000);
    // 4- Da Capo, to make sure it works more than 1 time
    t = raxs.getEditableTree();
    Log.debug("The edit-2 tree is");
    XmlrDebug.dumpTree(t);
    t.delete(DeweyKey.createKey("/0/2")); // Delete the comment
    Log.debug("The edited-2 tree is");
    XmlrDebug.dumpTree(t);
    Log.debug("The change-2 tree is");
    // See  Issue 20061009-10
    XmlrDebug.dumpTree(((ChangeBuffer) t).getChangeTree()); 
    version = raxs.commit(t);
    Log.debug("Committed version "+version);
    // Get a previous version
    Log.debug("Recalled version "+(version-2));
    XmlrDebug.dumpTree(raxs.getTree(version-2));
    Log.debug("Recalled version "+(version-1));
    XmlrDebug.dumpTree(raxs.getTree(version-1));
    // 5 sync
    // TDB... :) 
  }
    
}

// arch-tag: 4f8347e7-570f-4f9e-af42-325dd2f506b0
