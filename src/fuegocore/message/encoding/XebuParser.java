/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.message.encoding;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.xmlpull.v1.XmlPullParserException;

import fuegocore.util.Util;
import fuegocore.util.xas.Event;
import fuegocore.util.xas.EventList;
import fuegocore.util.xas.EventSequence;
import fuegocore.util.xas.TypedXmlParser;
import fuegocore.util.xas.ContentDecoder;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.xas.XmlReader;

/**
 * A parser for Xebu documents. This class implements the parsing of Xebu documents as described in
 * the Xebu specification. The parsing interface is {@link TypedXmlParser}. The parser implements
 * all caching features of the Xebu format.
 * <p>
 * This class is not a complete implementation of the {@link TypedXmlParser} interface, mainly
 * because some methods inherited from the <code>XmlPullParser</code> interface are too high-level.
 * The preference is to wrap a {@link TypedXmlParser} inside a
 * {@link fuegocore.util.xas.EventStream} and use the XAS API to implement high-level views of XML
 * documents.
 */
public class XebuParser extends ContentDecoder implements TypedXmlParser {

    private boolean cacheItem;
    private boolean cacheContent;
    private boolean cacheSequence;

    private int eventType;
    private int depth = 0;
    private boolean inProgress = false;
    private boolean readingContent = false;
    private boolean readContentFromStream = false;
    private int position = 0;

    private Reader reader;
    private int pushback = -1;

    private static final int BUFFER_SIZE = 512;
    private char[] buffer = new char[BUFFER_SIZE];
    private int bufferOffset = 0;
    private int endOffset = 0;

    private String[] elementStack = new String[16];
    private String[] namespaceStack = new String[8];
    private int[] namespaceCounts = new int[4];

    private int attributeCount = 0;
    private String[] attributeStack = new String[16];

    private String currentData;
    private Object currentObject;
    private String typedData = null;
    private int offset = 0;
    private String namespace;
    private String name;
    private boolean areDecoded = false;
    private ContentDecoder decoder = this;
    private DoaMachine doa;

    private Object[][] caches;

    private Vector sequenceCache;
    private EventList currentSequence;
    private Enumeration cachedSequence;
    private Event cachedEvent = null;


    private int tokenize(int c) {
        return c & XebuConstants.TOKEN_SPACE;
    }


    private int flagize(int c) {
        return c & XebuConstants.FLAG_SPACE;
    }


    private int fillBuffer(int n) throws IOException {
        if (n > buffer.length - bufferOffset) {
            int bufferLength = endOffset - bufferOffset;
            if (n > buffer.length) {
                char[] newBuffer = new char[n];
                System.arraycopy(buffer, bufferOffset, newBuffer, 0, bufferLength);
                buffer = newBuffer;
            } else {
                System.arraycopy(buffer, bufferOffset, buffer, 0, bufferLength);
            }
            bufferOffset = 0;
            endOffset = bufferLength;
        }
        while (n > endOffset - bufferOffset) {
            int len = reader.read(buffer, endOffset, buffer.length - endOffset);
            if (len <= 0) {
                break;
            }
            endOffset += len;
        }
        if (n > endOffset - bufferOffset) { return -1; }
        return 0;
    }


    private int readToken() throws IOException {
        int c = pushback;
        if (c == -1) {
            if (fillBuffer(1) >= 0) {
                position += 1;
                c = buffer[bufferOffset++];
            }
        } else {
            pushback = -1;
        }
        return c;
    }


    private int readTypedToken() throws IOException {
        if (offset < typedData.length()) {
            return (int) typedData.charAt(offset++);
        } else {
            return readToken();
        }
    }


    private String readString(int length) throws IOException {
        String result = "";
        String prefix = null;
        int c = pushback;
        pushback = -1;
        if (c != -1) {
            prefix = new String(new char[] { (char) c });
            length -= 1;
        }
        if (fillBuffer(length) >= 0) {
            position += length;
            result = new String(buffer, bufferOffset, length);
            bufferOffset += length;
        }
        if (prefix != null) {
            return prefix + result;
        } else {
            return result;
        }
    }


    private String readTypedString(int length) throws IOException {
        if (typedData.length() >= offset + length) {
            String result = typedData.substring(offset, offset + length);
            offset += length;
            return result;
        } else {
            /*
             * This area needs improvement in object creation
             */
            int remain = typedData.length() - offset;
            StringBuffer result = new StringBuffer(length);
            result.append(typedData.substring(offset, offset + remain));
            result.append(readString(length - remain));
            offset = typedData.length();
            return result.toString();
        }
    }


    private int readCompressedInt() throws IOException {
        int result = 0;
        int c = readToken();
        if ((c & 0x80) == 0) {
            result = c;
        } else if ((c & 0xC0) == 0x80) {
            result = (c & 0x3F) << 8;
            result |= readToken();
        } else {
            for (int i = 0; i < 4; i++) {
                result <<= 8;
                result |= readToken();
            }
        }
        return result;
    }


    private int readTypedCompressedInt() throws IOException {
        int result = 0;
        int c = readTypedToken();
        if ((c & 0x80) == 0) {
            result = c;
        } else if ((c & 0xC0) == 0x80) {
            result = (c & 0x3F) << 8;
            result |= readTypedToken();
        } else {
            for (int i = 0; i < 4; i++) {
                result <<= 8;
                result |= readTypedToken();
            }
        }
        return result;
    }


    private void checkInProgress() {
        if (inProgress) { throw new IllegalStateException("Requested operation not "
                                                          + "permitted while processing "
                                                          + "a document"); }
    }


    private boolean hasCachedEvents() {
        return cachedEvent != null || (cachedSequence != null && cachedSequence.hasMoreElements());
    }


    private Event nextCachedEvent() {
        Event result = null;
        if (cachedEvent != null) {
            result = cachedEvent;
            cachedEvent = null;
        } else if (cachedSequence != null && cachedSequence.hasMoreElements()) {
            result = (Event) cachedSequence.nextElement();
        }
        return result;
    }


    private String readData() throws IOException {
        int len = readCompressedInt();
        return readString(len);
    }


    private String readTypedData() throws IOException {
        int len = readTypedCompressedInt();
        return readTypedString(len);
    }


    private String readFullString() throws IOException {
        int len = readToken();
        return readString(len);
    }


    private String readCachedNamespace() throws IOException {
        int index = -1;
        if (cacheItem) {
            index = readToken();
        }
        String result = readFullString();
        if (cacheItem) {
            caches[XebuConstants.NAMESPACE_INDEX][index] = result;
        }
        return result;
    }


    private String readCachedName(String namespace) throws IOException {
        int index = -1;
        if (cacheItem) {
            index = readToken();
        }
        String result = readFullString();
        if (cacheItem) {
            caches[XebuConstants.NAME_INDEX][index] = Event.createStartElement(namespace, result);
        }
        return result;
    }


    private String readCachedValue(String namespace, String name) throws IOException {
        int index = -1;
        if (cacheContent) {
            index = readToken();
        }
        String result = readFullString();
        if (cacheContent) {
            caches[XebuConstants.VALUE_INDEX][index] = Event.createTypedContent(namespace, name,
                                                                                result);
        }
        return result;
    }


    private String readCachedContent() throws IOException {
        int index = -1;
        if (cacheContent) {
            index = readToken();
        }
        String result = readData();
        if (cacheContent) {
            caches[XebuConstants.CONTENT_INDEX][index] = result;
        }
        return result;
    }


    private String readCachedEntity() throws IOException {
        int index = -1;
        if (cacheItem) {
            index = readToken();
        }
        String result = readFullString();
        if (cacheItem) {
            caches[XebuConstants.CONTENT_INDEX][index] = result;
        }
        return result;
    }


    private void insertNamespacePrefix(String namespace, String prefix) {
        // System.out.println("iNP(" + namespace + "," + prefix + ")");
        int current = namespaceCounts[depth + 1];
        namespaceStack = Util.ensureCapacity(namespaceStack, 2 * (current + 1));
        namespaceStack[2 * current] = namespace;
        namespaceStack[2 * current + 1] = prefix;
        namespaceCounts[depth + 1] += 1;
    }


    private void insertStartElement(String namespace, String name) {
        // System.out.println("iSE(" + namespace + "," + name + ")");
        elementStack = Util.ensureCapacity(elementStack, 2 * (depth + 1));
        elementStack[2 * depth] = namespace;
        elementStack[2 * depth + 1] = name;
        depth += 1;
        namespaceCounts = Util.ensureCapacity(namespaceCounts, depth + 2);
        namespaceCounts[depth + 1] = namespaceCounts[depth];
        attributeCount = 0;
    }


    private void insertAttribute(String namespace, String name, String value) {
        // System.out.println("iA(" + namespace + "," + name + "," + value +
        // ")");
        attributeStack = Util.ensureCapacity(attributeStack, 3 * (attributeCount + 1));
        attributeStack[3 * attributeCount] = namespace;
        attributeStack[3 * attributeCount + 1] = name;
        attributeStack[3 * attributeCount + 2] = value;
        attributeCount += 1;
        if (Util.equals(name, "type") && Util.equals(namespace, XasUtil.XSI_NAMESPACE)) {
            int i = value.indexOf(':');
            if (i > 0) {
                this.namespace = getNamespace(value.substring(0, i));
                this.name = value.substring(i + 1);
                areDecoded = false;
                // System.out.println("{" + this.namespace + "}" + this.name);
            }
        }
    }


    private void advanceReaderAttribute(int c) throws IOException, XmlPullParserException {
        String ans, an, value;
        int flags = flagize(c);
        if ((flags & XebuConstants.VALUE_FETCH) == XebuConstants.VALUE_FETCH) {
            int index = readToken();
            Event ev = (Event) caches[XebuConstants.VALUE_INDEX][index];
            if (ev == null) { throw new XmlPullParserException("No value cached at index " + index,
                                                               this, null); }
            // assert ev.getType() == Event.TYPED_CONTENT
            // : "Bogus event type in value cache: "
            // + ev.getType();
            ans = ev.getNamespace();
            an = ev.getName();
            value = (String) ev.getValue();
        } else if ((flags & XebuConstants.NAME_FETCH) == XebuConstants.NAME_FETCH) {
            int index = readToken();
            Event ev = (Event) caches[XebuConstants.NAME_INDEX][index];
            if (ev == null) { throw new XmlPullParserException("No name cached at index " + index,
                                                               this, null); }
            // assert ev.getType() == Event.START_ELEMENT
            // : "Bogus event type in name cache: "
            // + ev.getType();
            ans = ev.getNamespace();
            an = ev.getName();
            value = readCachedValue(ans, an);
        } else if ((flags & XebuConstants.NAMESPACE_FETCH) == XebuConstants.NAMESPACE_FETCH) {
            int index = readToken();
            ans = (String) caches[XebuConstants.NAMESPACE_INDEX][index];
            if (ans == null) { throw new XmlPullParserException("No namespace cached at " +
                                                                "index " + index, this, null); }
            an = readCachedName(ans);
            value = readCachedValue(ans, an);
        } else {
            ans = readCachedNamespace();
            an = readCachedName(ans);
            value = readCachedValue(ans, an);
        }
        Event ev = Event.createAttribute(ans, an, value);
        doa.nextEvent(ev);
    }


    private Event advanceAttribute() throws IOException, XmlPullParserException {
        Event result = null;
        boolean ready = false;
        // System.out.println("Attr: <" + hasCachedEvents() + "><"
        // + doa.hasEvents() + ">");
        while (!ready) {
            if (hasCachedEvents()) {
                ready = true;
                result = nextCachedEvent();
                if (result.getType() != Event.ATTRIBUTE) {
                    cachedEvent = result;
                    result = null;
                }
                // System.out.println("Cached attribute: " + result);
            } else if (doa.hasEvents()) {
                ready = true;
                result = doa.peekEvent();
                if (result.getType() == Event.ATTRIBUTE) {
                    result = doa.getEvent();
                    if (cacheSequence) {
                        currentSequence.add(result);
                    }
                } else {
                    result = null;
                }
                // System.out.println("DOA attribute: " + result);
            } else {
                int c = readToken();
                // System.out.println("Read token " + c);
                if ((c & XebuConstants.TOKEN_SPACE) == XebuConstants.ATTRIBUTE) {
                    advanceReaderAttribute(c);
                } else {
                    if ((c & XebuConstants.TOKEN_SPACE) != XebuConstants.ATTRIBUTE_END) {
                        pushback = c;
                    }
                    ready = true;
                }
            }
        }
        return result;
    }


    private int advancePreparedEvent(Event ev) throws IOException, XmlPullParserException {
        int result = -1;
        // System.out.println("Prepared event: " + ev);
        switch (ev.getType()) {
            case Event.START_DOCUMENT:
                break;
            case Event.END_DOCUMENT:
                result = END_DOCUMENT;
                break;
            case Event.START_ELEMENT:
                insertStartElement(ev.getNamespace(), ev.getName());
                while ((ev = advanceAttribute()) != null) {
                    insertAttribute(ev.getNamespace(), ev.getName(), (String) ev.getValue());
                }
                result = START_TAG;
                break;
            case Event.ATTRIBUTE:
                insertAttribute(ev.getNamespace(), ev.getName(), (String) ev.getValue());
                break;
            case Event.END_ELEMENT:
                result = END_TAG;
                break;
            case Event.CONTENT:
                currentData = (String) ev.getValue();
                result = TEXT;
                break;
            case Event.TYPED_CONTENT:
                currentObject = ev.getValue();
                result = OBJECT;
                break;
            case Event.NAMESPACE_PREFIX:
                insertNamespacePrefix(ev.getNamespace(), (String) ev.getValue());
                break;
            case Event.COMMENT:
                currentData = (String) ev.getValue();
                result = COMMENT;
                break;
            case Event.PROCESSING_INSTRUCTION:
                currentData = (String) ev.getValue();
                result = PROCESSING_INSTRUCTION;
                break;
            case Event.ENTITY_REFERENCE:
                name = ev.getName();
                result = ENTITY_REF;
                break;
            default:
                throw new XmlPullParserException("Invalid type for prepared event:" + " " +
                                                 ev.getType(), this, null);
        }
        if (areDecoded && result != OBJECT) {
            this.namespace = null;
            this.name = null;
        }
        return result;
    }


    private void advanceReaderEvent() throws IOException, XmlPullParserException {
        Event doaEvent = null;
        int c = readToken();
        if (c == -1) {
            if (inProgress) {
                doaEvent = Event.createEndDocument();
                inProgress = false;
            }
        } else {
            // System.out.println("Read character " + Integer.toHexString(c));
            int token = tokenize(c);
            int flags = flagize(c);
            switch (token) {
                case XebuConstants.DOCUMENT:
                    cacheItem = (flags & XebuConstants.FLAG_ITEM_CACHING) != 0;
                    cacheContent = (flags & XebuConstants.FLAG_CONTENT_CACHING) != 0;
                    cacheSequence = (flags & XebuConstants.FLAG_SEQUENCE_CACHING) != 0;
                    doaEvent = Event.createStartDocument();
                    if (cacheSequence) {
                        currentSequence = new EventList();
                    }
                    // System.out.println("SD(" + Integer.toHexString(flags) + ")");
                    break;
                case XebuConstants.NAMESPACE_START: {
                    String namespace = null;
                    String name = null;
                    if ((flags & XebuConstants.NAME_FETCH) == XebuConstants.NAME_FETCH) {
                        int index = readToken();
                        Event ev = (Event) caches[XebuConstants.NAME_INDEX][index];
                        if (ev == null) { throw new XmlPullParserException("No name cached at " +
                                                                           "index " + index, this,
                                                                           null); }
                        namespace = ev.getNamespace();
                        name = ev.getName();
                    } else if ((flags & XebuConstants.NAMESPACE_FETCH) == XebuConstants.NAMESPACE_FETCH) {
                        int index = readToken();
                        namespace = (String) caches[XebuConstants.NAMESPACE_INDEX][index];
                        if (namespace == null) { throw new XmlPullParserException(
                                                                                  "No namespace cached " +
                                                                                          "at index " +
                                                                                          index,
                                                                                  this, null); }
                        name = readCachedName(namespace);
                    } else {
                        namespace = readCachedNamespace();
                        name = readCachedName(namespace);
                    }
                    doaEvent = Event.createNamespacePrefix(namespace, name);
                    // System.out.println("NS(" + namespace + "=" + name + ")");
                    break;
                }
                case XebuConstants.ELEMENT_START: {
                    String namespace = null;
                    String name = null;
                    if ((flags & XebuConstants.NAME_FETCH) == XebuConstants.NAME_FETCH) {
                        int index = readToken();
                        Event ev = (Event) caches[XebuConstants.NAME_INDEX][index];
                        if (ev == null) { throw new XmlPullParserException("No name cached at " +
                                                                           "index " + index, this,
                                                                           null); }
                        namespace = ev.getNamespace();
                        name = ev.getName();
                    } else if ((flags & XebuConstants.NAMESPACE_FETCH) == XebuConstants.NAMESPACE_FETCH) {
                        int index = readToken();
                        namespace = (String) caches[XebuConstants.NAMESPACE_INDEX][index];
                        if (namespace == null) { throw new XmlPullParserException(
                                                                                  "No namespace cached " +
                                                                                          "at index " +
                                                                                          index,
                                                                                  this, null); }
                        name = readCachedName(namespace);
                    } else {
                        namespace = readCachedNamespace();
                        name = readCachedName(namespace);
                    }
                    doaEvent = Event.createStartElement(namespace, name);
                    // System.out.println("ES({" + namespace + "}" + name + ")");
                    break;
                }
                case XebuConstants.ATTRIBUTE:
                    advanceReaderAttribute(c);
                    break;
                case XebuConstants.ELEMENT_END:
                    doaEvent = Event.createEndElement(elementStack[2 * (depth - 1)],
                                                      elementStack[2 * depth - 1]);
                    // System.out.println("EE()");
                    break;
                case XebuConstants.DATA_START: {
                    String data = null;
                    if ((flags & XebuConstants.VALUE_FETCH) == XebuConstants.VALUE_FETCH) {
                        int index = readToken();
                        data = (String) caches[XebuConstants.CONTENT_INDEX][index];
                        if (data == null) { throw new XmlPullParserException("No content cached " +
                                                                             "at index " + index,
                                                                             this, null); }
                    } else {
                        data = readCachedContent();
                    }
                    doaEvent = Event.createContent(data);
                    if ((flags & XebuConstants.COALESCE_FLAG) == XebuConstants.COALESCE_FLAG) {
                        readingContent = true;
                    }
                    // System.out.println("C(" + data + ")");
                    break;
                }
                case XebuConstants.TYPED_DATA: {
                    if ((flags & XebuConstants.VALUE_FETCH) == XebuConstants.VALUE_FETCH) {
                        int index = readToken();
                        typedData = (String) caches[XebuConstants.CONTENT_INDEX][index];
                    } else if ((flags & XebuConstants.TYPED_MULTIDATA_FLAG) == XebuConstants.TYPED_MULTIDATA_FLAG) {
                        // typedData = readCachedContent();
                        typedData = readData();
                    } else {
                        /*
                         * This area needs improvement in object creation
                         */
                        typedData = new String(new char[] { (char) readToken() });
                    }
                    // System.out.println("TD(" + Util.toPrintable(typedData) +
                    // ")");
                    offset = 0;
                    doa.promiseEvent(Event.TYPED_CONTENT);
                    break;
                }
                case XebuConstants.SEQUENCE_FETCH: {
                    if (!cacheSequence) { throw new XmlPullParserException("Sequence caching "
                                                                           + "operation not "
                                                                           + "permitted for this "
                                                                           + "document", this, null); }
                    int index = readToken();
                    index |= flags << 4;
                    if (index < 0 || index >= sequenceCache.size()) { throw new XmlPullParserException(
                                                                                                       "Invalid sequence cache " +
                                                                                                               "index: " +
                                                                                                               index,
                                                                                                       this,
                                                                                                       null); }
                    EventSequence es = (EventSequence) sequenceCache.elementAt(index);
                    // System.out.println("SQF(" + index + ") = " + es);
                    cachedSequence = es.events();
                    if (cacheSequence) {
                        currentSequence.addAll(es);
                    }
                    break;
                }
                case XebuConstants.SEQUENCE_ENTRY: {
                    if (!cacheSequence) { throw new XmlPullParserException("Sequence caching "
                                                                           + "operation not "
                                                                           + "permitted for this "
                                                                           + "document", this, null); }
                    int index = readToken();
                    index |= flags << 4;
                    int length = readToken();
                    int seqIndex = currentSequence.size() - length;
                    if (seqIndex < 0) { throw new XmlPullParserException(
                                                                         "Invalid cached sequence " +
                                                                                 "length: " +
                                                                                 length +
                                                                                 ", should have been " +
                                                                                 "less than " +
                                                                                 currentSequence.size(),
                                                                         this, null); }
                    // System.out.println("SQE(" + index + "," + length + ") = "
                    // + currentSequence.subSequence(seqIndex));
                    if (sequenceCache.size() <= index) {
                        sequenceCache.setSize(index + 1);
                    }
                    sequenceCache.setElementAt(currentSequence.subSequence(seqIndex), index);
                    break;
                }
                case XebuConstants.SPECIAL: {
                    switch (flags & XebuConstants.SPECIAL_MASK) {
                        case XebuConstants.FLAG_PI: {
                            String data = null;
                            if ((flags & 0x80) == 0x80) {
                                int index = readToken();
                                data = (String) caches[XebuConstants.CONTENT_INDEX][index];
                            } else {
                                data = readCachedContent();
                            }
                            doaEvent = Event.createProcessingInstruction(data);
                            // System.out.println("PI(" + data + ")");
                            break;
                        }
                        case XebuConstants.FLAG_COMMENT: {
                            String data = readData();
                            doaEvent = Event.createComment(data);
                            // System.out.println("CM(" + data + ")");
                            break;
                        }
                        case XebuConstants.FLAG_ENTITY: {
                            String name = null;
                            if ((flags & 0x80) == 0x80) {
                                int index = readToken();
                                name = (String) caches[XebuConstants.CONTENT_INDEX][index];
                            } else {
                                name = readCachedEntity();
                            }
                            doaEvent = Event.createEntityReference(name);
                            // System.out.println("ER(" + name + ")");
                            break;
                        }
                        default:
                            throw new XmlPullParserException(
                                                             "Read invalid special " +
                                                                     "character: " +
                                                                     Integer.toHexString(c), this,
                                                             null);
                    }
                    break;
                }
                default:
                    throw new XmlPullParserException("Read invalid character: " +
                                                     Integer.toHexString(c), this, null);
            }
        }
        if (doaEvent != null) {
            doa.nextEvent(doaEvent);
        }
    }


    private int advanceContentFromStream() throws IOException, XmlPullParserException {
        int result = -1;
        int c = readToken();
        if (c < 0) { throw new XmlPullParserException("Unexpected end of stream", this, null); }
        int token = tokenize(c);
        int flags = flagize(c);
        if (token == XebuConstants.DATA_START) {
            String data = null;
            if ((flags & XebuConstants.VALUE_FETCH) == XebuConstants.VALUE_FETCH) {
                int index = readToken();
                currentData = (String) caches[XebuConstants.CONTENT_INDEX][index];
                if (currentData == null) { throw new XmlPullParserException(
                                                                            "No content cached at " +
                                                                                    "index " +
                                                                                    index, this,
                                                                            null); }
            } else {
                currentData = readCachedContent();
            }
            result = TEXT;
        } else {
            if (token != XebuConstants.ATTRIBUTE_END) { throw new XmlPullParserException(
                                                                                         "Content sequence not ended "
                                                                                                 + "by end token",
                                                                                         this, null); }
            readContentFromStream = false;
        }
        return result;
    }


    private void advanceEvent() throws IOException, XmlPullParserException {
        if (reader == null) { throw new XmlPullParserException("No input source specified", this,
                                                               null); }
        int type = -1;
        if (eventType == START_DOCUMENT) {
            inProgress = true;
        } else if (eventType == END_DOCUMENT) {
            type = eventType;

        } else if (eventType == END_TAG) {
            depth -= 1;
        }
        while (type == -1) {
            if (readContentFromStream) {
                type = advanceContentFromStream();
            } else if (hasCachedEvents()) {
                type = advancePreparedEvent(nextCachedEvent());
            } else if (doa.hasEvents()) {
                Event ev = doa.getEvent();
                if (cacheSequence) {
                    currentSequence.add(ev);
                }
                if (readingContent && ev.getType() == Event.CONTENT) {
                    readingContent = false;
                    readContentFromStream = true;
                }
                type = advancePreparedEvent(ev);
            } else if (typedData != null && offset < typedData.length()) {
                if (this.namespace == null || this.name == null) {
                    doa.promiseEvent(Event.TYPED_CONTENT);
                } else {
                    Object object = primitiveDecode(this.namespace, this.name);
                    doa.nextEvent(Event.createTypedContent(this.namespace, this.name, object));
                    areDecoded = true;
                }
            } else {
                advanceReaderEvent();
            }
        }
        eventType = type;
        return;
    }


    XebuParser(Object[][] caches) {
        this(caches, new Vector());
    }


    XebuParser(Vector sequenceCache) {
        this(new Object[XebuConstants.INDEX_NUMBER][XebuConstants.CACHE_SIZE], sequenceCache);
    }


    XebuParser(Object[][] caches, Vector sequenceCache) {
        this(caches, sequenceCache, new IdentityDoaMachine());
    }


    XebuParser(Object[][] caches, Vector sequenceCache, DoaMachine doa) {
        this.caches = caches;
        this.sequenceCache = sequenceCache;
        this.doa = doa;
    }


    public XebuParser() {
        this(new Object[XebuConstants.INDEX_NUMBER][XebuConstants.CACHE_SIZE]);
    }


    public void setFeature(String name, boolean state) {
    }


    public boolean getFeature(String name) {
        return false;
    }


    public void setProperty(String name, Object value) {
        if (XebuConstants.PROPERTY_INITIAL_CACHES.equals(name)) {
            checkInProgress();
            if (!(value instanceof Object[][])) { throw new IllegalArgumentException(
                                                                                     "Value " +
                                                                                             value +
                                                                                             " is " +
                                                                                             "not a valid initial " +
                                                                                             "cache"); }
            caches = (Object[][]) value;
        } else if (XebuConstants.PROPERTY_INITIAL_SEQUENCE_CACHE.equals(name)) {
            checkInProgress();
            if (!(value instanceof Vector)) { throw new IllegalArgumentException(
                                                                                 "Value " +
                                                                                         value +
                                                                                         " is " +
                                                                                         "not a valid initial " +
                                                                                         "sequence cache"); }
            sequenceCache = (Vector) value;
        } else if (XebuConstants.PROPERTY_COA_MACHINE.equals(name)) {
            checkInProgress();
            if (!(value instanceof DoaMachine)) { throw new IllegalArgumentException(
                                                                                     "Value " +
                                                                                             value +
                                                                                             " is " +
                                                                                             "not a valid COA " +
                                                                                             "machine"); }
            doa = (DoaMachine) value;
        } else if (XasUtil.PROPERTY_CONTENT_CODEC.equals(name)) {
            checkInProgress();
            if (!(value instanceof ContentDecoder)) { throw new IllegalArgumentException(
                                                                                         "Value " +
                                                                                                 value +
                                                                                                 " is " +
                                                                                                 "not a valid content " +
                                                                                                 "decoder"); }
            decoder = (ContentDecoder) value;
        } else {
            throw new IllegalStateException("Property " + name + " not supported");
        }
    }


    public Object getProperty(String name) {
        Object result = null;
        if (XebuConstants.PROPERTY_INITIAL_CACHES.equals(name)) {
            result = caches;
        } else if (XebuConstants.PROPERTY_INITIAL_SEQUENCE_CACHE.equals(name)) {
            result = sequenceCache;
        } else if (XebuConstants.PROPERTY_COA_MACHINE.equals(name)) {
            result = doa;
        } else if (XasUtil.PROPERTY_CONTENT_CODEC.equals(name)) {
            result = decoder;
        }
        return result;
    }


    public void setInput(Reader reader) {
        eventType = START_DOCUMENT;
        position = 0;
        this.reader = reader;
    }


    public void setInput(InputStream in, String encoding) throws XmlPullParserException {
        try {
            if (encoding != null) {
                setInput(new InputStreamReader(in, encoding));
            } else {
                setInput(new InputStreamReader(in));
            }
        } catch (Exception e) {
            throw new XmlPullParserException("Invalid encoding: " + encoding, this, e);
        }
    }


    public String getInputEncoding() {
        return "ISO-8859-1";
    }


    public void defineEntityReplacementText(String name, String text) {
    }


    public int getNamespaceCount(int depth) {
        if (depth > this.depth) { throw new IndexOutOfBoundsException("Invalid depth: " + depth); }
        return namespaceCounts[depth];
    }


    public String getNamespacePrefix(int pos) {
        return namespaceStack[2 * pos + 1];
    }


    public String getNamespaceUri(int pos) {
        return namespaceStack[2 * pos];
    }


    public String getNamespace(String prefix) {
        for (int i = 2 * (namespaceCounts[depth] - 1); i >= 0; i -= 2) {
            if (Util.equals(prefix, namespaceStack[i + 1])) { return namespaceStack[i]; }
        }
        return null;
    }


    public int getDepth() {
        return depth;
    }


    public String getPositionDescription() {
        StringBuffer buffer = new StringBuffer();
        if (eventType < TYPES.length) {
            buffer.append(TYPES[eventType]);
            buffer.append(":");
        }
        buffer.append(position);
        return buffer.toString();
    }


    public int getLineNumber() {
        return 1;
    }


    public int getColumnNumber() {
        return position;
    }


    public boolean isWhitespace() {
        return false;
    }


    public String getText() {
        if (eventType == TEXT || eventType == PROCESSING_INSTRUCTION || eventType == COMMENT) {
            return currentData;
        } else {
            return null;
        }
    }


    public char[] getTextCharacters(int[] startAndLength) {
        String text = getText();
        if (text != null) {
            startAndLength[0] = 0;
            startAndLength[1] = text.length();
            return text.toCharArray();
        } else {
            return null;
        }
    }


    public Object getObject() {
        if (eventType == OBJECT) {
            return currentObject;
        } else {
            return getText();
        }
    }


    public String getNamespace() {
        if (eventType == OBJECT) {
            return namespace;
        } else if (eventType != START_TAG && eventType != END_TAG) {
            return null;
        } else {
            return elementStack[2 * (depth - 1)];
        }
    }


    public String getName() {
        if (eventType == OBJECT || eventType == ENTITY_REF) {
            return name;
        } else if (eventType == START_TAG || eventType == END_TAG) {
            return elementStack[2 * depth - 1];
        } else {
            return null;
        }
    }


    public String getPrefix() {
        return null;
    }


    public boolean isEmptyElementTag() {
        return false;
    }


    public int getAttributeCount() {
        return attributeCount;
    }


    public String getAttributeNamespace(int index) {
        if (index >= attributeCount) { throw new IndexOutOfBoundsException("Invalid index: " +
                                                                           index); }
        return attributeStack[3 * index];
    }


    public String getAttributeName(int index) {
        if (index >= attributeCount) { throw new IndexOutOfBoundsException("Invalid index: " +
                                                                           index); }
        return attributeStack[3 * index + 1];
    }


    public String getAttributePrefix(int index) {
        return null;
    }


    public String getAttributeType(int index) {
        return "CDATA";
    }


    public boolean isAttributeDefault(int index) {
        return false;
    }


    public String getAttributeValue(int index) {
        if (index >= attributeCount) { throw new IndexOutOfBoundsException("Invalid index: " +
                                                                           index); }
        return attributeStack[3 * index + 2];
    }


    public String getAttributeValue(String namespace, String name) {
        for (int i = 0; i < 3 * attributeCount; i += 3) {
            if (Util.equals(name, attributeStack[i + 1]) &&
                Util.equals(namespace, attributeStack[i])) { return attributeStack[i + 2]; }
        }
        return null;
    }


    public int getEventType() {
        return eventType;
    }


    public int next() throws XmlPullParserException, IOException {
        advanceEvent();
        return eventType;
    }


    public int nextToken() throws XmlPullParserException, IOException {
        advanceEvent();
        return eventType;
    }


    public void require(int type, String namespace, String name) throws XmlPullParserException {
        if (type != eventType || (namespace != null && !Util.equals(namespace, getNamespace())) ||
            (name != null && !Util.equals(name, getName()))) { throw new XmlPullParserException(
                                                                                                "Expected " +
                                                                                                        type +
                                                                                                        ", got " +
                                                                                                        getPositionDescription(),
                                                                                                this,
                                                                                                null); }
    }


    public String nextText() {
        return null;
    }


    public int nextTag() {
        return 0;
    }


    private Object primitiveDecode(String typeNs, String typeName) {
        Object result = null;
        try {
            if (XasUtil.XSD_NAMESPACE.equals(typeNs) && typeName != null) {
                if (typeName.equals("boolean")) {
                    int value = readTypedToken();
                    if (value == 't') {
                        result = Boolean.valueOf(true);
                    } else if (value == 'f') {
                        result = Boolean.valueOf(false);
                    }
                } else if (typeName.equals("int")) {
                    result = new Integer(readTypedCompressedInt());
                } else if (typeName.equals("string")) {
                    result = readTypedData();
                } else if (typeName.equals("dateTime")) {
                    long value = 0;
                    for (int i = 0; i < 8; i++) {
                        value <<= 8;
                        value |= readTypedToken();
                    }
                    Date d = new Date(value);
                    Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                    c.setTime(d);
                    result = c;
                } else if (typeName.equals("hexBinary") || typeName.equals("base64Binary")) {
                    result = readTypedData().getBytes("ISO-8859-1");
                } else if (typeName.equals("long")) {
                    long value = 0;
                    for (int i = 0; i < 8; i++) {
                        value <<= 8;
                        value |= readTypedToken();
                    }
                    result = new Long(value);
                } else if (typeName.equals("short")) {
                    short value = 0;
                    for (int i = 0; i < 2; i++) {
                        value <<= 8;
                        value |= readTypedToken();
                    }
                    result = new Short(value);
                } else if (typeName.equals("byte")) {
                    result = new Byte((byte) readTypedToken());
                }
            }
        } catch (IOException ex) {
            /*
             * At least we do this much
             */
            ex.printStackTrace();
        }
        return result;
    }


    public Object decode(String typeNs, String typeName, XmlReader reader, EventList attributes) {
        Object result = null;
        // System.out.println("decode(" + typeNs + "," + typeName + "," + reader
        // + "," + attributes + ")");
        if (XasUtil.XSD_NAMESPACE.equals(typeNs) && typeName != null) {
            Event ev = reader.advance();
            if (ev.getType() == Event.CONTENT) {
                String content = (String) ev.getValue();
                if (content != null) {
                    typedData = content;
                    offset = 0;
                    result = primitiveDecode(typeNs, typeName);
                }
            } else {
                reader.backup();
            }
        }
        return result;
    }

}
