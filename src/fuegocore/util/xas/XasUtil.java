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
import java.util.Enumeration;

import fuegocore.util.Util;

/**
 * General utilities often needed in connection with the XAS API. This class collects (as static
 * methods) several utility methods that are typically needed by applications using or extending the
 * XAS API.
 */
public class XasUtil {

    /*
     * Private constructor to prevent instantiation.
     */
    private XasUtil() {
    }

    /**
     * The XML Schema namespace name. All XML Schema data types are defined in this namespace. Since
     * these types are needed in many places, it is reasonable to have this namespace constant in a
     * central location.
     */
    public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

    /**
     * The XML Schema namespace instance name. Attribute names defined in XML Schema are in this
     * namespace. Of most interest is the type attribute defining an element's data type, which is
     * needed in many places.
     */
    public static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * The SOAP envelope namespace.
     */
    public static final String SOAP_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";

    /**
     * The SOAP encoding style value.
     */
    public static final String SOAP_ENCODING = "http://schemas.xmlsoap.org/soap/encoding/";

    public static final String XAS_NAMESPACE = "http://www.hiit.fi/fuego/fc/xas";

    /**
     * The name of the property for typed content encoders and decoders. A serializer needs to know
     * how to handle typed content events. This property, the value of which must be a
     * {@link ContentEncoder} object for {@link TypedXmlSerializer} objects and a
     * {@link ContentDecoder} object for <code>XmlPullParser</code> objects, gives the typed content
     * codecs to use.
     * <p>
     * It is strongly recommended that the existing codecs are always honored by chaining them after
     * any application-provided setting, since the predefined codecs are designed to know about the
     * encodings of the lowest-level datatypes.
     */
    public static final String PROPERTY_CONTENT_CODEC = "http://www.hiit.fi/fuego/fc/xas/typed-codec";


    /**
     * Serialize a single XML event. This method will output the given XML event to the given
     * serializer, possibly using the given special-purpose encoder.
     * @param ev
     *            the event to serialize
     * @param ser
     *            the serializer to use
     */
    public static void outputEvent(Event ev, TypedXmlSerializer ser) throws IOException {
        switch (ev.getType()) {
            case Event.START_DOCUMENT:
                ser.startDocument(null, null);
                break;
            case Event.END_DOCUMENT:
                ser.endDocument();
                break;
            case Event.START_ELEMENT:
                ser.startTag(ev.getNamespace(), ev.getName());
                break;
            case Event.ATTRIBUTE:
                ser.attribute(ev.getNamespace(), ev.getName(), (String) ev.getValue());
                break;
            case Event.END_ELEMENT:
                ser.endTag(ev.getNamespace(), ev.getName());
                break;
            case Event.CONTENT:
                ser.text((String) ev.getValue());
                break;
            case Event.TYPED_CONTENT:
                ser.typedContent(ev.getValue(), ev.getNamespace(), ev.getName());
                break;
            case Event.NAMESPACE_PREFIX:
                ser.setPrefix((String) ev.getValue(), ev.getNamespace());
                break;
            case Event.COMMENT:
                ser.comment((String) ev.getValue());
                break;
            case Event.PROCESSING_INSTRUCTION:
                ser.processingInstruction((String) ev.getValue());
                break;
            case Event.ENTITY_REFERENCE:
                ser.entityRef(ev.getName());
                break;
            default:
                if ((ev.getType() & Event.FLAG_BITMASK) == Event.TYPE_EXTENSION_FLAG) {
                    EventSequence es = (EventSequence) ev.getValue();
                    if (es != null) {
                        outputSequence(es, ser);
                    }
                } else {
                    throw new IOException("Unrecognized Event type " + ev.getType());
                }
        }
    }


    /**
     * Serialize a full XML event sequence. This method will output each event in the given sequence
     * to the given serializer in their sequential order.
     * @param es
     *            the event sequence to serialize
     * @param ser
     *            the serializer to use
     */
    public static void outputSequence(EventSequence es, TypedXmlSerializer ser) throws IOException {
        for (Enumeration e = es.events(); e.hasMoreElements();) {
            Event ev = (Event) e.nextElement();
            outputEvent(ev, ser);
        }
    }


    /**
     * Compare two event sequences for equality. It is often necessary to compare two event
     * sequences for equality even if their underlying implementations are completely different.
     * This method will compare two event sequences as if it iterated through each and comparing the
     * member events for equality. It is handy when implementing the {@link Object#equals} method in
     * classes implementing {@link EventSequence}.
     * @param es1
     *            the first event sequence
     * @param es2
     *            the second event sequence
     * @return <code>true</code> if the argument sequences are equal, <code>false</code> otherwise
     */
    public static boolean sequenceEquals(EventSequence es1, EventSequence es2) {
        boolean result = true;
        if (es1 == null) {
            if (es2 != null) {
                result = false;
            }
        } else if (es2 == null) {
            result = false;
        } else {
            Enumeration i = es1.events();
            Enumeration j = es2.events();
            while (result && i.hasMoreElements() && j.hasMoreElements()) {
                result = result && Util.equals(i.nextElement(), j.nextElement());
            }
            if (result && (i.hasMoreElements() || j.hasMoreElements())) {
                result = false;
            }
        }
        return result;
    }


    /**
     * Return a hash code for an event sequence. Since it is recommended that {@link EventSequence}
     * classes override the {@link Object#equals} method by calling {@link #sequenceEquals}, this
     * method is provided to be called when overriding the {@link Object#hashCode} method. It
     * returns a number that is guaranteed to be equal for any two event sequences for which
     * {@link #sequenceEquals} returns <code>true</code>.
     * @param es
     *            the event sequence for which to compute a hash value
     * @return the computed hash value
     */
    public static int sequenceHashCode(EventSequence es) {
        int result = 0;
        if (es != null) {
            for (Enumeration e = es.events(); e.hasMoreElements();) {
                result = 31 * result + Util.hashCode(e.nextElement());
            }
        }
        return result;
    }

}
