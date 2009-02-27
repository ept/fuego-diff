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

package fc.xml.xas;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public abstract class XmlPullFactory implements FormatFactory {

    private String type;
    
    protected XmlPullFactory (String type) {
	this.type = type;
    }

    public ParserSource createSource (InputStream in) throws IOException {
	XmlPullParser parser = createParser();
	return new XmlPullSource(parser, in);
    }

    public ParserSource createSource (InputStream in, StartTag context)
	    throws IOException {
	XmlPullParser parser = createParser();
	XmlPullSource source = new XmlPullSource(parser, in);
	source.setContext(context);
	return source;
    }

    public SerializerTarget createTarget (OutputStream out,
	    String encoding) throws IOException {
	XmlSerializer serializer = createSerializer();
	return new XmlPullTarget(serializer, type, out, encoding);
    }

    protected abstract XmlPullParser createParser ();

    protected abstract XmlSerializer createSerializer ();

}

// arch-tag: 364f3c7d-a250-4b1d-b9e6-388d859b6bb2
