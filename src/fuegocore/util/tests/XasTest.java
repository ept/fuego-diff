/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.tests;

import java.util.List;
import java.util.Set;
import java.util.Iterator;
import java.util.Enumeration;
import java.io.StringWriter;
import java.io.StringReader;
import org.w3c.dom.Node;

import junit.framework.TestCase;

import fuegocore.util.xas.*;

/**
 * Test the XAS implementation. This class tests that the XAS event sequences function properly and
 * that the various codecs do something sensible.
 */
public class XasTest extends TestCase {

    private List sequences;


    private boolean hasTypedContent(EventSequence es) {
        for (Enumeration e = es.events(); e.hasMoreElements();) {
            Event ev = (Event) e.nextElement();
            if (ev.getType() == Event.TYPED_CONTENT) { return true; }
        }
        return false;
    }


    @Override
    protected void setUp() {
        sequences = XasData.getSequences();
    }


    public XasTest(String name) {
        super(name);
    }


    public void testEncDec() throws Exception {
        assertTrue("No XML sequences defined", sequences.size() > 0);
        Set types = CodecIndustry.availableTypes();
        assertTrue("No codec types registered", types.size() > 0);
        System.out.println("Serialize-parse cycle");
        boolean[] printed = new boolean[sequences.size()];
        for (Iterator i = types.iterator(); i.hasNext();) {
            String type = (String) i.next();
            System.out.println(type);
            CodecFactory factory = CodecIndustry.getFactory(type);
            assertNotNull("Returned factory null for type " + type, factory);
            Object token = new Object();
            int index = 0;
            for (Iterator j = sequences.iterator(); j.hasNext();) {
                EventSequence seq = (EventSequence) j.next();
                // System.out.println(seq);
                System.out.println(index);
                if (!printed[index]) {
                    System.out.println(seq.toString());
                    printed[index] = true;
                }
                index++;
                TypedXmlSerializer enc = factory.getNewEncoder(token);
                StringWriter target = new StringWriter();
                enc.setOutput(target);
                ContentEncoder tEnc = (ContentEncoder) enc.getProperty(XasUtil.PROPERTY_CONTENT_CODEC);
                enc.setProperty(XasUtil.PROPERTY_CONTENT_CODEC,
                                new XasData.IntListEncoder(new XasData.CompoundEncoder(tEnc)));
                XasUtil.outputSequence(seq, enc);
                // System.out.println(Util.toPrintable(target.toString()));
                TypedXmlParser dec = factory.getNewDecoder(token);
                dec.setInput(new StringReader(target.toString()));
                ContentDecoder tDec = (ContentDecoder) dec.getProperty(XasUtil.PROPERTY_CONTENT_CODEC);
                EventSequence res = new TypedEventStream(
                                                         new EventStream(dec),
                                                         new XasData.IntListDecoder(
                                                                                    new XasData.CompoundDecoder(
                                                                                                                tDec)));
                if (factory instanceof XmlCodecFactory) {
                    res = new CoalescedEventSequence(res);
                }
                // System.out.println(res);
                assertEquals("Enc-dec sequence failed for type " + type + "\n" + seq + "\n\n" +
                             res + "\n\n", seq, res);
            }
        }
    }


    public void testDomConversion() throws Exception {
        assertTrue("No XML sequences defined", sequences.size() > 0);
        System.out.println("DOM conversion");
        int index = 0;
        for (Iterator i = sequences.iterator(); i.hasNext();) {
            EventSequence seq = (EventSequence) i.next();
            if (!hasTypedContent(seq)) {
                System.out.println(index++);
                Node n = XasExtUtil.sequenceToDom(XasExtUtil.newDocument(), seq);
                EventSequence res = XasExtUtil.domToSequence(n);
                assertEquals("DOM conversion failed\n" + seq + "\n\n" + res + "\n\n", seq, res);
            }
        }
    }

}
