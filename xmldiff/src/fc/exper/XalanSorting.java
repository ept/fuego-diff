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

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import fc.util.Measurer;
import fc.util.Util;

public class XalanSorting {

    public static void main (String[] args) {
	try {
	    if (args.length != 3) {
		System.err.println("Usage: XalanSorting <xsl> <in> <out>");
		System.exit(1);
	    }
	    Measurer.init(Measurer.TIMING);
	    Measurer timer = Measurer.get(Measurer.TIMING);
	    TransformerFactory factory = TransformerFactory.newInstance();
	    Transformer transformer = factory.newTransformer(new StreamSource(
		    args[0]));
	    int end = 10;
	    for (int i = 0; i < end; i++ ) {
		Util.runGc();
		long beginMemory = Util.usedMemory();
		Object token = timer.start();
		transformer.transform(new StreamSource(args[1]),
			new StreamResult(args[2]));
		if (i >= end - 2) {
		    timer.finish(token, "Transform");
		    long endMemory = Util.usedMemory();
		    System.out.println("Memory: " + (endMemory - beginMemory));
		    timer.output(System.out);
		}
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
    }

}

// arch-tag: 7e41090a-42c8-4b31-a9bc-f8abd763b325
