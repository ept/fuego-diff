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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Stack;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParserException;
import fuegocore.message.encoding.XebuConstants;
import fuegocore.util.MagicInputStream;
import fuegocore.util.Util;

/**
 * General utilities often needed in connection with the XAS API. This class collects (as static
 * methods) several utility methods that are typically needed by applications using or extending the
 * XAS API.
 * <p>
 * The difference between this class and the {@link XasUtil} class is that this class is allowed to
 * contain methods depending on libraries outside the MIDP and CLDC specifications. This
 * relationship parallels that between {@link fuegocore.util.Util} and
 * {@link fuegocore.util.ExtUtil}.
 */
public class XasExtUtil {

    private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();


    /*
     * Private constructor to prevent instantiation.
     */
    private XasExtUtil() {
    }


    private static String searchPrefix(String namespace, Stack nss) {
        for (int i = nss.size() - 1; i >= 0; i--) {
            EventList el = (EventList) nss.elementAt(i);
            for (int j = 0; j < el.size(); j++) {
                Event ev = el.get(j);
                if (Util.equals(namespace, ev.getNamespace())) { return (String) ev.getValue(); }
            }
        }
        return null;
    }


    private static Node readerToDom(Document doc, XmlReader xr, Stack nss) {
        Node result = null;
        Event ev = xr.getCurrentEvent();
        // System.out.println("Read event " + ev);
        EventList pms = new EventList();
        switch (ev.getType()) {
            case Event.START_DOCUMENT: {
                xr.advance();
                Node node;
                while ((node = readerToDom(doc, xr)) != null) {
                    doc.appendChild(node);
                }
                result = doc;
                break;
            }
            case Event.END_DOCUMENT:
                xr.advance();
                break;
            case Event.NAMESPACE_PREFIX:
                do {
                    pms.add(xr.advance());
                } while ((ev = xr.getCurrentEvent()).getType() == Event.NAMESPACE_PREFIX);
            case Event.START_ELEMENT: {
                nss.push(pms);
                String ns = ev.getNamespace();
                String name = ev.getName();
                String prefix = searchPrefix(ns, nss);
                if (prefix != null) {
                    name = prefix + ":" + name;
                }
                Element elem = doc.createElementNS(ns, name);
                for (Enumeration e = pms.events(); e.hasMoreElements();) {
                    ev = (Event) e.nextElement();
                    elem.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" + ev.getValue(),
                                        ev.getNamespace());
                }
                xr.advance();
                while ((ev = xr.getCurrentEvent()).getType() == Event.ATTRIBUTE) {
                    ns = ev.getNamespace();
                    name = ev.getName();
                    prefix = searchPrefix(ns, nss);
                    if (prefix != null) {
                        name = prefix + ":" + name;
                    }
                    elem.setAttributeNS(ns, name, (String) ev.getValue());
                    xr.advance();
                }
                Node node;
                while ((node = readerToDom(doc, xr, nss)) != null) {
                    elem.appendChild(node);
                }
                nss.pop();
                result = elem;
                break;
            }
            case Event.END_ELEMENT:
                xr.advance();
                break;
            case Event.CONTENT:
                result = doc.createTextNode((String) ev.getValue());
                xr.advance();
                break;
            case Event.COMMENT:
                result = doc.createComment((String) ev.getValue());
                xr.advance();
                break;
            case Event.PROCESSING_INSTRUCTION: {
                String target = (String) ev.getValue();
                String data = null;
                int i = target.indexOf(' ');
                if (i >= 0) {
                    data = target.substring(i + 1);
                    target = target.substring(0, i);
                }
                result = doc.createProcessingInstruction(target, data);
                xr.advance();
                break;
            }
            case Event.ENTITY_REFERENCE:
                result = doc.createEntityReference(ev.getName());
                xr.advance();
                break;
            default:
                if ((ev.getType() & Event.FLAG_BITMASK) == Event.TYPE_EXTENSION_FLAG) {
                    EventSequence es = (EventSequence) ev.getValue();
                    if (es != null) {
                        result = sequenceToDom(doc, es);
                    }
                } else {
                    throw new IllegalArgumentException("Unhandled event type: " + ev.getType());
                }
        }
        // System.out.println("Returning " + result);
        return result;
    }


    public static Document newDocument() throws ParserConfigurationException {
        return dbf.newDocumentBuilder().newDocument();
    }


    /**
     * Convert XML from an XML reader to a DOM tree. This method takes a {@link XmlReader} and
     * returns a {@link Node} representing the same XML document or a fragment of it. The XML
     * represented by the underlying event sequence of the reader needs to be well-formed and
     * balanced, and must not contain {@link Event#TYPED_CONTENT} events. A DOM {@link Document}
     * object needs to be provided to act as a factory for DOM {@link Node} objects.
     * <p>
     * If the underlying event sequence of the reader represents a complete XML document, the passed
     * in DOM {@link Document} needs to be empty, since the resulting document will be built into
     * that.
     * @param doc
     *            the DOM document to use for creating DOM nodes
     * @param xr
     *            the reader from which to read the converted event sequence
     * @return the DOM node representing <code>es</code>
     * @throws IllegalArgumentException
     *             if the XML represented by <code>xr</code>'s underlying event sequence is not
     *             convertible to DOM
     */
    public static Node readerToDom(Document doc, XmlReader xr) {
        return readerToDom(doc, xr, new Stack());
    }


    /**
     * Convert XML from an event sequence to a DOM tree. This method takes a {@link EventSequence}
     * and returns a {@link Node} representing the same XML document or a fragment of it. The XML
     * represented by the event sequence needs to be well-formed and balanced, and must not contain
     * {@link Event#TYPED_CONTENT} events. A DOM {@link Document} object needs to be provided to act
     * as a factory for DOM {@link Node} objects.
     * <p>
     * If the event sequence represents a complete XML document, the passed in DOM {@link Document}
     * needs to be empty, since the resulting document will be built into that.
     * @param doc
     *            the DOM document to use for creating DOM nodes
     * @param es
     *            the event sequence to convert
     * @return the DOM node representing <code>es</code>
     * @throws IllegalArgumentException
     *             if the XML represented by <code>es</code> is not convertible to DOM
     */
    public static Node sequenceToDom(Document doc, EventSequence es) {
        XmlReader reader = new XmlReader(es);
        return readerToDom(doc, reader);
    }


    /**
     * Convert XML from a DOM tree to an event sequence. This method takes a DOM tree as a
     * {@link Node} and returns a {@link EventSequence} representing the same XML document or a
     * fragment of it. Some DOM features (such as document types) are not supported by this method,
     * since {@link EventSequence} does not support them.
     * @param n
     *            the DOM node to convert
     * @return the event sequence representing <code>n</code>
     * @throws IllegalArgumentException
     *             if the XML represented by <code>n</code> contains unsupported features
     */
    public static EventSequence domToSequence(Node n) {
        EventList el = new EventList();
        switch (n.getNodeType()) {
            case Node.DOCUMENT_NODE: {
                el.add(Event.createStartDocument());
                NodeList nodes = n.getChildNodes();
                if (nodes != null) {
                    for (int i = 0; i < nodes.getLength(); i++) {
                        el.addAll(domToSequence(nodes.item(i)));
                    }
                }
                el.add(Event.createEndDocument());
                break;
            }
            case Node.ELEMENT_NODE: {
                EventList pms = new EventList();
                EventList atts = new EventList();
                NamedNodeMap attMap = n.getAttributes();
                for (int i = 0; i < attMap.getLength(); i++) {
                    Attr att = (Attr) attMap.item(i);
                    String name = att.getNodeName();
                    if (name.startsWith("xmlns:")) {
                        pms.add(Event.createNamespacePrefix(att.getValue(), name.substring(6)));
                    } else {
                        atts.add(Event.createAttribute(att.getNamespaceURI(), att.getLocalName(),
                                                       att.getValue()));
                    }
                }
                el.addAll(pms);
                el.add(Event.createStartElement(n.getNamespaceURI(), n.getLocalName()));
                el.addAll(atts);
                NodeList children = n.getChildNodes();
                if (children != null) {
                    for (int i = 0; i < children.getLength(); i++) {
                        el.addAll(domToSequence(children.item(i)));
                    }
                }
                el.add(Event.createEndElement(n.getNamespaceURI(), n.getLocalName()));
                break;
            }
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                el.add(Event.createContent(n.getNodeValue()));
                break;
            case Node.ENTITY_REFERENCE_NODE:
                el.add(Event.createEntityReference(n.getNodeName()));
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                el.add(Event.createProcessingInstruction(n.getNodeName() + " " + n.getNodeValue()));
                break;
            case Node.COMMENT_NODE:
                el.add(Event.createComment(n.getNodeValue()));
                break;
            default:
                throw new IllegalArgumentException("Unhandled node type: " + n.getNodeType());
        }
        return el;
    }


    /**
     * Get an XML parser suitable for the given input stream. Uses a
     * {@link XasExtUtil.XebuXmlIdentifier} to determine the mime type.
     * @param in
     *            input stream that will be attached to the parser
     * @param enc
     *            input character encoding, e.g. <code>utf-8</code>
     * @return a parser suitable for the input stream, or null if none available
     * @throws IOException
     *             if an I/O error occured when determining the type of the stream.
     */
    public static TypedXmlParser getParser(InputStream in, String enc) throws IOException {
        return getParser(new XebuXmlIdentifier(in), enc);
    }


    /**
     * Get a parser suitable for the given input stream.
     * @param in
     *            input stream that will be attached to the parser
     * @param enc
     *            input character encoding, e.g. <code>utf-8</code>
     * @return a parser suitable for the input stream, or null if none available
     * @throws IOException
     *             if an I/O error occured when determining the type of the stream.
     */

    public static TypedXmlParser getParser(MagicInputStream in, String enc) throws IOException {
        String type = in.getMimeType();
        if (type == null) return null; // Can't determine parser
        CodecFactory cf = CodecIndustry.getFactory(type);
        if (cf == null) return null;
        TypedXmlParser p = cf.getNewDecoder(new Object());
        try {
            boolean isBinary = !(p instanceof DefaultXmlParser);
            p.setInput(in, isBinary ? p.getInputEncoding() : enc);
        } catch (XmlPullParserException ex) {
            IOException iox = new IOException("XmlPullParserException");
            iox.initCause(ex);
            throw iox;
        }
        return p;
    }


    /**
     * Get XML serializer for an output stream. The serializer uses the recommended MIME type for
     * XML on the platform (usually a type belongin to the <code>application/x-ebu</code> family)
     * @return a serializer, or <code>null</code> if none available
     * @param out
     *            output stream for serializer
     * @param enc
     *            character encoding
     * @return serializer
     * @throws IOException
     *             if the serializer causes an IOException
     */
    public static TypedXmlSerializer getSerializer(OutputStream out, String enc) throws IOException {
        return getSerializer(out, "application/x-ebu+item", enc);
    }


    /**
     * Get XML serializer for an output stream. Use <code>text/xml</code> to get textual, idented
     * output.
     * @param out
     *            target stream for the serializer
     * @param mimeType
     *            MIME type of output
     * @param enc
     *            Output encoding (ignored for Xebu formats)
     * @return a serializer, or <code>null</code> if none available
     * @throws IOException
     *             if the serializer causes an IOException
     */

    public static TypedXmlSerializer getSerializer(OutputStream out, String mimeType, String enc)
            throws IOException {
        CodecFactory cf = CodecIndustry.getFactory(mimeType);
        if (cf == null) return null;
        TypedXmlSerializer ser = cf.getNewEncoder(new Object());
        boolean isBinary = !(ser instanceof DefaultXmlSerializer);
        ser.setOutput(out, isBinary ? "ISO-8859-1" : enc);
        if (!isBinary)
            ser.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        return ser;
    }

    /**
     * Identifies textual and Xebu encoded XML files. Identification is done by inspecting the first
     * 4 bytes of the file. The case insensitive string <code>&lt;?xm</code> identifies the file as
     * <code>text/xml</code>. A Xebu <code>START_DOCUMENT</code> event identifies the content as
     * Xebu encoded; the correct subtype is determined from the document start flags.
     */

    public static class XebuXmlIdentifier extends MagicInputStream {

        public XebuXmlIdentifier(InputStream in) throws IOException {
            super(in);
        }


        protected String identify(byte[] magic, int len) {
            final String[] SUBTYPES = { "none", "item", null, "data", "elem", "elit", null, "elid" };
            String ms = new String(magic, 0, len);
            if (len == 4 && ms.toLowerCase().startsWith("<?xm")) return "text/xml";
            if (len < 1) return null;
            if ((magic[0] & XebuConstants.TOKEN_SPACE) == XebuConstants.DOCUMENT) {
                int subtype = ((magic[0] & XebuConstants.FLAG_SEQUENCE_CACHING) != 0 ? 4 : 0) +
                              ((magic[0] & XebuConstants.FLAG_CONTENT_CACHING) != 0 ? 2 : 0) +
                              ((magic[0] & XebuConstants.FLAG_ITEM_CACHING) != 0 ? 1 : 0);
                return SUBTYPES[subtype] == null ? null : "application/x-ebu+" + SUBTYPES[subtype];
            }
            return null;
        }

    }
}
