/*
 * Copyright 2006 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import java.util.Map;
import java.util.HashMap;
import java.io.OutputStream;
import java.io.IOException;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.xmlpull.v1.XmlSerializer;

/**
 * A wrapper class to write with SAX events with XmlPull.  This class
 * wraps a <code>XmlSerializer</code> and implements the SAX {@link
 * ContentHandler} interface, which is useful for output.  The intent
 * is to allow legacy SAX applications to use an XmlPull-based
 * serializer without altering their own code.  New classes are
 * recommended to use the {@link EventSequence} interface and its
 * implementing classes to handle XML.
 */
public class SaxWriter implements ContentHandler {

    private static Log log = LogFactory.getLog(SaxWriter.class.getName());

    protected XmlSerializer target;
    private Map prefixes = new HashMap();

    public SaxWriter (XmlSerializer target) {
	this(target, null);
    }

    public SaxWriter (XmlSerializer target, OutputStream os) {
	if (log.isDebugEnabled()) {
	    log.debug("Constructor: target=" + target + ", os=" + os);
	}
	this.target = target;
	if (os != null) {
	    try {
		target.setOutput(os, "ISO-8859-1");
	    } catch (IOException ex) {
		/*
		 * It is nonsensical for this method to throw exceptions,
		 * since this should only prepare for output, not actually
		 * output anything.
		 */
	    }
	}
    }

    public void setDocumentLocator (Locator locator) {
    }

    public void startDocument () throws SAXException {
	if (log.isDebugEnabled()) {
	    log.debug("startDocument()");
	}
	if (target != null) {
	    try {
		target.startDocument("ISO-8859-1", null);
	    } catch (IOException ex) {
		throw new SAXException(ex);
	    }
	}
    }

    public void endDocument () throws SAXException {
	if (log.isDebugEnabled()) {
	    log.debug("endDocument()");
	}
	if (target != null) {
	    try {
		target.endDocument();
		target.flush();
	    } catch (IOException ex) {
		throw new SAXException(ex);
	    }
	}
    }

    public void startPrefixMapping (String prefix, String uri)
	throws SAXException {
	if (log.isDebugEnabled()) {
	    log.debug("startPrefixMapping(" + prefix + ", " + uri + ")");
	}
	prefixes.put(prefix, uri);
	if (target != null) {
	    try {
		target.setPrefix(prefix, uri);
	    } catch (IOException ex) {
		throw new SAXException(ex);
	    }
	}
    }

    public void endPrefixMapping (String prefix) throws SAXException {
	if (log.isDebugEnabled()) {
	    log.debug("endPrefixMapping(" + prefix + ")");
	}
	prefixes.remove(prefix);
    }

    public void startElement (String namespaceURI, String localName,
			      String qName, Attributes attributes)
	throws SAXException {
	if (log.isDebugEnabled()) {
	    log.debug("startElement(" + namespaceURI + ", " + localName
		      + ", " + qName + ")");
	}
	if (target != null) {
	    try {
		if (localName == null || localName.equals("")) {
		    int index = qName.indexOf(':');
		    if (index >= 0) {
			String uri = (String)
			    prefixes.get(qName.substring(0, index));
			if (uri != null) {
			    namespaceURI = uri;
			}
		    }
		    localName = qName.substring(index + 1);
		}
		target.startTag(namespaceURI, localName);
		if (attributes != null) {
		    int length = attributes.getLength();
		    for (int i = 0; i < length; i++) {
                        String localAttName=attributes.getLocalName(i);
                        String attUri=attributes.getURI(i);
                        String attQName = attributes.getQName(i);
                        if( localAttName == null || localAttName.length() == 0) {
                          int index = attQName.indexOf(':');
                          if (index >= 0) {
                            String uri = (String)
                                prefixes.get(attQName.substring(0, index));
                            if (uri != null) {
                              attUri = uri;
                            }
                          }
                          localAttName = attQName.substring(index + 1);
                        }
                        target.attribute(attUri, localAttName,
                                         attributes.getValue(i));
		    }
		}
	    } catch (IOException ex) {
		throw new SAXException(ex);
	    }
	}
    }

    public void endElement (String namespaceURI, String localName,
			    String qName)
	throws SAXException {
	if (log.isDebugEnabled()) {
	    log.debug("endElement(" + namespaceURI + ", " + localName
		      + ", " + qName + ")");
	}
	if (target != null) {
	    try {
		if (localName == null || localName.equals("")) {
		    int index = qName.indexOf(':');
		    if (index >= 0) {
			String uri = (String)
			    prefixes.get(qName.substring(0, index));
			if (uri != null) {
			    namespaceURI = uri;
			}
		    }
		    localName = qName.substring(index + 1);
		}
		target.endTag(namespaceURI, localName);
	    } catch (IOException ex) {
		throw new SAXException(ex);
	    }
	}
    }

    public void characters (char[] ch, int start, int length)
	throws SAXException {
	if (log.isDebugEnabled()) {
	    log.debug("characters(" + new String(ch, start, length) + ")");
	}
	if (target != null) {
	    try {
		target.text(ch, start, length);
	    } catch (IOException ex) {
		throw new SAXException(ex);
	    }
	}
    }

    public void ignorableWhitespace (char[] ch, int start, int length)
	throws SAXException {
    }

    public void processingInstruction (String target, String data)
	throws SAXException {
    }

    public void skippedEntity (String name) throws SAXException {
    }

}
