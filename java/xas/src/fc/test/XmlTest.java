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

package fc.test;

import java.io.FileInputStream;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.XmlPullSource;

public class XmlTest {

    public static void main (String[] args) {
	try {
	    for (String fileName : args) {
		XmlPullParser parser = new KXmlParser();
		ItemSource is =
		    new XmlPullSource(parser,
				      new FileInputStream(fileName));
		ItemTarget it = new XmlOutput(System.out, "ISO-8859-1");
		for (Item i = is.next(); i != null; i = is.next()) {
		    System.out.println(i);
		    it.append(i);
		}
		System.out.println();
		System.out.println();
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

}

// arch-tag: 33a2cb98-974c-4793-85a6-314c9a11d645
