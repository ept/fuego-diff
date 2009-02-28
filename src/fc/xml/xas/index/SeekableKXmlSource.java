/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.index;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import fc.util.BaInputStream;
import fc.util.RaInputStream;
import fc.util.Utf8Reader;
import fc.xml.xas.Item;
import fc.xml.xas.StartTag;
import fc.xml.xas.XmlPullSource;

public class SeekableKXmlSource implements SeekableParserSource {

    private XmlPullSource source;
    private KXmlParser parser;
    private RaInputStream ra;


    public SeekableKXmlSource(RaInputStream rin) throws FileNotFoundException {
        parser = new KXmlParser();
        ra = rin;
        Reader reader = new Utf8Reader(ra);
        source = new XmlPullSource(parser, ra, reader);
    }


    public SeekableKXmlSource(String file) throws FileNotFoundException {
        parser = new KXmlParser();
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        ra = new RaInputStream(raf);
        Reader reader = new Utf8Reader(ra);
        source = new XmlPullSource(parser, ra, reader);
    }


    public SeekableKXmlSource(byte[] buffer, int offset, int length) {
        parser = new KXmlParser();
        BaInputStream ba = new BaInputStream(buffer, offset, length);
        Reader reader = new Utf8Reader(ba);
        source = new XmlPullSource(parser, ba, reader);
    }


    public Item next() throws IOException {
        return source.next();
    }


    public InputStream getInputStream() {
        return source.getInputStream();
    }


    public String getEncoding() {
        return source.getEncoding();
    }


    public StartTag getContext() {
        return source.getContext();
    }


    public int getCurrentPosition() {
        return parser.getStreamPos();
    }


    public int getPreviousPosition() {
        return parser.getPrevPos();
    }


    public void setPosition(int pos, StartTag context) throws IOException {
        try {
            parser.reposition(pos, context);
        } catch (XmlPullParserException ex) {
            throw (IOException) new IOException(ex.getMessage()).initCause(ex);
        }
        if (pos < 0) {
            source.reset();
        } else {
            source.setContext(context);
        }
    }


    public void close() throws IOException {
        ra.close();
    }

}

// arch-tag: 66e3799e-aa98-4ca2-ab97-1c3619e5f473
