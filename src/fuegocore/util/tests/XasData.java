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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Arrays;
import java.io.IOException;

import fuegocore.util.Util;
import fuegocore.util.xas.Event;
import fuegocore.util.xas.EventList;
import fuegocore.util.xas.EventSequence;
import fuegocore.util.xas.EventSerializer;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.XmlWriter;
import fuegocore.util.xas.XmlReader;
import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.ChainedContentDecoder;
import fuegocore.util.xas.XasUtil;

/**
 * Common XML documents for testing. This class creates a group of {@link EventSequence} objects
 * representing different XML documents. These can be used to test various XML handling classes.
 */
public class XasData {

    /**
     * The namespace used in the documents of the data.
     */
    public static final String FOO_NS = "http://www.hiit.fi/fuego/fc/test";

    /**
     * The local type name for the {@link XasData.Compound} type.
     */
    public static final String CP_TYPE = "compound";

    /**
     * The local type name for the {@link List} type.
     */
    public static final String LIST_TYPE = "list";


    /*
     * Private constructor to prevent instantiation.
     */
    private XasData() {
    }


    /**
     * Get all XML documents. This method returns the XML documents that this class has created.
     * @return XML documents for testing as {@link EventSequence} objects
     */
    public static List getSequences() {
        try {
            List sequences = new ArrayList();
            EventSerializer z = new EventSerializer();
            XmlWriter zamunda = new XmlWriter(z);
            zamunda.addEvent(Event.createStartDocument());
            zamunda.addEvent(Event.createNamespacePrefix(FOO_NS, "foo"));
            zamunda.addEvent(Event.createStartElement(FOO_NS, "zamunda"));
            zamunda.simpleElement(FOO_NS, "ruler", "Jaffe Joffer", Event.createAttribute(FOO_NS,
                                                                                         "title",
                                                                                         "King"));
            zamunda.simpleElement(FOO_NS, "ruler", "Jaffe Joffer", Event.createAttribute(FOO_NS,
                                                                                         "title",
                                                                                         "King"));
            zamunda.simpleElement(FOO_NS, "futureruler", "Akeem", Event.createAttribute(FOO_NS,
                                                                                        "title",
                                                                                        "Prince"));
            zamunda.simpleElement(FOO_NS, "futureruler", "Akeem", Event.createAttribute(FOO_NS,
                                                                                        "title",
                                                                                        "Prince"));
            zamunda.addEvent(Event.createEndElement(FOO_NS, "zamunda"));
            zamunda.addEvent(Event.createEndDocument());
            sequences.add(z.getCurrentSequence());
            EventSerializer l = new EventSerializer();
            XmlWriter list = new XmlWriter(l);
            list.simpleElement(FOO_NS, "ruler", "Jaffe Joffer", Event.createAttribute(FOO_NS,
                                                                                      "title",
                                                                                      "King"));
            EventSequence content = l.getCurrentSequence();
            EventList deep = new EventList();
            deep.add(Event.createStartDocument());
            deep.add(Event.createNamespacePrefix(FOO_NS, "bar"));
            for (int i = 0; i < 15; i++) {
                EventSerializer c = new EventSerializer();
                new XmlWriter(c).complexElement(FOO_NS, "content", content,
                                                Event.createAttribute(FOO_NS, "height",
                                                                      Integer.toString(i)));
                content = c.getCurrentSequence();
            }
            deep.addAll(content);
            deep.add(Event.createEndDocument());
            sequences.add(deep);
            EventSerializer t = new EventSerializer();
            XmlWriter typed = new XmlWriter(t);
            typed.addEvent(Event.createStartDocument());
            typed.addEvent(Event.createNamespacePrefix(FOO_NS, "baz"));
            typed.addEvent(Event.createNamespacePrefix(XasUtil.XSI_NAMESPACE, "xsi"));
            typed.addEvent(Event.createNamespacePrefix(XasUtil.XSD_NAMESPACE, "xsd"));
            typed.typedElement(FOO_NS, "integer", XasUtil.XSD_NAMESPACE, "int", new Integer(500));
            typed.addEvent(Event.createEndDocument());
            sequences.add(t.getCurrentSequence());
            EventSerializer cp = new EventSerializer();
            XmlWriter compound = new XmlWriter(cp);
            compound.addEvent(Event.createStartDocument());
            compound.addEvent(Event.createNamespacePrefix(FOO_NS, "quux"));
            compound.addEvent(Event.createNamespacePrefix(XasUtil.XSI_NAMESPACE, "xsi"));
            compound.addEvent(Event.createNamespacePrefix(XasUtil.XSD_NAMESPACE, "xsd"));
            byte[] bs = new byte[2];
            bs[0] = 15;
            bs[1] = -15;
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            compound.typedElement(FOO_NS, "compound", FOO_NS, CP_TYPE, new Compound(500, (byte) 50,
                                                                                    bs, c));
            compound.addEvent(Event.createEndDocument());
            sequences.add(cp.getCurrentSequence());
            EventSerializer il = new EventSerializer();
            XmlWriter intList = new XmlWriter(il);
            intList.addEvent(Event.createStartDocument());
            intList.addEvent(Event.createNamespacePrefix(FOO_NS, "xyzzy"));
            intList.addEvent(Event.createNamespacePrefix(XasUtil.XSI_NAMESPACE, "xsi"));
            intList.addEvent(Event.createNamespacePrefix(XasUtil.XSD_NAMESPACE, "xsd"));
            intList.addEvent(Event.createStartElement(FOO_NS, "lists"));
            List l1 = new ArrayList();
            l1.add(new Integer(100));
            List l2 = new ArrayList();
            for (int i = 0; i < 10; i++) {
                l2.add(new Integer(1000 * i * i));
            }
            intList.typedElement(FOO_NS, "first", FOO_NS, LIST_TYPE, l1);
            intList.typedElement(FOO_NS, "second", FOO_NS, LIST_TYPE, l2);
            intList.addEvent(Event.createEndElement(FOO_NS, "lists"));
            intList.addEvent(Event.createEndDocument());
            sequences.add(il.getCurrentSequence());
            EventSerializer misc = new EventSerializer();
            misc.startDocument(null, null);
            misc.processingInstruction("reader ignore");
            misc.processingInstruction("reader ignore");
            misc.setPrefix("kuf", FOO_NS);
            misc.startTag(FOO_NS, "container");
            misc.comment("This is a weird document");
            misc.comment("This is a weird document");
            misc.entityRef("author");
            misc.entityRef("author");
            misc.endTag(FOO_NS, "container");
            misc.endDocument();
            sequences.add(misc.getCurrentSequence());
            EventSerializer ent = new EventSerializer();
            ent.startDocument(null, null);
            ent.setPrefix("fis", FOO_NS);
            ent.startTag(FOO_NS, "prog");
            ent.text("if (a < b && b > c) { /* do nothing */ }");
            ent.endTag(FOO_NS, "prog");
            ent.endDocument();
            sequences.add(ent.getCurrentSequence());
            return sequences;
        } catch (IOException ex) {
            /*
             * Since we are using EventSerializers, this exception cannot happen.
             */
            return null;
        }
    }

    /**
     * A compound object with a few fields of different types. This is basically a record with some
     * differently-typed fields for testing typed content.
     */
    public static class Compound {

        private int i;
        private byte b;
        private byte[] bs;
        private Calendar c;


        public Compound(int i, byte b, byte[] bs, Calendar c) {
            this.i = i;
            this.b = b;
            this.bs = bs;
            this.c = c;
        }


        public int getI() {
            return i;
        }


        public byte getB() {
            return b;
        }


        public byte[] getBs() {
            return bs;
        }


        public Calendar getC() {
            return c;
        }


        public boolean equals(Object o) {
            if (!(o instanceof Compound)) { return false; }
            Compound cp = (Compound) o;
            return i == cp.i && b == cp.b && Arrays.equals(bs, cp.bs) && Util.equals(c, cp.c);
        }


        public int hashCode() {
            return i ^ b ^ bs.hashCode() ^ c.hashCode();
        }


        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append(Integer.toString(i));
            buf.append(", ");
            buf.append(Byte.toString(b));
            buf.append(", [");
            for (int j = 0; j < bs.length; j++) {
                buf.append(Byte.toString(bs[j]));
                if (j < bs.length - 1) {
                    buf.append(", ");
                }
            }
            buf.append("], ");
            buf.append(c.toString());
            return buf.toString();
        }

    }

    /**
     * A {@link ContentEncoder} for the {@link XasData.Compound} type.
     */
    public static class CompoundEncoder implements ContentEncoder {

        private ContentEncoder chain;


        public CompoundEncoder(ContentEncoder chain) {
            this.chain = chain;
        }


        public boolean encode(Object o, String namespace, String name, TypedXmlSerializer ser)
                throws IOException {
            // System.out.println("CE.encode called with");
            // System.out.println("o=" + String.valueOf(o));
            // System.out.println("namespace=" + namespace);
            // System.out.println("name=" + name);
            boolean result = false;
            if (FOO_NS.equals(namespace) && CP_TYPE.equals(name) && o instanceof Compound) {
                Compound cp = (Compound) o;
                String prefix = ser.getPrefix(namespace, false);
                ser.attribute(XasUtil.XSI_NAMESPACE, "type", prefix + ":" + name);
                XmlWriter writer = new XmlWriter(ser);
                writer.typedElement("", "i", XasUtil.XSD_NAMESPACE, "int", new Integer(cp.getI()));
                writer.typedElement("", "b", XasUtil.XSD_NAMESPACE, "byte", new Byte(cp.getB()));
                writer.typedElement("", "bs", XasUtil.XSD_NAMESPACE, "base64Binary", cp.getBs());
                writer.typedElement("", "c", XasUtil.XSD_NAMESPACE, "dateTime", cp.getC());
                result = true;
            } else if (chain != null) {
                result = chain.encode(o, namespace, name, ser);
            }
            return result;
        }

    }

    /**
     * A {@link ContentDecoder} for the {@link XasData.Compound} type.
     */
    public static class CompoundDecoder extends ChainedContentDecoder {

        public Object decode(String typeNs, String typeName, XmlReader reader, EventList attributes) {
            // System.out.println("CD.decode called with");
            // System.out.println("typeNs=" + typeNs);
            // System.out.println("typeName=" + typeName);
            // System.out.println("reader=" + reader);
            Object result = null;
            if (FOO_NS.equals(typeNs) && CP_TYPE.equals(typeName)) {
                int pos = reader.getCurrentPosition();
                Integer io = (Integer) expect("", "i", reader);
                Byte bo = (Byte) expect("", "b", reader);
                byte[] bs = (byte[]) expect("", "bs", reader);
                Calendar c = (Calendar) expect("", "c", reader);
                // System.out.println("Compound: " + String.valueOf(io) + ", "
                // + String.valueOf(bo) + ", "
                // + String.valueOf(bs) + ", "
                // + String.valueOf(c));
                if (io != null && bo != null && bs != null && c != null) {
                    result = new Compound(io.intValue(), bo.byteValue(), bs, c);
                } else {
                    reader.setCurrentPosition(pos);
                }
            } else if (chain != null) {
                result = chain.decode(typeNs, typeName, reader, attributes);
            }
            return result;
        }


        public CompoundDecoder(ContentDecoder chain) {
            super(null);
            if (chain == null) { throw new IllegalArgumentException("Chained decoder must be"
                                                                    + " non-null"); }
            this.chain = chain;
        }

    }

    /**
     * A {@link ContentEncoder} for an integer list. The object is of type {@link List} with all
     * elements of type {@link Integer}.
     */
    public static class IntListEncoder implements ContentEncoder {

        private ContentEncoder chain;


        public IntListEncoder(ContentEncoder chain) {
            this.chain = chain;
        }


        public boolean encode(Object o, String namespace, String name, TypedXmlSerializer ser)
                throws IOException {
            // System.out.println("ILE.encode called with");
            // System.out.println("o=" + String.valueOf(o));
            // System.out.println("namespace=" + namespace);
            // System.out.println("name=" + name);
            boolean result = false;
            if (FOO_NS.equals(namespace) && LIST_TYPE.equals(name) && o instanceof List) {
                String prefix = ser.getPrefix(namespace, false);
                ser.attribute(XasUtil.XSI_NAMESPACE, "type", prefix + ":" + name);
                ser.attribute(FOO_NS, "reverse", "true");
                List l = (List) o;
                XmlWriter writer = new XmlWriter(ser);
                for (int i = l.size() - 1; i >= 0; i--) {
                    writer.typedElement(FOO_NS, "item", XasUtil.XSD_NAMESPACE, "int", l.get(i));
                }
                result = true;
            } else {
                result = chain.encode(o, namespace, name, ser);
            }
            return result;
        }

    }

    /**
     * A {@link ContentDecoder} for an integer list. The object is of type {@link List} with all
     * elements of type {@link Integer}.
     */
    public static class IntListDecoder extends ChainedContentDecoder {

        public IntListDecoder(ContentDecoder chain) {
            super(null);
            if (chain == null) { throw new IllegalArgumentException("Chained decoder must be"
                                                                    + " non-null"); }
            this.chain = chain;
        }


        public Object decode(String typeNs, String typeName, XmlReader reader, EventList attributes) {
            // System.out.println("ILD.decode called with");
            // System.out.println("typeNs=" + typeNs);
            // System.out.println("typeName=" + typeName);
            // System.out.println("reader=" + reader);
            Object result = null;
            if (FOO_NS.equals(typeNs) && LIST_TYPE.equals(typeName)) {
                boolean reverse = false;
                for (int i = 0; i < attributes.size(); i++) {
                    Event ev = attributes.get(i);
                    if (FOO_NS.equals(ev.getNamespace()) && "reverse".equals(ev.getName())) {
                        reverse = "true".equals(ev.getValue());
                        attributes.remove(i);
                    }
                }
                ArrayList l = new ArrayList();
                Object item = expect(FOO_NS, "item", reader);
                while (item != null) {
                    if (reverse) {
                        l.add(0, item);
                    } else {
                        l.add(item);
                    }
                    item = expect(FOO_NS, "item", reader);
                }
                result = l;
            } else if (chain != null) {
                result = chain.decode(typeNs, typeName, reader, attributes);
            }
            return result;
        }

    }

}
