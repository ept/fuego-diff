/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.compat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A wrapper class to mimic SAX with XmlPull. This class wraps an XmlPullParser and implements the
 * SAX parser interface. The intent is to allow legacy SAX applications to use an XmlPull-based
 * parser without altering their own code.
 * <p>
 * This parser is namespace-aware and cannot be set otherwise. However, qualified names are still
 * reported by the callbacks, since some applications buggily expect the qname to actually contain a
 * non-empty string.
 */
public class SaxReader implements XMLReader {

    private static final String NS_URI = "http://xml.org/sax/features/namespaces";
    private static final String NSPRE_URI = "http://xml.org/sax/features/namespace-prefixes";

    private ContentHandler contentHandler;
    private DTDHandler dtdHandler;
    private EntityResolver entityResolver;
    private ErrorHandler errorHandler;

    private XmlPullParser parser;


    public SaxReader(XmlPullParser parser) {
        this.parser = parser;
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        } catch (XmlPullParserException ex) {
            /*
             * Every parser is required to support setting namespace processing on, so this
             * exception will never be thrown.
             */
            ex.printStackTrace();
        }
    }


    public ContentHandler getContentHandler() {
        return contentHandler;
    }


    public void setContentHandler(ContentHandler handler) {
        if (handler == null) { throw new NullPointerException(); }
        contentHandler = handler;
    }


    public DTDHandler getDTDHandler() {
        return dtdHandler;
    }


    public void setDTDHandler(DTDHandler handler) {
        if (handler == null) { throw new NullPointerException(); }
        dtdHandler = handler;
    }


    public EntityResolver getEntityResolver() {
        return entityResolver;
    }


    public void setEntityResolver(EntityResolver resolver) {
        if (resolver == null) { throw new NullPointerException(); }
        entityResolver = resolver;
    }


    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }


    public void setErrorHandler(ErrorHandler handler) {
        if (handler == null) { throw new NullPointerException(); }
        errorHandler = handler;
    }


    public boolean getFeature(String name) throws SAXNotRecognizedException {
        if (NS_URI.equals(name)) {
            return true;
        } else if (NSPRE_URI.equals(name)) {
            return false;
        } else {
            throw new SAXNotRecognizedException("Feature " + name + " not recognized");
        }
    }


    public void setFeature(String name, boolean value) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        if (NS_URI.equals(name)) {
            if (!value) { throw new SAXNotSupportedException("Feature " + name +
                                                             " may not be set to false"); }
        } else if (NSPRE_URI.equals(name)) {
            if (value) { throw new SAXNotSupportedException("Feature " + name +
                                                            " may not be set to true"); }
        } else {
            throw new SAXNotRecognizedException("Feature " + name + " not recognized");
        }
    }


    public Object getProperty(String name) throws SAXNotRecognizedException {
        return null;
    }


    public void setProperty(String name, Object value) throws SAXNotRecognizedException {
    }


    public void parse(String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }


    public void parse(InputSource input) throws IOException, SAXException {
        InputStream stream = input.getByteStream();
        if (stream == null) { throw new SAXException("Must have an InputStream to parse"); }
        try {
            Stack nsStack = new Stack();
            List nsCurrent = new ArrayList();
            parser.setInput(stream, "ISO-8859-1");
            int[] textParams = new int[2];
            for (int eventType = parser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = parser.nextToken()) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT: {
                        if (contentHandler != null) {
                            contentHandler.startDocument();
                        }
                        break;
                    }
                    case XmlPullParser.START_TAG: {
                        int nsStart = parser.getNamespaceCount(parser.getDepth() - 1);
                        int nsEnd = parser.getNamespaceCount(parser.getDepth());
                        for (int i = nsStart; i < nsEnd; i++) {
                            String prefix = parser.getNamespacePrefix(i);
                            String uri = parser.getNamespaceUri(i);
                            if (contentHandler != null) {
                                contentHandler.startPrefixMapping(prefix, uri);
                            }
                            nsCurrent.add(prefix);
                        }
                        String namespace = parser.getNamespace();
                        String localName = parser.getName();
                        AttributesImpl atts = new AttributesImpl();
                        int attLen = parser.getAttributeCount();
                        for (int i = 0; i < attLen; i++) {
                            String aNamespace = parser.getAttributeNamespace(i);
                            String aName = parser.getAttributeName(i);
                            String aValue = parser.getAttributeValue(i);
                            atts.addAttribute(aNamespace, aName, "", parser.getAttributeType(i),
                                              aValue);
                        }
                        if (contentHandler != null) {
                            contentHandler.startElement(namespace, localName, "", atts);
                        }
                        nsStack.push(nsCurrent);
                        nsCurrent = new ArrayList();
                        break;
                    }
                    case XmlPullParser.END_TAG: {
                        String namespace = parser.getNamespace();
                        String localName = parser.getName();
                        if (contentHandler != null) {
                            contentHandler.endElement(namespace, localName, "");
                        }
                        List current = (List) nsStack.pop();
                        for (Iterator i = current.iterator(); i.hasNext();) {
                            String prefix = (String) i.next();
                            if (contentHandler != null) {
                                contentHandler.endPrefixMapping(prefix);
                            }
                        }
                        break;
                    }
                    case XmlPullParser.TEXT:
                    case XmlPullParser.CDSECT: {
                        char[] ch = parser.getTextCharacters(textParams);
                        if (contentHandler != null) {
                            contentHandler.characters(ch, textParams[0], textParams[1]);
                        }
                        break;
                    }
                    case XmlPullParser.PROCESSING_INSTRUCTION: {
                        String target = parser.getText();
                        if (contentHandler != null) {
                            String data = null;
                            int i = target.indexOf(' ');
                            if (i >= 0) {
                                data = target.substring(i + 1);
                                target = target.substring(0, i);
                            }
                            contentHandler.processingInstruction(target, data);
                        }
                        break;
                    }
                    case XmlPullParser.ENTITY_REF: {
                        String name = parser.getName();
                        if (contentHandler != null) {
                            if (name.charAt(0) == '#') {
                                char[] ch = parser.getTextCharacters(textParams);
                                contentHandler.characters(ch, textParams[0], textParams[1]);
                            } else {
                                contentHandler.skippedEntity(name);
                            }
                        }
                        break;
                    }
                    case XmlPullParser.COMMENT:
                        break;
                    default:
                        break;
                }
            }
        } catch (XmlPullParserException ex) {
            throw new SAXException(ex);
        }
        if (contentHandler != null) {
            contentHandler.endDocument();
        }
    }

}

// arch-tag: 5be216c6-53c5-49ba-be87-5880b3daa7f6
