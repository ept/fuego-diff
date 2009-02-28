/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import fc.util.IOExceptionTrap;
import fc.util.log.Log;
import fc.xml.xas.AttributeNode;
import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.XasFragment;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.XmlPullSource;
import fc.xml.xas.index.Document;
import fc.xml.xas.typing.TypedItem;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.KeyModel;
import fc.xml.xmlr.model.StringKey;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.test.RandomDirectoryTree.DirectoryEntry;
import fc.xml.xmlr.xas.DeweyKey;
import fc.xml.xmlr.xas.DeweyXasSource;
import fc.xml.xmlr.xas.IdAttributeXasSource;
import fc.xml.xmlr.xas.PeekableItemSource;
import fc.xml.xmlr.xas.UniformXasCodec;
import fc.xml.xmlr.xas.XasRefTree;
import fc.xml.xmlr.xas.XasSerialization;

public class XasTests extends TestCase implements IOExceptionTrap {

    // Stringkey generator
    private RandomDirectoryTree.KeyGen kg = new RandomDirectoryTree.KeyGen(0) {

        @Override
        public Key next() {
            return StringKey.createKey(this.id++);
        }
    };


    public XasTests() {
        super("XMLR XAS tests");
    }


    public void testCodecCycle() throws IOException {
        int RND_SEED = 314 * 42;
        double DIRP = 0.05;
        long TSIZE = 50;
        long DSIZE = 5;
        double DPROB = 0.1;
        double VAR = 5.0;
        double DVAR = 2.0;
        RefTree dt = RandomDirectoryTree.randomDirTree(TSIZE, DSIZE, DPROB, VAR, DVAR,
                                                       new Random(RND_SEED), kg);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlOutput t = new XmlOutput(out, "UTF-8");
        TreeModel tm = TreeModel.createIdAsStringKey(new DirTreeModel());
        XasSerialization.writeTree(dt, t, tm);
        t.flush();
        Log.debug("Encoded XML ", new String(out.toByteArray()));
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ItemSource is = new XmlPullSource(new KXmlParser(), in);
        RefTree dt2 = XasSerialization.readTree(is, tm);
        Assert.assertTrue("Round-tripped trees do not match", XmlrDebug.treeComp(dt, dt2));
        // Log.info("Original tree");
        // TreeUtil.dumpTree(dt);
        // Log.info("Re-read tree");
        // TreeUtil.dumpTree(dt2);
    }


    public void testXasRefTree() throws IOException {
        // Build a tree to read...
        Log.setLogger(new fc.util.log.SysoutLogger());
        int RND_SEED = 314 * 42;
        double DIRP = 0.05;
        long TSIZE = 5000;
        long DSIZE = 50;
        double DPROB = 0.1;
        double VAR = 5.0;
        double DVAR = 2.0;
        RefTree dt = RandomDirectoryTree.randomDirTree(TSIZE, DSIZE, DPROB, VAR, DVAR,
                                                       new Random(RND_SEED), kg);
        // dewify(dt.getRoot(),fc.xml.xas.index.DeweyKey.initial());
        RefTree dtd = new DeweyKeyedRefTree(dt, DeweyKey.ROOT_KEY.child(0));
        // TreeUtil.dumpTree(dtd);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlOutput xo = new XmlOutput(out, "UTF-8");
        XasSerialization.writeTree(dt, xo, TreeModel.createIdAsStringKey(new DirTreeModel()));
        xo.flush();
        // Build Xas doc on the stream
        XmlPullParser parser = new KXmlParser();
        ItemSource is = new XmlPullSource(parser, new ByteArrayInputStream(out.toByteArray()));
        XasFragment basedocf = convert(is);
        Document basedoc = new Document(basedocf);
        basedocf.treeify(); // TODO: 4JK: Shouldn't need to do this...
        DirTreeModel dtm = new DirTreeModel(KeyIdentificationModel.ID_AS_STRINGKEY);
        // NOTE: ugly override to DirTreeModel to get right getId() to the
        // content
        // Try the dewey-keyed variant
        XasRefTree tt = new XasRefTree(DeweyXasSource.createForRootTag(basedoc), dtm);
        tt.setTrap(this);
        Log.debug("Testing Dewey tree..");
        // XmlrDebug.dumpTree(tt);
        Assert.assertTrue("Re-parsed Dewey tree does not match original", XmlrDebug.treeComp(dtd,
                                                                                             tt));
        // TreeUtil.dumpTree(t);
        // And now the id-keyed
        Log.debug("Making id tree..");
        IdAttributeXasSource ixas = new IdAttributeXasSource.MemoryIdIndexSource(
                                                                                 basedoc,
                                                                                 IdAttributeXasSource.getRootPointer(basedoc));
        XasRefTree t = new XasRefTree(ixas, dtm);
        t.setTrap(this);
        // XmlrDebug.dumpTree(t);
        // XmlrDebug.dumpTree(dt);

        Log.debug("Testing id tree..");
        Assert.assertTrue("Re-parsed id tree does not match original", XmlrDebug.treeComp(dt, t));
        Log.debug("done..");
        t = null;
        tt = null;
        System.runFinalization();
    }


    public void testDeweyAddressableTree() throws Exception {
        XmlPullParser parser = new KXmlParser();
        ItemSource is = new XmlPullSource(parser, new FileInputStream("test/ebook/linux-intro.xml"));
        XasFragment basedocf = convert(is);
        Document basedoc = new Document(basedocf);
        basedocf.treeify(); // TODO: 4JK: Shouldn't need to do this...
        // Try the dewey-keyed variant
        XasRefTree t = new XasRefTree(new DeweyXasSource(basedoc), UniformXasCodec.ITEM_CODEC);
        /*
         * DeweyXasRefTree t = new DeweyXasRefTree( basedoc, XasSerialization.ITEM);
         */
        t.setTrap(this);
        DeweyKey k = DeweyKey.createKey("/2/3/1/0");
        RefTreeNode n = t.getNode(k);
        Log.info("Node at " + k + " is " + n);
        Log.info("Key at " + k + " is " + n.getId());
        Log.info("Content at " + k + " is " + (n != null ? n.getContent() : null));
        t = null;
        System.runFinalization();
        // Thread.sleep(2000);
    }


    // TODO Some sort of convenience method from ItemSource to Fragment
    private static XasFragment convert(ItemSource source) throws IOException {
        List<Item> items = new ArrayList<Item>();
        Item item = source.next();
        Item firstItem = item;
        while (item != null) {
            items.add(item);
            item = source.next();
        }
        // TODO: 4JK: This copies the list items;there should be a constructor
        // that builds on top of an existing list
        return new XasFragment(items, firstItem);
    }

    // TODO: 4JK Make some sort of base classes for easy write of
    // 1-n and n-1 transforms (in the former case, filtering at append() is
    // easy, in the latter at next()
    // This is a simple try at this
    public abstract static class AbstractItemTransform implements ItemTransform {

        protected Queue<Item> queue = new LinkedList<Item>();


        public boolean hasItems() {
            return queue.peek() != null;
        }


        public Item next() throws IOException {
            return queue.poll();
        }


        public abstract void append(Item item) throws IOException;

    }

    public static class EntryDecoder extends AbstractItemTransform {

        public static final Qname DUMMY_TYPE = new Qname("", "");
        public static final Qname ID_ATTR = new Qname("", "id");
        public static final Qname NAME_ATTR = new Qname("", "name");


        @Override
        public void append(Item i) throws IOException {
            if (i.getType() == Item.START_TAG) {
                StartTag t = (StartTag) i;
                String ts = t.getName().getName();
                String id = t.getAttribute(ID_ATTR).getValue().toString();
                AttributeNode name = t.getAttribute(NAME_ATTR);
                DirectoryEntry de = new DirectoryEntry(StringKey.createKey(id), name == null ? null
                        : name.getValue().toString(), "file".equals(ts) ? DirectoryEntry.FILE
                        : ("tree".equals(ts) ? DirectoryEntry.TREE : DirectoryEntry.DIR));
                queue.offer(i);
                queue.offer(new TypedItem(DUMMY_TYPE, de));
            } else {
                queue.offer(i);
                // Log.info("Skipping item ",i);
            }
        }

    }

    public static class EntryEncoder extends AbstractItemTransform {

        private Stack<StartTag> openTags = new Stack<StartTag>();


        @Override
        public void append(Item i) throws IOException {
            if (i.getType() == Item.START_TAG) return; // Just ignore it...
            else if (i.getType() == TypedItem.TYPED) {
                TypedItem t = (TypedItem) i;
                if (!(t.getValue() instanceof DirectoryEntry))
                    throw new IOException("Cannot encode data " + t.getValue().getClass());
                DirectoryEntry de = (DirectoryEntry) t.getValue();
                StartTag st = new StartTag(
                                           new Qname(
                                                     "",
                                                     de.getType() == DirectoryEntry.FILE ? "file"
                                                             : (de.getType() == DirectoryEntry.TREE ? "tree"
                                                                     : "directory")));
                st.addAttribute(EntryDecoder.ID_ATTR, de.getId().toString());
                if (de.getName() != null) // <tree> has no name
                    st.addAttribute(EntryDecoder.NAME_ATTR, de.getName());
                openTags.push(st);
                queue.offer(st);
                // queue.offer(new Text("\n")); // For prettyprinting
            } else if (i.getType() == Item.END_TAG) {
                // Add end tag matching the last emitted start one
                // (id we didn't encode type in tag name, this wouldn't be
                // necessary,
                // and we could just use a standard name here)
                /*
                 * if( openTags.isEmpty() ) { Log.warning("Empty stack",i); return; }
                 */
                StartTag t = openTags.pop();
                queue.offer(new EndTag(t.getName()));
            } else {
                queue.offer(i);
                // Log.warning("Unexpected item",i);
            }
        }
    }

    public static class DirTreeModel implements UniformXasCodec {

        KeyModel okim = null; // Overriding kim to get proper class for
        // de.getId(), when the node key is not correct
        EntryDecoder ed = new EntryDecoder();


        public DirTreeModel(KeyModel okim) {
            this.okim = okim;
        }


        public DirTreeModel() {
        }


        public Object decode(PeekableItemSource is, KeyIdentificationModel kim) throws IOException {
            // assert kim != null;
            // A bit kludge-ish .. we way we re-use the sequence encoder...
            Item i = is.next();
            ed.append(i);
            ed.next(); // The untouched ST
            // FIXME: A dummy wrapper node for the content is really a bit ugly
            // OTOH, we quite nicely got key decode here!
            TypedItem ti = (TypedItem) ed.next();
            DirectoryEntry de = null;
            if (ti != null) {
                de = (DirectoryEntry) ti.getValue();
                // de.setId((okim == null ? kim : okim) .identify(i));
                de.setId((okim == null ? kim : okim).makeKey(de.getId().toString()));
            }
            return de;
        }


        public void encode(ItemTarget t, RefTreeNode n, StartTag context) throws IOException {
            DirectoryEntry e = (DirectoryEntry) n.getContent();
            StartTag st = new StartTag(new Qname("", e.getType() == DirectoryEntry.FILE ? "file"
                    : (e.getType() == DirectoryEntry.DIR ? "directory" : "tree")), context);
            if (e.getType() != DirectoryEntry.TREE)
                st.addAttribute(EntryDecoder.NAME_ATTR, e.getName());
            st.addAttribute(EntryDecoder.ID_ATTR, e.getId().toString());
            t.append(st);
        }


        public int size() {
            return 1;
        }

    }


    public void trap(IOException ex) {
        Log.fatal("An I/O error occurred", ex);
        Assert.fail();
    }

}

// arch-tag: c2f3eec0-3c2f-4faf-8f62-fc1e73d1e2d2
//
