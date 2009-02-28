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

package fuegocore.message.exper;

import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import fuegocore.util.ByteBuffer;
import fuegocore.util.TimeMeasurer;
import fuegocore.util.Util;
import fuegocore.util.ExtUtil;
import fuegocore.util.xas.CodecIndustry;
import fuegocore.util.xas.CodecFactory;
import fuegocore.util.xas.TypedXmlParser;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.EventSequence;
import fuegocore.util.xas.EventSerializer;
import fuegocore.util.xas.DefaultXmlParser;
import fuegocore.util.xas.DefaultXmlSerializer;
import fuegocore.util.xas.EventStream;
import fuegocore.util.xas.TypedEventStream;
import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.XasUtil;

/**
 * Runs the encoding experiments for Xebu against standard formats.
 * This class contains the encoding experiments detailed in the Design
 * of Experimentation document of the Fuego Core project.  The main
 * method reads the names of the XML files to be tested on the command
 * line.  These XML files are encoded into an internal representation
 * to remove any parsing overhead from the measurements.  The encoded
 * documents are saved and run through the decoding process, also
 * verifying that the decoded document is identical to the original
 * one.  The total sizes of the documents are also output.
 *
 * <p>The main experiments consist of trying the various binary
 * formats.  The baseline here are XML and gzipped XML (produced with
 * GzipStreams).  The binary formats are the ones detailed in the
 * {@link fuegocore.message.encoding.XebuCodecFactory} class
 * documentation.
 *
 * <p>The first command line argument specifies the encoding type to
 * use and is mandatory.  The available values are <code>xml</code>
 * (XML), <code>xmlz</code> (Gzipped XML), and the ones for Xebu are
 * the same as those given to {@link
 * fuegocore.examples.util.XmlSerializer}.  All the other arguments
 * are names of files containing XML documents.
 */
public class Encoding {

    /*
     * Private constructor to prevent instantiation.
     */
    private Encoding () {
    }

    private static String[] acceptedTypes = { "xml", "xmlz", "none", "item",
					      "data", "elem", "elit", "elid" };
    private static final int BUSY_LOOP = 20;

    private static EventSequence xmlToEventSequence (byte[] xml) {
	EventSerializer ser = new EventSerializer();
	try {
	    TypedXmlParser parser = new DefaultXmlParser();
	    parser.setInput(new ByteArrayInputStream(xml), "ISO-8859-1");
	    EventStream es = new EventStream(parser);
	    TypedEventStream tes = new TypedEventStream
		(es, (ContentDecoder)
		 parser.getProperty(XasUtil.PROPERTY_CONTENT_CODEC));
	    XasUtil.outputSequence(tes, ser);
	} catch (Exception e) {
	    System.err.println("Invalid XML document:");
	    System.err.println(new String(xml));
	    System.err.println("Caused by:");
	    e.printStackTrace(System.err);
	    System.exit(1);
	}
	return ser.getCurrentSequence();
    }

    private static TypedXmlSerializer getEncoder (int index, OutputStream os,
						  Object key)
	throws IOException {
	TypedXmlSerializer result = null;
	try {
	    switch (index) {
	    case 0:
	    case 1:
		result = new DefaultXmlSerializer();
		break;
	    case 2:
	    case 3:
	    case 4:
	    case 5:
	    case 6:
	    case 7: {
		CodecFactory factory =
		    CodecIndustry.getFactory("application/x-ebu+"
					     + acceptedTypes[index]);
		result = factory.getNewEncoder(key);
		break;
	    }
	    default:
		return null;
	    }
	} catch (Exception ex) {
	    return null;
	}
	result.setOutput(os, "ISO-8859-1");
	return result;
    }

    private static TypedXmlParser getDecoder (int index, Object key) {
	try {
	    switch (index) {
	    case 0:
	    case 1:
		return new DefaultXmlParser();
	    case 2:
	    case 3:
	    case 4:
	    case 5:
	    case 6:
	    case 7: {
		CodecFactory factory =
		    CodecIndustry.getFactory("application/x-ebu+"
					     + acceptedTypes[index]);
		return factory.getNewDecoder(key);
	    }
	    default:
		return null;
	    }
	} catch (Exception ex) {
	    return null;
	}
    }

    private static void doEncoding (String type, EventSequence[] docs,
				    int maxLength)
	throws IOException {
	TimeMeasurer timer = new TimeMeasurer();
	timer.init();
	CodecTimer cdTimer = new CodecTimer();
	String encString = "Serialization for type " + type;
	String decString = "Parsing for type " + type;
	int index;
	for (index = 0; index < acceptedTypes.length; index++) {
	    if (acceptedTypes[index].equals(type)) {
		break;
	    }
	}
	if (index < acceptedTypes.length) {
	    byte[][] strings = new byte[docs.length][];
	    for (int i = 0; i < 2 * BUSY_LOOP; i++) {
		for (int j = 0; j < docs.length; j++) {
		    strings[j] = cdTimer.encode(index, docs[j]);
		}
		cdTimer.reset();
	    }
	    ExtUtil.runGc();
	    long beginMemory = ExtUtil.usedMemory();
	    for (int i = 0; i < docs.length; i++) {
		strings[i] = cdTimer.encode(index, docs[i], timer, encString,
					    maxLength);
	    }
	    long memory = ExtUtil.usedMemory() - beginMemory;
	    System.out.println(encString + ": " + timer.getTime(encString)
			       + " (" + timer.getNumber(encString) + ")");
	    int size = 0;
	    for (int i = 0; i < docs.length; i++) {
		size += strings[i].length;
	    }
	    System.out.println("Size of type " + type + " documents: " + size);
	    System.out.println("Memory spent serializing type " + type
			       + " documents: " + memory);
	    EventSequence[] results = new EventSequence[docs.length];
	    for (int i = 0; i < 2 * BUSY_LOOP; i++) {
		for (int j = 0; j < docs.length; j++) {
		    results[j] = cdTimer.decode(index, strings[j]);
		}
		cdTimer.reset();
	    }
	    ExtUtil.runGc();
	    beginMemory = ExtUtil.usedMemory();
	    for (int i = 0; i < docs.length; i++) {
		results[i] = cdTimer.decode(index, strings[i], timer,
					    decString, maxLength);
	    }
	    memory = ExtUtil.usedMemory() - beginMemory;
	    System.out.println(decString + ": " + timer.getTime(decString)
			       + " (" + timer.getNumber(decString) + ")");
	    System.out.println("Memory spent parsing type " + type
			       + " documents: " + memory);
	    for (int i = 0; i < docs.length; i++) {
		if (!docs[i].equals(results[i])) {
		    System.err.println("Failure with document " + i);
		    System.err.println(docs[i]);
		    System.err.println(Util.toPrintable(strings[i]));
		    System.err.println(results[i]);
		}
	    }
	}
    }

    public static void main (String args[]) {
	try {
	    if (args.length < 2) {
		System.err.println("Usage: Encoding <type> <file...>");
		System.exit(1);
	    }
	    String type = args[0];
	    int length = 0;
	    int initial = 1;
	    int nondocs = 1;
	    String baseType = null;
	    byte[][] contents = new byte[args.length - nondocs][];
	    for (int i = nondocs; i < args.length; i++) {
		InputStream file = new BufferedInputStream(new FileInputStream(args[i]));
		ByteBuffer buffer = new ByteBuffer();
		byte[] tempBuffer = new byte[4096];
		int bytesRead;
		while ((bytesRead = file.read(tempBuffer, 0, tempBuffer.length))
		       > 0) {
		    buffer.append(tempBuffer, 0, bytesRead);
		}
		contents[i - nondocs] = buffer.getBytes();
		if (contents[i - nondocs].length > length) {
		    length = contents[i - nondocs].length;
		}
	    }
	    EventSequence[] parseds = new EventSequence[contents.length];
	    for (int i = 0; i < parseds.length; i++) {
		parseds[i] = xmlToEventSequence(contents[i]);
	    }
	    doEncoding(type, parseds, 2 * length);
	} catch (Exception e) {
	    System.err.println("Operation failed:");
	    e.printStackTrace();
	}
    }

    private static class CodecTimer {

	private Object key;

	public CodecTimer () {
	    key = new Object();
	}

	public void reset () {
	    key = new Object();
	}

	public byte[] encode (int index, EventSequence source)
	    throws IOException {
	    return encode(index, source, null, "", 32);
	}

	public byte[] encode (int index, EventSequence source,
			      TimeMeasurer timer, String timerKey,
			      int maxLength)
	    throws IOException {
	    ByteArrayOutputStream os = new ByteArrayOutputStream(maxLength);
	    // Gratuitous initialization
	    long start = 0;
	    Object token = null;
	    TypedXmlSerializer encoder;
	    GZIPOutputStream gs = null;
	    if (index == 1) {
		gs = new GZIPOutputStream(os);
		encoder = getEncoder(index, gs, key);
	    } else {
		encoder = getEncoder(index, os, key);
	    }
	    if (timer != null) {
		token = timer.beginWall();
	    }
	    XasUtil.outputSequence(source, encoder);
	    if (gs != null) {
		gs.close();
	    }
	    if (timer != null) {
		timer.end(token, timerKey);
	    }
	    return os.toByteArray();
	}

	public EventSequence decode (int index, byte[] source)
	    throws IOException {
	    return decode(index, source, null, "", 2048);
	}

	public EventSequence decode (int index, byte[] source,
				     TimeMeasurer timer, String timerKey,
				     int bufferSize)
	    throws IOException {
	    try {
		EventSerializer result = new EventSerializer();
		TypedXmlParser decoder = getDecoder(index, key);
		InputStream in;
		if (index != 1) {
		    in = new ByteArrayInputStream(source);
		} else {
		    in = new GZIPInputStream(new ByteArrayInputStream(source));
		}
		in = new BufferedInputStream(in, bufferSize);
		decoder.setInput(in, "ISO-8859-1");
		// Gratuitous initialization
		long start = 0;
		Object token = null;
		if (timer != null) {
		    token = timer.beginWall();
		}
		EventStream es = new EventStream(decoder);
		TypedEventStream tes = new TypedEventStream
		    (es, (ContentDecoder)
		     decoder.getProperty(XasUtil.PROPERTY_CONTENT_CODEC));
		XasUtil.outputSequence(tes, result);
		if (timer != null) {
		    timer.end(token, timerKey);
		}
		return result.getCurrentSequence();
	    } catch (Exception ex) {
		ex.printStackTrace();
		throw new IOException(ex.getMessage());
	    }
	}

    }

}
