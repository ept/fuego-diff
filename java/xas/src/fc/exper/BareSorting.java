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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;

import org.kxml2.io.KXmlParser;

import fc.util.Measurer;
import fc.util.Util;
import fc.xml.xas.Item;
import fc.xml.xas.ItemList;
import fc.xml.xas.XasFragment;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.XmlPullSource;

public class BareSorting {

    public static void main (String[] args) {
	try {
	    if (args.length != 1) {
		System.err.println("Usage: BareSorting <file>");
		System.exit(1);
	    }
	    Measurer.init(Measurer.TIMING);
	    Measurer timer = Measurer.get(Measurer.TIMING);
	    String fileName = args[0];
	    String outName = fileName.concat(".bsort");
	    int end = 10;
	    for (int i = 0; i < end; i++ ) {
		Util.runGc();
		long beginMemory = Util.usedMemory();
		Object token = timer.start();
		ItemList list = new ItemList();
		XmlPullSource source = new XmlPullSource(new KXmlParser(),
			new FileInputStream(fileName));
		Item item;
		while ((item = source.next()) != null) {
		    list.append(item);
		}
		XasFragment fragment = list.fragment();
		list = null;
		if (i >= end - 2) {
		    timer.finish(token, "Reading");
		}
		long spentMemory = Util.usedMemory();
		Util.runGc();
		long endMemory = Util.usedMemory();
		System.out.println("Reading memory: "
			+ (spentMemory - beginMemory));
		System.out.println("Fragment size: "
			+ (endMemory - beginMemory));
		token = timer.start();
		List<Item> items = fragment.getFragmentContent();
		Collections.sort(items, new Sorting.TagCompare());
		fragment.setFragmentContent(items);
		if (i >= end - 2) {
		    timer.finish(token, "Sorting");
		}
		Util.runGc();
		beginMemory = Util.usedMemory();
		XmlOutput out = new XmlOutput(new FileOutputStream(outName),
			source.getEncoding());
		token = timer.start();
		fragment.appendTo(out);
		out.flush();
		if (i >= end - 2) {
		    timer.finish(token, "Output");
		}
		spentMemory = Util.usedMemory();
		Util.runGc();
		endMemory = Util.usedMemory();
		System.out.println("Performing output: " + (spentMemory - beginMemory));
		System.out.println("Output left: " + (endMemory - beginMemory));
		if (i >= end -2) {
		    timer.output(System.out);
		}
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
    }

}

// arch-tag: 9054c8bd-06a8-474a-ad55-6eabb10f5297
