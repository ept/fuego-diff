/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fc.util.log.Log;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class provides conversion from a XAS EventSequence to SAX events.
 */

// ctl: sorry for not writing tip-top code, this is because im in a slight
// hurry...
public class SaxBridge {

    protected EventSequence es;


    public SaxBridge(EventSequence source) {
        es = source;
    }


    public static void output(EventSequence es, ContentHandler contentHandler) throws SAXException,
            IOException {
        (new SaxBridge(es)).output(contentHandler);
    }


    public void output(ContentHandler contentHandler) throws SAXException, IOException {
        if (es == null) { throw new SAXException("Must have an EventSequence to output"); }
        Map prefixMapping = new HashMap();
        XmlReader r = new XmlReader(es);
        int[] textParams = new int[2];
        for (Event e = r.advance(); e != null && e.getType() != Event.END_DOCUMENT; e = r.advance()) {
            int eventType = e.getType();
            switch (eventType) {
                case Event.START_DOCUMENT: {
                    Log.debug("START_DOCUMENT");
                    if (contentHandler != null) {
                        contentHandler.startDocument();
                    }
                    break;
                }
                case Event.NAMESPACE_PREFIX: {
                    String prefix = e.getValue().toString();
                    String uri = e.getNamespace();
                    contentHandler.startPrefixMapping(prefix, uri);
                    prefixMapping.put(uri, prefix);
                    Log.debug("PM(" + e.getNamespace() + ", " + e.getValue() + ")");
                    break;
                }
                case Event.START_ELEMENT: {
                    String namespace = e.getNamespace();
                    String localName = e.getName();
                    String prefix = (String) prefixMapping.get(namespace);
                    AttributesImpl atts = new AttributesImpl();
                    Log.debug("START_TAG(" + namespace + ", " + localName + ")");
                    // ctl: Scan attributes; is there any other event that may
                    // occur inside attributes than COMMENT?
                    for (Event e2 = null; (e2 = r.advance()) != null;) {
                        if (e2.getType() == Event.ATTRIBUTE) {
                            atts.addAttribute(makeNameSpace(prefixMapping, e2.getNamespace(),
                                                            e2.getName()),
                                              makeName(prefixMapping, e2.getNamespace(),
                                                       e2.getName()), makeQName(prefixMapping,
                                                                                e2.getNamespace(),
                                                                                e2.getName()),
                                              "CDATA", e2.getValue().toString());
                        } else if (e2.getType() == Event.COMMENT) continue;
                        else break;
                    }
                    r.backup(); // Since we read past last attribute
                    if (contentHandler != null) {
                        contentHandler.startElement(makeNameSpace(prefixMapping, namespace,
                                                                  localName),
                                                    makeName(prefixMapping, namespace, localName),
                                                    makeQName(prefixMapping, namespace, localName),
                                                    atts);
                    }
                    break;
                }
                case Event.END_ELEMENT: {
                    String namespace = e.getNamespace();
                    String localName = e.getName();
                    Log.debug("END_TAG(" + namespace + ", " + localName + ")");
                    if (contentHandler != null) {
                        contentHandler.endElement(
                                                  makeNameSpace(prefixMapping, namespace, localName),
                                                  makeName(prefixMapping, namespace, localName),
                                                  makeQName(prefixMapping, namespace, localName));
                    }
                    break;
                }
                case Event.CONTENT: {
                    char[] ch = e.getValue().toString().toCharArray();
                    Log.debug("TEXT(" + new String(ch, textParams[0], textParams[1]) + ")");
                    if (contentHandler != null) {
                        contentHandler.characters(ch, 0, ch.length);
                    }
                    break;
                }
                default:
                    break;
            }
        }
        if (contentHandler != null) {
            contentHandler.endDocument();
        }
    }


    protected String makeNameSpace(Map prefixes, String nameSpace, String localName) {
        return nameSpace;
    }


    protected String makeName(Map prefixes, String nameSpace, String localName) {
        return localName;
    }


    protected String makeQName(Map prefixes, String nameSpace, String localName) {
        String prefix = (String) prefixes.get(nameSpace);
        if (prefix != null && prefix.length() > 0) {
            return prefix + ":" + localName;
        } else return localName;
    }
}
