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

package fcme.tests;

import java.util.List;
import java.util.Iterator;
import java.io.StringWriter;
import java.io.StringReader;

import junit.framework.TestCase;

import fuegocore.util.xas.CodecFactory;
import fuegocore.util.xas.EventSequence;
import fuegocore.util.xas.EventStream;
import fuegocore.util.xas.TypedEventStream;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.TypedXmlParser;
import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.tests.XasData;
import fuegocore.util.Util;

public class XasTest extends TestCase {

    private List sequences;

    protected void setUp () {
	fcme.message.encoding.XebuCodecFactory.init();
	sequences = XasData.getSequences();
    }

    public XasTest (String name) {
	super(name);
    }

    public void testEndDec () throws Exception {
	assertTrue("No XML sequences defined", sequences.size() > 0);
	String[] types = { "application/x-ebu+none", "application/x-ebu+item",
			   "application/x-ebu+data" };
	String[] tests = { "ME->SE", "SE->ME", "ME->ME" };
	CodecFactory serFactory = null;
	CodecFactory parFactory = null;
	for (int i = 0; i < 3; i++) {
	    String test = tests[i];
	    System.out.println("Serialize-parse cycle for " + test);
	    for (int j = 0; j < types.length; j++) {
		String type = types[j];
		System.out.println(type);
		if (i == 0 || i == 2) {
		    serFactory = fcme.util.xas.CodecIndustry.getFactory(type);
		} else {
		    serFactory =
			fuegocore.util.xas.CodecIndustry.getFactory(type);
		}
		assertNotNull("Returned serializer factory null for type "
			      + type + " in test " + test, serFactory);
		if (i == 1 || i == 2) {
		    parFactory = fcme.util.xas.CodecIndustry.getFactory(type);
		} else {
		    parFactory =
			fuegocore.util.xas.CodecIndustry.getFactory(type);
		}
		assertNotNull("Returned parser factory null for type "
			      + type + " in test " + test, parFactory);
		Object token = new Object();
		for (Iterator k = sequences.iterator(); k.hasNext(); ) {
		    EventSequence seq = (EventSequence) k.next();
		    //System.out.println(seq);
		    TypedXmlSerializer enc = serFactory.getNewEncoder(token);
		    StringWriter target = new StringWriter();
		    enc.setOutput(target);
		    ContentEncoder tEnc = (ContentEncoder)
			enc.getProperty(XasUtil.PROPERTY_CONTENT_CODEC);
		    enc.setProperty(XasUtil.PROPERTY_CONTENT_CODEC,
				    new XasData.IntListEncoder
				    (new XasData.CompoundEncoder(tEnc)));
		    XasUtil.outputSequence(seq, enc);
		    //System.out.println(Util.toPrintable(target.toString()));
		    TypedXmlParser dec = parFactory.getNewDecoder(token);
		    dec.setInput(new StringReader(target.toString()));
		    ContentDecoder tDec = (ContentDecoder)
			dec.getProperty(XasUtil.PROPERTY_CONTENT_CODEC);
		    EventSequence res = new TypedEventStream
			(new EventStream(dec),
			 new XasData.IntListDecoder
			 (new XasData.CompoundDecoder(tDec)));
		    //System.out.println(res);
		    assertEquals("Enc-dec sequence failed for type " + type
				 + "\n" + seq + "\n\n" + res + "\n\n",
				 seq, res);
		}
	    }
	}
    }

}
