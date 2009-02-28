/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import org.xmlpull.v1.XmlPullParserException;

import org.kxml2.io.KXmlParser;

/**
 * Default parser implementation for XML. This is an implementation of the
 * <code>XmlPullParser</code> interface that recognizes the {@link XasUtil#PROPERTY_CONTENT_CODEC}
 * property. By default it contains the {@link XmlSchemaContentDecoder} for typed content.
 * <p>
 * Note that normally the parser does not care about typed content, but rather just constructs
 * {@link Event#CONTENT} events. However, the parser is a logical place to put the typed content
 * decoder, and this placement is also symmetric with respect to how typed content encoders are
 * handled.
 */
public class DefaultXmlParser extends KXmlParser implements TypedXmlParser {

    private ContentDecoder decoder = new XmlSchemaContentDecoder();


    @Override
    public void setProperty(String name, Object value) throws XmlPullParserException {
        if (XasUtil.PROPERTY_CONTENT_CODEC.equals(name)) {
            if (value instanceof ContentDecoder) {
                decoder = (ContentDecoder) value;
            } else {
                throw new IllegalArgumentException("Not a ContentDecoder: " + value);
            }
        } else {
            super.setProperty(name, value);
        }
    }


    @Override
    public Object getProperty(String name) {
        if (XasUtil.PROPERTY_CONTENT_CODEC.equals(name)) {
            return decoder;
        } else {
            return super.getProperty(name);
        }
    }


    public Object getObject() {
        return getText();
    }

}
