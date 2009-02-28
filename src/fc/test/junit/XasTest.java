/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.test.junit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.kxml2.io.KXmlParser;

import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.FormatFactory;
import fc.xml.xas.Item;
import fc.xml.xas.ItemList;
import fc.xml.xas.ItemSource;
import fc.xml.xas.MutablePointer;
import fc.xml.xas.ParserSource;
import fc.xml.xas.Pointer;
import fc.xml.xas.Queryable;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.XasFragment;
import fc.xml.xas.XasUtil;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.XmlPullSource;
import fc.xml.xas.index.VersionedDocument;
import fc.xml.xas.index.VersionedPointer;
import fc.xml.xas.typing.Codec;
import fc.xml.xas.typing.DecodeSource;
import fc.xml.xas.typing.PrimitiveSource;
import fc.xml.xas.typing.TypingUtil;
import fc.xml.xas.typing.XmlCodec;

public class XasTest extends TestCase {

    List<XasFragment> fragments;
    List<ItemList> typeds;

    protected void setUp () throws IOException {
	fragments = XmlData.getData();
	typeds = XmlData.getTypedData();
	Codec.registerPrimitiveCodec(new XmlCodec());
	Codec.registerValueCodec(new XmlData.PersonCodec());
    }

    public void testTreeify () {
	Log.log("Begin test", Log.DEBUG);
	for (XasFragment f : fragments) {
	    f.treeify();
	    Log.log("Fragment", Log.DEBUG, f);
	}
    }

    // public void testFlattenPure () {
    // Log.log("Begin test", Log.DEBUG);
    // for (XasFragment f : fragments) {
    // XasFragment tree = f.treeifyPure();
    // XasFragment f2 = tree.flattenPure();
    // assertEquals("FlattenPure does not round-trip", f, f2);
    // }
    // }

    // public void testTreeifyPure () {
    // Log.log("Begin test", Log.DEBUG);
    // for (XasFragment f : fragments) {
    // XasFragment t = f.treeifyPure();
    // XasFragment f2 = t.flattenPure();
    // XasFragment t2 = f2.treeifyPure();
    // assertEquals("TreeifyPure does not round-trip", t, t2);
    // }
    // }

    // public void testIdempotency () {
    // Log.log("Begin test", Log.DEBUG);
    // for (XasFragment f : fragments) {
    // XasFragment flat = f.flattenPure();
    // XasFragment tree = f.treeifyPure();
    // assertEquals("FlattenPure not idempotent", flat, flat.flattenPure());
    // assertEquals("TreeifyPure not idempotent", tree, tree.treeifyPure());
    // }
    // }

    private void ioRoundTrip (XasFragment f) throws IOException {
	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	XmlOutput target = new XmlOutput(bout, "UTF-8");
	Iterator<Item> it = f.iterator();
	while (it.hasNext()) {
	    target.append(it.next());
	}
	byte[] init = bout.toByteArray();
	ByteArrayInputStream bin = new ByteArrayInputStream(init);
	KXmlParser parser = new KXmlParser();
	XmlPullSource source = new XmlPullSource(parser, bin);
	bout = new ByteArrayOutputStream();
	target = new XmlOutput(bout, "UTF-8");
	for (Item item = source.next(); item != null; item = source.next()) {
	    target.append(item);
	}
	byte[] result = bout.toByteArray();
	assertEquals("Input/output round trip failed",
	    new String(init, "UTF-8"), new String(result, "UTF-8"));
    }

    public void testIoRoundTrip () throws IOException {
	Log.log("Begin test", Log.DEBUG);
	for (XasFragment f : fragments) {
	    ioRoundTrip(f);
	    // ioRoundTrip(f.flattenPure());
	    // ioRoundTrip(f.treeifyPure());
	}
    }

    public void testTyping () throws IOException {
	Log.log("Begin test", Log.DEBUG);
	for (String type : XasUtil.factoryTypes()) {
	    for (ItemList source : typeds) {
		Log.debug("Source fragment", source);
		ItemList target = new ItemList();
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		FormatFactory factory = XasUtil.getFactory(type);
		SerializerTarget serTarget =
		    factory.createTarget(bout, "UTF-8");
		XasUtil.copy(source.source(), serTarget);
		serTarget.flush();
		byte[] result = bout.toByteArray();
		Log.log("Document", Log.DEBUG, Util.toPrintable(result));
		ByteArrayInputStream bin = new ByteArrayInputStream(result);
		ParserSource parserSource = factory.createSource(bin);
		ItemSource primitiveSource =
		    new PrimitiveSource(parserSource, type, "UTF-8");
		ItemSource decodeSource = new DecodeSource(primitiveSource);
		XasUtil.copy(decodeSource, target);
		Log.debug("Target fragment", target);
		assertEquals("Type information did not round-trip with type "
		    + type, source, target);
	    }
	    typeds = XmlData.getTypedData();
	}
    }


    public void testQuery () throws IOException {
	Log.log("Begin test", Log.DEBUG);
	for (Queryable fragment : XmlData.getTrees()) {
	    if (fragment instanceof Iterable) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		XmlOutput xout = new XmlOutput(bout, "UTF-8");
		Iterable<Item> f = (Iterable<Item>) fragment;
		for (Item i : f) {
		    xout.append(i);
		}
		xout.flush();
		Log.log("Document", Log.DEBUG, new String(bout.toByteArray(),
		    "UTF-8"));
	    }
	    int[] k0 = new int[] {};
	    int[] k1 = new int[] { 0 };
	    int[] k2 = new int[] { 0, 1 };
	    int[] k3 = new int[] { 0, 2 };
	    int[] k4 = new int[] { 0, 1, 1 };
	    Pointer p0 = fragment.query(k0);
	    Pointer p1 = fragment.query(k1);
	    Pointer p2 = fragment.query(k2);
	    Pointer p3 = fragment.query(k3);
	    Pointer p4 = fragment.query(k4);
	    assertNotNull(p0);
	    assertNotNull(p1);
	    assertNotNull(p2);
	    assertNull(p3);
	    assertNotNull(p4);
	    p0.canonicalize();
	    p1.canonicalize();
	    p2.canonicalize();
	    p4.canonicalize();
	    assertEquals(XmlData.sd, p0.get());
	    assertEquals(XmlData.s0, p1.get());
	    assertEquals(XmlData.s01, p2.get());
	    assertEquals(XmlData.s011, p4.get());
	}
    }

    public void testModify () throws IOException {
	Log.log("Begin test", Log.DEBUG);
	for (Queryable fragment : XmlData.getTrees()) {
	    boolean isVer = fragment instanceof VersionedDocument;
	    int[] k001 = new int[] { 0, 0, 1 };
	    int[] k011 = new int[] { 0, 1, 1 };
	    int[] k012 = new int[] { 0, 1, 2 };
	    MutablePointer p001 = (MutablePointer) fragment.query(k001);
	    VersionedPointer q001 = null;
	    if (isVer) {
		q001 = (VersionedPointer) fragment.query(k001);
	    }
	    MutablePointer p011 = (MutablePointer) fragment.query(k011);
	    MutablePointer p012 = (MutablePointer) fragment.query(k012);
	    VersionedPointer q012 = null;
	    if (isVer) {
		q012 = (VersionedPointer) fragment.query(k012);
	    }
	    assertNotNull(p001);
	    assertNotNull(p011);
	    assertNotNull(p012);
	    if (isVer) {
		assertNotNull(q001);
		assertNotNull(q012);
	    }
	    p011.insert(XmlData.getFragment());
	    p001.move(p012);
	    p012.delete();
	    if (isVer) {
		assertFalse(q012.isValid());
		assertNotNull(p001.get());
		assertNotNull(q001.get());
		assertEquals(p001.get(), q001.get());
	    }
	    if (fragment instanceof Iterable) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		XmlOutput xout = new XmlOutput(bout, "UTF-8");
		Iterable<Item> f = (Iterable<Item>) fragment;
		for (Item i : f) {
		    xout.append(i);
		}
		xout.flush();
		Log.log("Fragment", Log.DEBUG, fragment);
		Log.log("Result", Log.DEBUG, new String(bout.toByteArray(),
		    "UTF-8"));
	    }
	}
    }

}

// arch-tag: 83a93f33-d3ff-4470-80b7-aee628db0f57
