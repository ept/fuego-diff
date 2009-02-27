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

package fc.exper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import fc.util.Measurer;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.AttributeNode;
import fc.xml.xas.FragmentItem;
import fc.xml.xas.Item;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.index.DeweyKey;
import fc.xml.xas.index.Index;
import fc.xml.xas.index.LazyFragment;
import fc.xml.xas.index.SeekableKXmlSource;

public class Sorting {

    private static SeekableKXmlSource source = null;
    
    private static DeweyKey parseKey (String keyStr, DeweyKey current,
	    int index) {
	if (keyStr.length() <= index) {
	    return current;
	} else {
	    int i = keyStr.indexOf('/', index);
	    if (i > index) {
		int c = Integer.parseInt(keyStr.substring(index, i));
		return parseKey(keyStr, current.child(c), i + 1);
	    } else {
		int c = Integer.parseInt(keyStr.substring(index));
		return current.child(c);
	    }
	}
    }

    public static void main (String[] args) {
	try {
	    if (args.length < 2 || args.length > 5) {
		System.err.println("Usage: Sorting <file> <key>"
			+ " [<index type> [<index depth> [<index file>]]]");
		System.exit(1);
	    }
	    Measurer.init(Measurer.TIMING);
	    Measurer timer = Measurer.get(Measurer.TIMING);
	    String fileStr = args[0];
	    String outStr = fileStr + ".out";
	    String keyStr = args[1];
	    boolean elem = args.length > 2 && args[2].equals("elem");
	    int depth = args.length > 3 ? Integer.parseInt(args[3])
		    : Integer.MAX_VALUE;
	    if (depth <= 0) {
		depth = Integer.MAX_VALUE;
	    }
	    File indexFile = null;
	    if (args.length > 4) {
		indexFile = new File(args[4]);
	    }
	    DeweyKey key = parseKey(keyStr, DeweyKey.root(), 1);
	    Log.log("Key", Log.INFO, key);
	    byte[] input = null;
	    int end = 10;
	    for (int i = 0; i < end; i++ ) {
		Util.runGc();
		long beginMemory = Util.usedMemory();
		Index index;
		Object totalToken = timer.start();
		Object token = timer.start();
		if (indexFile != null && indexFile.exists()) {
		    InputStream base = null;
		    if (System.getProperty("fc.test.slurp") != null) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			FileInputStream fin = new FileInputStream(indexFile);
			byte[] buffer = new byte[4096];
			int n;
			while ((n = fin.read(buffer, 0, buffer.length)) > 0) {
			    bout.write(buffer, 0, n);
			}
			base = new ByteArrayInputStream(bout.toByteArray());
		    } else {
			base = new BufferedInputStream(new FileInputStream(
				indexFile), 4096);
		    }
		    ObjectInputStream in = new ObjectInputStream(base);
		    index = (Index) in.readObject();
		    in.close();
		} else {
		    if (System.getProperty("fc.test.slurp") != null) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			FileInputStream fin = new FileInputStream(fileStr);
			byte[] buffer = new byte[4096];
			int n;
			while ((n = fin.read(buffer, 0, buffer.length)) > 0) {
			    bout.write(buffer, 0, n);
			}
			input = bout.toByteArray();
			source = new SeekableKXmlSource(input, 0, input.length);
		    } else {
			source = new SeekableKXmlSource(fileStr);
		    }
		    index = elem ? Index.buildElement(source, depth) : Index
			    .buildFull(source, depth);
		}
		if (i >= end - 2) {
		    timer.finish(token, "Index building");
		    timer.finish(totalToken, "Total time");
		}
		long spentMemory = Util.usedMemory();
		Util.runGc();
		long endMemory = Util.usedMemory();
		if (i >= end - 2) {
		    System.out.println("Index building: " + (spentMemory - beginMemory));
		    System.out.println("Index size: "
			    + (endMemory - beginMemory));
		    // System.out.println("RIS: " + fc.util.RaInputStream.count);
		    // System.out.println("U8R: " + fc.util.Utf8Reader.count);
		    if (Log.isEnabled(Log.TRACE)) {
			Log.log("Index", Log.TRACE, index);
		    } else {
			Log.log("Indexed entries", Log.INFO, index.size());
		    }
		}
		totalToken = timer.start();
		if (input != null) {
		    source = new SeekableKXmlSource(input, 0, input.length);
		} else {
		    source = new SeekableKXmlSource(fileStr);
		}
		index.setSource(source);
		if (i >= end - 2) {
		    timer.finish(totalToken, "Total time");
		}
		Util.runGc();
		beginMemory = Util.usedMemory();
		totalToken = timer.start();
		token = timer.start();
		Item firstItem = source.next();
		DeweyKey firstKey = DeweyKey.initial();
		DeweyKey prevKey = firstKey;
		while (!Item.isStartTag(firstItem)) {
		    prevKey = firstKey;
		    firstItem = source.next();
		    firstKey = firstKey.next();
		}
		source.setPosition(source.getPreviousPosition(), null);
		LazyFragment fragment = new LazyFragment(index, prevKey, firstItem);
		if (i >= end - 2) {
		    timer.finish(token, "Lazy fragment building");
		    timer.finish(totalToken, "Total time");
		}
		spentMemory = Util.usedMemory();
		Util.runGc();
		endMemory = Util.usedMemory();
		if (i >= end - 2) {
		    System.out.println("Lazy fragment building: " + (spentMemory - beginMemory));
		    System.out.println("Lazy fragment size: "
			    + (endMemory - beginMemory));
		}
		Log.log("Fragment", Log.DEBUG, fragment);
		Util.runGc();
		beginMemory = Util.usedMemory();
		totalToken = timer.start();
		token = timer.start();
		FragmentItem sortable = fragment.force(key);
		if (i >= end - 2) {
		    timer.finish(token, "Fragment forcing");
		    timer.finish(totalToken, "Total time");
		}
		spentMemory = Util.usedMemory();
		Util.runGc();
		endMemory = Util.usedMemory();
		if (i >= end - 2) {
		    System.out.println("Fragment forcing: " + (spentMemory - beginMemory));
		    System.out.println("Forced fragment size: "
			    + (endMemory - beginMemory));
		    Log.log("Forced fragment", Log.DEBUG, sortable);
		    // System.out.println("RIS: " + fc.util.RaInputStream.count);
		    // System.out.println("U8R: " + fc.util.Utf8Reader.count);
		}
		totalToken = timer.start();
		token = timer.start();
		if (sortable != null) {
		    List<Item> items = sortable.getFragmentContent();
		    if (items != null) {
			Collections.sort(items, new TagCompare());
			sortable.setFragmentContent(items);
		    }
		}
		if (i >= end - 2) {
		    timer.finish(token, "Sorting in place");
		    timer.finish(totalToken, "Total time");
		    Log.log("Sorted fragment", Log.DEBUG, sortable);
		}
		Util.runGc();
		beginMemory = Util.usedMemory();
		XmlOutput out = new XmlOutput(new FileOutputStream(outStr),
			source.getEncoding());
		totalToken = timer.start();
		token = timer.start();
		fragment.appendTo(out);
		out.flush();
		if (i >= end - 2) {
		    timer.finish(token, "Output");
		}
		if (indexFile != null && !indexFile.exists()) {
		    token = timer.start();
		    ObjectOutputStream oos = new ObjectOutputStream(
			    new BufferedOutputStream(new FileOutputStream(
				    indexFile), 4096));
		    oos.writeObject(index);
		    oos.flush();
		    oos.close();
		    if (i >= end - 2) {
			timer.finish(token, "Index output");
		    }
		}
		if (i >= end - 2) {
		    timer.finish(totalToken, "Total time");
		}
		spentMemory = Util.usedMemory();
		Util.runGc();
		endMemory = Util.usedMemory();
		if (i >= end - 2) {
		    System.out.println("Performing output: " + (spentMemory - beginMemory));
		    System.out.println("Output left: " + (endMemory - beginMemory));
		    timer.output(System.out);
		    // System.out.println("RIS: " + fc.util.RaInputStream.count);
		    // System.out.println("U8R: " + fc.util.Utf8Reader.count);
		}
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
    }

    static class TagCompare implements Comparator<Item> {

	private static final String keyAttribute = System.getProperty(
		"fc.test.xassortkey", "key");
	private static final Qname NAME_NAME = new Qname("", keyAttribute);

	public int compare (Item i1, Item i2) {
	    int t1 = i1.getType();
	    int t2 = i2.getType();
	    if (t1 != t2) {
		return t1 - t2;
	    } else if (FragmentItem.isFragment(i1)) {
		return compare(((FragmentItem) i1).get(0), ((FragmentItem) i2)
			.get(0));
	    } else if (Item.isStartTag(i1)) {
		StartTag st1 = (StartTag) i1;
		StartTag st2 = (StartTag) i2;
		AttributeNode a1 = st1.getAttribute(NAME_NAME);
		AttributeNode a2 = st2.getAttribute(NAME_NAME);
		if (a1 == null) {
		    return a2 == null ? 0 : -1;
		} else if (a2 == null) {
		    return 1;
		} else {
		    String v1 = (String) a1.getValue();
		    String v2 = (String) a2.getValue();
		    return v1.compareTo(v2);
		}
	    } else {
		return 0;
	    }
	}

	public boolean equals (Object o) {
	    return o instanceof TagCompare;
	}

    }

}

// arch-tag: d58d88e2-3899-41eb-9b43-601df2626885
