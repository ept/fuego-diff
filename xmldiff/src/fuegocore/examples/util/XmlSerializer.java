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

package fuegocore.examples.util;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;

import fuegocore.util.xas.EventSequence;
import fuegocore.util.xas.EventStream;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.xas.CodecFactory;
import fuegocore.util.xas.CodecIndustry;
import fuegocore.util.xas.XmlCodecFactory;
import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.TypedEventStream;
import fuegocore.util.xas.TypedXmlParser;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.DataEventSequence;

import fuegocore.message.encoding.CachePair;
import fuegocore.message.encoding.DoaMachine;
import fuegocore.message.encoding.EoaMachine;
import fuegocore.message.encoding.XebuConstants;
import fuegocore.message.encoding.coa.ReaderCoaMachine;

/**
 * Convert an XML document to an alternate representation.  This
 * program reads XML files given as arguments and outputs them in a
 * specified XML Infoset encoding, each to a file whose name is
 * derived from the input file.
 *
 * <p>The first argument to the program is the name of the format and
 * it is mandatory.  The rest of the arguments are interpreted as XML
 * file names, and they need to end with the string
 * <code>".xml"</code> (file names not matching this will be silently
 * ignored).  The output file name will have the <code>"xml"</code>
 * suffix replaced with the name of the format.  The program also
 * outputs a file with the suffix <code>"es"</code>, which contains
 * the XML document's representation as an {@link EventSequence}.
 *
 * <p>The recognized format names and their corresponding MIME types
 * are: <ul> <li><code>xmlb</code>: <code>text/xml</code></li>
 * <li><code>none</code>: <code>application/x-ebu+none</code></li>
 * <li><code>item</code>: <code>application/x-ebu+item</code></li>
 * <li><code>data</code>: <code>application/x-ebu+data</code></li>
 * <li><code>elem</code>: <code>application/x-ebu+elem</code></li>
 * <li><code>elit</code>: <code>application/x-ebu+elit</code></li>
 * <li><code>elid</code>: <code>application/x-ebu+elid</code></li>
 * </ul> The exact semantics of the various Xebu types are given in
 * the {@link fuegocore.message.encoding.XebuCodecFactory}
 * documentation.  In addition, any of these names may be suffixed
 * with <code>z</code> to indicate that gzip compression is to be
 * applied to the result.
 *
 * <p>This program does not support typed content apart from the data
 * types recognized by the default ones of each codec.
 */
public class XmlSerializer {

    private static Map types = new HashMap();

    static {
	types.put("xmlb", "text/xml");
	types.put("none", "application/x-ebu+none");
	types.put("item", "application/x-ebu+item");
	types.put("data", "application/x-ebu+data");
	types.put("elem", "application/x-ebu+elem");
	types.put("elit", "application/x-ebu+elit");
	types.put("elid", "application/x-ebu+elid");
    }

    public static void main (String[] args) {
	try {
	    if (args.length < 2) {
		System.err.println("Need to specify type of output "
				   + "and at least one file");
		System.exit(1);
	    }
	    boolean gzip = false;
	    boolean schema = false;
	    boolean table = false;
	    int beginIndex = 1;
	    ReaderCoaMachine rcm = null;
	    String rawType = args[0];
	    if (rawType.endsWith("z")) {
		rawType = rawType.substring(0, rawType.length() - 1);
		gzip = true;
	    }
	    if (rawType.length() > 4) {
		if (rawType.endsWith("s")) {
		    schema = true;
		    table = true;
		} else if (rawType.endsWith("t")) {
		    table = true;
		}
		rawType = rawType.substring(0, 4);
	    }
	    if (table) {
		beginIndex = 2;
		rcm = new ReaderCoaMachine(new FileReader(args[1]));
	    }
	    String type = (String) types.get(rawType);
	    if (type == null) {
		System.err.println("Not a recognized type: " + args[0]);
		System.exit(2);
	    }
	    CodecFactory factory = CodecIndustry.getFactory(type);
	    CodecFactory xmlFactory = XmlCodecFactory.getInstance();
	    TypedXmlParser parser = xmlFactory.getNewDecoder(null);
	    Object token = new Object();
	    for (int i = beginIndex; i < args.length; i++) {
		if (args[i].endsWith(".xml")) {
		    CachePair pair = null;
		    EoaMachine eoa = null;
		    if (rcm != null) {
			pair = rcm.createNewPair();
			if (schema) {
			    eoa = rcm.createNewEoa();
			}
		    }
		    String esName = args[i].replaceAll("xml$", "es");
		    String outName = args[i].replaceAll("xml$", args[0]);
		    FileReader in = new FileReader(args[i]);
		    parser.setInput(in);
		    ContentDecoder dec = (ContentDecoder)
			parser.getProperty(XasUtil.PROPERTY_CONTENT_CODEC);
		    EventSequence es =
			new TypedEventStream(new EventStream(parser), dec);
// 		    if (rcm != null) {
// 			es = new DataEventSequence(es, true, false);
// 		    }
		    FileWriter esOut = new FileWriter(esName);
		    esOut.write(es.toString());
		    esOut.close();
		    TypedXmlSerializer ser = factory.getNewEncoder(token);
		    OutputStream outStream = new FileOutputStream(outName);
		    if (gzip) {
			outStream = new GZIPOutputStream(outStream);
		    }
		    Writer out = new OutputStreamWriter(outStream);
		    ser.setOutput(out);
		    if (pair != null) {
			ser.setProperty(XebuConstants.PROPERTY_INITIAL_CACHES,
					pair.getOutCache());
		    }
		    if (eoa != null) {
			ser.setProperty(XebuConstants.PROPERTY_COA_MACHINE,
					eoa);
		    }
		    XasUtil.outputSequence(es, ser);
		    out.close();
		}
	    }
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

}
