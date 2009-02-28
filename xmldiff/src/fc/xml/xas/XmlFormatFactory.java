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
import java.io.OutputStream;

import org.kxml2.io.KXmlParser;
import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

public class XmlFormatFactory extends XmlPullFactory {

    public XmlFormatFactory () {
	super(XasUtil.XML_MIME_TYPE);
    }

    @Override
    protected XmlPullParser createParser () {
	return new KXmlParser();
    }

    @Override
    protected XmlSerializer createSerializer () {
	return new KXmlSerializer();
    }

    public SerializerTarget createCanonicalTarget (OutputStream out)
	    throws IOException {
	return new XmlOutput(out, "UTF-8");
    }

}

// arch-tag: b69eaf92-4bad-444f-8401-8dfdb2020c43
