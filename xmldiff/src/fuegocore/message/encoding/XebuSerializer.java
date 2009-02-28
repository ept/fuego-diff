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

package fuegocore.message.encoding;

import java.io.Writer;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.Calendar;

import org.xmlpull.v1.XmlSerializer;

import fuegocore.util.Util;
import fuegocore.util.xas.Event;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.XasUtil;

/**
 * A serializer for Xebu documents.  This class implements the output
 * of Xebu documents as described in the Xebu specification.  The
 * parsing interface is {@link TypedXmlSerializer}, an extension to
 * XmlPull's <code>XmlSerializer</code>.  The parser implements all
 * caching features of the Xebu format, each of which can be turned on
 * or off with the use of {@link #setFeature}.
 */
public class XebuSerializer implements TypedXmlSerializer, ContentEncoder {

    private boolean cacheItem = true;
    private boolean cacheContent = false;
    private boolean cacheSequence = false;

    private Writer writer;
    private StringBuffer buffer = new StringBuffer();
    private StringBuffer typedData = new StringBuffer();
    private boolean isMultiData = false;
    private int depth = 0;
    private int elemDepth = 1;
    private boolean inProgress = false;
    private boolean writingAttributes = false;
    private boolean writingContent = false;
    private Event waitingContent = null;

    private Object[] namespaceStack = new Object[8];
    private int[] namespaceCounts = new int[4];
    private Object[] nameStack = new Object[16];
    private int[] indexStack = new int[8];
    private int validIndex = 0;

    private Object[] tentativeKeys = new Object[8];
    private int[] tentativeValues = new int[24];
    private int tsp = 0;

    private ContentEncoder encoder = this;
    private EoaMachine eoa;

    private OutCache[] caches;
    private SequenceCache sequenceCache;
    private int sequenceSize = 0;
    private boolean isPrimitive = false;

    private boolean isAcceptable (char c) {
	return c < 0x100;
    }

    private boolean isAcceptable (char[] c, int offset, int length) {
	for (int i = 0; i < length; i++) {
	    if (!isAcceptable(c[offset + i])) {
		return false;
	    }
	}
	return true;
    }

    private void put (char c) throws IOException {
	//assert isAcceptable(c) : "Invalid character value: " + (int) c;
	buffer.append(c);
    }

    private void put (int i) throws IOException {
	//assert 0 <= i && i < 0x100 : "Invalid int value: " + i;
	put((char) i);
    }

    private void put (char[] c, int offset, int length) {
	//assert isAcceptable(c, offset, length) : "Invalid characters in "
	//+ new String(c, offset, length);
	buffer.append(c, offset, length);
    }

    private void put (String s) throws IOException {
	//assert isAcceptable(s.toCharArray(), 0, s.length()) : "Invalid "
	//+ "characters in " + s;
	buffer.append(s);
    }

    private void addToSequence (Event ev) {
	//System.out.println("Adding to cache sequence " + ev);
	sequenceCache.next(ev);
	sequenceSize++;
    }

    private void increaseDepth (String namespace, String name) {
	nameStack = Util.ensureCapacity(nameStack, 2 * (depth + 1));
	nameStack[2 * depth] = namespace;
	nameStack[2 * depth + 1] = name;
	depth += 1;
	namespaceCounts = Util.ensureCapacity(namespaceCounts, depth + 2);
	namespaceCounts[depth + 1] = namespaceCounts[depth];
    }

    private void decreaseDepth () {
	depth -= 1;
	if (depth <= elemDepth) {
	    for (int i = 0; i < caches.length; i++) {
		caches[i].tentativeCommit();
	    }
	    validIndex = sequenceSize = 0;
	}
    }

    private int insertIntoCache (int cacheIndex, Object key) {
	int value;
	if (cacheSequence && depth > elemDepth) {
	    value = caches[cacheIndex].tentativeInsert(key);
	    tentativeKeys = Util.ensureCapacity(tentativeKeys, tsp + 1);
	    tentativeKeys[tsp] = key;
	    tentativeValues = Util.ensureCapacity(tentativeValues,
						  3 * (tsp + 1));
	    tentativeValues[3 * tsp] = cacheIndex;
	    tentativeValues[3 * tsp + 1] = buffer.length();
	    tentativeValues[3 * tsp + 2] = value;
	    tsp += 1;
	} else {
	    value = caches[cacheIndex].insert(key);
	}
	return value;
    }

    private void checkInProgress () {
	if (inProgress) {
	    throw new IllegalStateException("Requested operation not "
					    + "permitted while processing "
					    + "a document");
	}
    }

    private void putContent (String content, int flag) throws IOException {
	int token = XebuConstants.DATA_START | flag;
	int value = caches[XebuConstants.CONTENT_INDEX].fetch(content);
	if (value >= 0) {
	    put(token | XebuConstants.VALUE_FETCH);
	    put(value);
	} else if (cacheContent) {
	    put(token);
	    value = insertIntoCache(XebuConstants.CONTENT_INDEX, content);
	    put(value);
	    putData(content);
	} else {
	    put(token);
	    putData(content);
	}
    }

    private void putAttributeEnd () throws IOException {
	if (writingAttributes) {
	    put(XebuConstants.ATTRIBUTE_END);
	    writingAttributes = false;
	}
    }

    private void putContentEnd () throws IOException {
	if (writingContent) {
	    put(XebuConstants.ATTRIBUTE_END);
	    writingContent = false;
	}
    }

    private void putWaitingContent (boolean isContent) throws IOException {
	if (waitingContent != null) {
	    int flag = 0;
	    if (isContent) {
		flag = XebuConstants.COALESCE_FLAG;
	    }
	    putContent((String) waitingContent.getValue(), flag);
	    waitingContent = null;
	    writingContent = isContent;
	}
    }

    private void putData (String data) throws IOException {
	char[] len = XebuUtil.putCompressedInt(data.length());
	put(len, 0, len.length);
	put(data);
    }

    private void putDataEscape (String data) throws IOException {
	int len = data.length();
	if (len < 256) {
	    put(len);
	    put(data);
	} else {
	    throw new IOException("Name " + data + " too long");
	}
    }

    private void putNamespace (String namespace, int token)
	throws IOException {
	int value = caches[XebuConstants.NAMESPACE_INDEX].fetch(namespace);
	if (value >= 0) {
	    put(token | XebuConstants.NAMESPACE_FETCH);
	    put(value);
	} else {
	    put(token);
	    if (cacheItem) {
		value = insertIntoCache(XebuConstants.NAMESPACE_INDEX,
					namespace);
		put(value);
	    }
	    putDataEscape(namespace);
	}
    }

    private void putName (String namespace, String name, int token)
	throws IOException {
	Event ev = Event.createStartElement(namespace, name);
	int value = caches[XebuConstants.NAME_INDEX].fetch(ev);
	if (value >= 0) {
	    put(token | XebuConstants.NAME_FETCH);
	    put(value);
	} else {
	    putNamespace(namespace, token);
	    if (cacheItem) {
		value = insertIntoCache(XebuConstants.NAME_INDEX, ev);
		put(value);
	    }
	    putDataEscape(name);
	}
    }

    private void putValue (String namespace, String name, String value,
			   int token)
	throws IOException {
	Event ev = Event.createTypedContent(namespace, name, value);
	int val = caches[XebuConstants.VALUE_INDEX].fetch(ev);
	if (val >= 0) {
	    put(token | XebuConstants.VALUE_FETCH);
	    put(val);
	} else {
	    putName(namespace, name, token);
	    if (cacheContent) {
		val = insertIntoCache(XebuConstants.VALUE_INDEX, ev);
		put(val);
	    }
	    putDataEscape(value);
	}
    }

    private void putTypedData () throws IOException {
	if (typedData.length() > 0) {
	    String data = typedData.toString();
	    typedData.setLength(0);
	    int token = XebuConstants.TYPED_DATA;
	    if (isMultiData) {
		token |= XebuConstants.TYPED_MULTIDATA_FLAG;
	    }
	    /*
	    int value = caches[XebuConstants.CONTENT_INDEX].fetch(data);
	    if (value >= 0) {
		put(token | XebuConstants.VALUE_FETCH);
		put(value);
	    } else {
	    */
		put(token);
		/*
		if (cacheContent && isMultiData) {
		    value = insertIntoCache(XebuConstants.CONTENT_INDEX, data);
		    put(value);
		}
		*/
		if (isMultiData) {
		    putData(data);
		} else {
		    put(data);
		}
		/*
	    }
		*/
	    isMultiData = false;
	}
    }

    private void putEvent (Event ev) throws IOException {
	Event next = eoa.nextEvent(ev);
	if (next != null) {
	    switch (next.getType()) {
	    case Event.START_ELEMENT:
		putTypedData();
		putName(next.getNamespace(), next.getName(),
			XebuConstants.ELEMENT_START);
		writingAttributes = true;
		break;
	    case Event.ATTRIBUTE:
		putValue(next.getNamespace(), next.getName(),
			 (String) next.getValue(), XebuConstants.ATTRIBUTE);
		writingAttributes = true;
		break;
	    case Event.END_ELEMENT:
		putTypedData();
		put(XebuConstants.ELEMENT_END);
		break;
	    case Event.CONTENT: {
		if (!writingContent) {
		    waitingContent = next;
		    writingContent = true;
		} else {
		    putContent((String) next.getValue(),
			       XebuConstants.COALESCE_FLAG);
		}
		break;
	    }
	    case Event.TYPED_CONTENT:
		break;
	    case Event.NAMESPACE_PREFIX:
		putTypedData();
		putName(next.getNamespace(), (String) next.getValue(),
			XebuConstants.NAMESPACE_START);
		break;
	    case Event.COMMENT:
		putTypedData();
		put(XebuConstants.SPECIAL | XebuConstants.FLAG_COMMENT);
		putData((String) next.getValue());
		break;
	    case Event.PROCESSING_INSTRUCTION: {
		putTypedData();
		int value =
		    caches[XebuConstants.CONTENT_INDEX].fetch(next.getValue());
		if (value >= 0) {
		    put(XebuConstants.SPECIAL | XebuConstants.FLAG_PI | 0x80);
		    put(value);
		} else {
		    put(XebuConstants.SPECIAL | XebuConstants.FLAG_PI);
		    if (cacheContent) {
			value = insertIntoCache(XebuConstants.CONTENT_INDEX,
						next.getValue());
			put(value);
		    }
		    putData((String) next.getValue());
		}
		break;
	    }
	    case Event.ENTITY_REFERENCE: {
		putTypedData();
		int value =
		    caches[XebuConstants.CONTENT_INDEX].fetch(next.getName());
		if (value >= 0) {
		    put(XebuConstants.SPECIAL | XebuConstants.FLAG_ENTITY
			| 0x80);
		    put(value);
		} else {
		    put(XebuConstants.SPECIAL | XebuConstants.FLAG_ENTITY);
		    if (cacheItem) {
			value = insertIntoCache(XebuConstants.CONTENT_INDEX,
						next.getName());
			put(value);
		    }
		    putDataEscape(next.getName());
		}
		break;
	    }
	    default:
		break;
	    }
	}
    }

    XebuSerializer (OutCache[] caches) {
	this(caches, new SequenceCache());
    }

    XebuSerializer (SequenceCache sequenceCache) {
	this(new OutCache[XebuConstants.INDEX_NUMBER], sequenceCache);
	for (int i = 0; i < caches.length; i++) {
	    caches[i] = new OutCache();
	}
    }

    XebuSerializer (OutCache[] caches, SequenceCache sequenceCache) {
	this(caches, sequenceCache, new IdentityEoaMachine());
    }

    XebuSerializer (OutCache[] caches, SequenceCache sequenceCache,
		    EoaMachine eoa) {
	this.caches = caches;
	this.sequenceCache = sequenceCache;
	this.eoa = eoa;
    }

    public XebuSerializer () {
	this(new OutCache[XebuConstants.INDEX_NUMBER]);
	for (int i = 0; i < caches.length; i++) {
	    caches[i] = new OutCache();
	}
    }

    public void setFeature (String name, boolean state) {
	if (XebuConstants.FEATURE_ITEM_CACHING.equals(name)) {
	    checkInProgress();
	    cacheItem = state;
	} else if (XebuConstants.FEATURE_CONTENT_CACHING.equals(name)) {
	    checkInProgress();
	    if (state && !cacheItem) {
		throw new IllegalStateException("Property CONTENT_CACHING "
						+ "may not be set without "
						+ "ITEM_CACHING");
	    }
	    cacheContent = state;
	} else if (XebuConstants.FEATURE_SEQUENCE_CACHING.equals(name)) {
	    checkInProgress();
	    cacheSequence = state;
	} else {
	    throw new IllegalStateException("Feature " + name
					    + " not supported");
	}
    }

    public boolean getFeature (String name) {
	boolean result = false;
	if (XebuConstants.FEATURE_ITEM_CACHING.equals(name)) {
	    result = cacheItem;
	} else if (XebuConstants.FEATURE_CONTENT_CACHING.equals(name)) {
	    result = cacheContent;
	} else if (XebuConstants.FEATURE_SEQUENCE_CACHING.equals(name)) {
	    result = cacheSequence;
	}
	return result;
    }

    public void setProperty (String name, Object value) {
	if (value == null) {
	    throw new IllegalArgumentException("Property must be non-null");
	}
	if (XebuConstants.PROPERTY_INITIAL_CACHES.equals(name)) {
	    checkInProgress();
	    /*
	    if (!cacheItem) {
		throw new IllegalStateException("Initial caches may not be "
						+ "set with item caching off");
	    }
	    */
	    if (!(value instanceof OutCache[])) {
		throw new IllegalArgumentException("Value " + value + " is "
						   + "not a valid initial "
						   + "cache");
	    }
	    caches = (OutCache[]) value;
	} else if (XebuConstants.PROPERTY_INITIAL_SEQUENCE_CACHE.equals(name)) {
	    checkInProgress();
	    if (!cacheSequence) {
		throw new IllegalStateException("Initial element cache may "
						+ "not be set with element "
						+ " caching off");
	    }
	    if (!(value instanceof SequenceCache)) {
		throw new IllegalArgumentException("Value " + value + " is "
						   + "not a valid initial "
						   + "sequence cache");
	    }
	    sequenceCache = (SequenceCache) value;
	} else if (XebuConstants.PROPERTY_SEQUENCE_CACHE_DEPTH.equals(name)) {
	    checkInProgress();
	    if (!cacheSequence) {
		throw new IllegalStateException("Element caching depth may "
						+ "not be set with element "
						+ " caching off");
	    }
	    if (!(value instanceof Integer)) {
		throw new IllegalArgumentException("Element cache depth "
						   + "must be an Integer");
	    }
	    elemDepth = ((Integer) value).intValue();
	} else if (XebuConstants.PROPERTY_COA_MACHINE.equals(name)) {
	    checkInProgress();
	    if (!(value instanceof EoaMachine)) {
		throw new IllegalArgumentException("Value " + value + " is "
						   + "not a valid COA "
						   + "machine");
	    }
	    eoa = (EoaMachine) value;
	} else if (XasUtil.PROPERTY_CONTENT_CODEC.equals(name)) {
	    checkInProgress();
	    if (!(value instanceof ContentEncoder)) {
		throw new IllegalArgumentException("Value " + value + " is "
						   + "not a valid content "
						   + "encoder");
	    }
	    encoder = (ContentEncoder) value;
	} else {
	    throw new IllegalStateException("Property " + name
					    + " not supported");
	}
    }

    public Object getProperty (String name) {
	Object result = null;
	if (XebuConstants.PROPERTY_INITIAL_CACHES.equals(name)) {
	    result = caches;
	} else if (XebuConstants.PROPERTY_INITIAL_SEQUENCE_CACHE.equals(name)) {
	    result = sequenceCache;
	} else if (XebuConstants.PROPERTY_SEQUENCE_CACHE_DEPTH.equals(name)) {
	    result = new Integer(elemDepth);
	} else if (XebuConstants.PROPERTY_COA_MACHINE.equals(name)) {
	    result = eoa;
	} else if (XasUtil.PROPERTY_CONTENT_CODEC.equals(name)) {
	    result = encoder;
	}
	return result;
    }

    public void setOutput (OutputStream os, String encoding)
	throws IOException {
	if (encoding != null) {
	    setOutput(new OutputStreamWriter(os, encoding));
	} else {
	    setOutput(new OutputStreamWriter(os));
	}
    }

    public void setOutput (Writer writer) {
	this.writer = writer;
    }

    public void startDocument (String encoding, Boolean standalone)
	throws IOException {
	inProgress = true;
	int flag = 0;
	if (cacheItem) {
	    flag |= XebuConstants.FLAG_ITEM_CACHING;
	    if (cacheContent) {
		flag |= XebuConstants.FLAG_CONTENT_CACHING;
	    }
	}
	if (cacheSequence) {
	    flag |= XebuConstants.FLAG_SEQUENCE_CACHING;
	}
	put(XebuConstants.DOCUMENT | flag);
    }

    public void endDocument () throws IOException {
	flush();
	inProgress = false;
    }

    public void setPrefix (String prefix, String namespace)
	throws IOException {
	putAttributeEnd();
	putWaitingContent(false);
	putContentEnd();
	int current = namespaceCounts[depth + 1];
	namespaceStack = Util.ensureCapacity(namespaceStack,
					     2 * (current + 1));
	namespaceStack[2 * current] = namespace;
	namespaceStack[2 * current + 1] = prefix;
	namespaceCounts[depth + 1] += 1;
	Event ev = Event.createNamespacePrefix(namespace, prefix);
	putEvent(ev);
	if (cacheSequence && depth > elemDepth) {
	    addToSequence(ev);
	}
	isPrimitive = false;
    }

    public String getPrefix (String namespace, boolean generatePrefix) {
	for (int i = 2 * (namespaceCounts[depth] - 1); i >= 0; i -= 2) {
	    if (Util.equals(namespace, namespaceStack[i])) {
		return (String) namespaceStack[i + 1];
	    }
	}
	return null;
    }

    public int getDepth () {
	return depth;
    }

    public String getNamespace () {
	String result = null;
	if (depth > 0) {
	    result = (String) nameStack[2 * (depth - 1)];
	}
	return result;
    }

    public String getName () {
	String result = null;
	if (depth > 0) {
	    result = (String) nameStack[2 * depth - 1];
	}
	return result;
    }

    public XmlSerializer startTag (String namespace, String name)
	throws IOException {
	putAttributeEnd();
	putWaitingContent(false);
	putContentEnd();
	Event ev = Event.createStartElement(namespace, name);
	increaseDepth(namespace, name);
	if (cacheSequence && depth > elemDepth) {
	    sequenceCache.start();
	    indexStack = Util.ensureCapacity(indexStack, depth - elemDepth);
	    indexStack[depth - elemDepth - 1] = buffer.length();
	    addToSequence(ev);
	}
	putEvent(ev);
	isPrimitive = false;
	return this;
    }

    public XmlSerializer attribute (String namespace, String name,
				    String value)
	throws IOException {
	Event ev = Event.createAttribute(namespace, name, value);
	putEvent(ev);
	if (cacheSequence && depth > elemDepth) {
	    addToSequence(ev);
	}
	isPrimitive = false;
	return this;
    }

    public XmlSerializer endTag (String namespace, String name)
	throws IOException {
	putAttributeEnd();
	putWaitingContent(false);
	putContentEnd();
	Event ev = Event.createEndElement(namespace, name);
	putEvent(ev);
	if (cacheSequence && depth > elemDepth) {
	    int index = indexStack[depth - elemDepth - 1];
	    addToSequence(ev);
	    int len = sequenceCache.topLength();
	    int value = sequenceCache.value();
	    //System.out.println("pSC(" + ev + "): len=" + len + ",value="
	    //+ value + "\nSC=" + sequenceCache);
	    if (value != -1) {
		if (len <= sequenceSize - validIndex) {
		    sequenceCache.end();
		    buffer.setLength(index);
		    //System.out.println("SQF(" + value + ") = "
		    //+ sequenceCache.fetch(value));
		    put(XebuConstants.SEQUENCE_FETCH
			| ((value & 0xF00) >>> 4));
		    put(value & 0xFF);
		    while (tsp > 0) {
			if (tentativeValues[3 * tsp - 2] >= index) {
			    tsp -= 1;
			    int cacheIndex = tentativeValues[3 * tsp];
			    caches[cacheIndex].tentativeCancel
				(tentativeKeys[tsp],
				 tentativeValues[3 * tsp + 2]);
			} else {
			    break;
			}
		    }
		}
	    } else if (len < 0x100 && eoa.isInitialState()) {
		//assert value >= 0 && value < XebuConstants.SEQUENCE_CACHE_SIZE
		//: "Received invalid cache index: " + value;
		int end = sequenceCache.end();
		//System.out.println("SQE(" + len + ") = "
		//+ sequenceCache.fetch(end));
		put(XebuConstants.SEQUENCE_ENTRY | ((end & 0x0F00) >>> 4));
		put(end & 0xFF);
		put(len);
	    } else {
		sequenceCache.forget();
	    }
	}
	decreaseDepth();
	isPrimitive = false;
	return this;
    }

    public XmlSerializer text (String text) throws IOException {
	//System.out.println("Text called: " + text);
	putAttributeEnd();
	putWaitingContent(true);
	Event ev = Event.createContent(text);
	putEvent(ev);
	if (cacheSequence && depth > elemDepth) {
	    addToSequence(ev);
	}
	isPrimitive = false;
	return this;
    }

    public XmlSerializer text (char[] ch, int start, int length)
	throws IOException {
	return text(new String(ch, start, length));
    }

    public TypedXmlSerializer typedContent (Object content, String namespace,
					    String name)
	throws IOException {
	//System.out.println("Typed content called:\n" + content + " of type {"
	//+ namespace + "}" + name);
	boolean isSecond = typedData.length() > 0;
	isPrimitive = false;
	if (encoder != null
	    && encoder.encode(content, namespace, name, this)) {
	    if (isPrimitive) {
		Event ev = Event.createTypedContent(namespace, name, content);
		putEvent(ev);
		if (cacheSequence && depth > elemDepth) {
		    addToSequence(ev);
		}
		if (isSecond) {
		    isMultiData = true;
		}
	    }
	} else {
	    throw new IOException("Failed to encode value " + content
				  + " as type {" + namespace + "}" + name);
	}
	return this;
    }

    public void cdsect (String text) throws IOException {
	/*
	 * I'm pretty sure a CDATA section is perfectly okay to output
	 * as such in a Xebu document.  You shouldn't use CDATA
	 * anyway.
	 */
	this.text(text);
    }

    public void entityRef (String text) throws IOException {
	if (text == null || text.length() == 0) {
	    throw new IllegalArgumentException("Entity reference requires a"
					       + " name");
	}
	putAttributeEnd();
	putWaitingContent(false);
	putContentEnd();
	Event ev = Event.createEntityReference(text);
	putEvent(ev);
	if (cacheSequence && depth > elemDepth) {
	    addToSequence(ev);
	}
    }

    public void processingInstruction (String text) throws IOException {
	if (text == null || text.length() == 0) {
	    throw new IllegalArgumentException("Processing instruction "
					       + "requires text");
	}
	putAttributeEnd();
	putWaitingContent(false);
	putContentEnd();
	Event ev = Event.createProcessingInstruction(text);
	putEvent(ev);
	if (cacheSequence && depth > elemDepth) {
	    addToSequence(ev);
	}
    }

    public void comment (String text) throws IOException {
	if (text == null) {
	    throw new IllegalArgumentException("Comment must be non-null");
	}
	putAttributeEnd();
	putWaitingContent(false);
	putContentEnd();
	Event ev = Event.createComment(text);
	putEvent(ev);
	if (cacheSequence && depth > elemDepth) {
	    addToSequence(ev);
	}
    }

    public void docdecl (String text) {
    }

    public void ignorableWhitespace (String text) throws IOException {
	this.text(text);
    }

    public void flush () throws IOException {
	if (writer != null) {
	    if (cacheSequence) {
		validIndex = sequenceSize;
	    }
	    writer.write(buffer.toString());
	    writer.flush();
	    buffer.setLength(0);
	}
    }

    public boolean encode (Object o, String namespace, String name,
			   TypedXmlSerializer ser)
	throws IOException {
	boolean result = false;
	//System.out.println("Primitive encode called:\n" + o + " of type {"
	//+ namespace + "}" + name);
	//System.out.println("Arg=" + ser + ", This=" + this);
	if (ser == this) {
	    if (XasUtil.XSD_NAMESPACE.equals(namespace)) {
		if (name != null) {
		    if (name.equals("boolean")) {
			Boolean b = (Boolean) o;
			if (b != null) {
			    typedData.append(b.booleanValue() ? "t" : "f");
			    result = true;
			}
		    } else if (name.equals("int")) {
			Integer i = (Integer) o;
			if (i != null) {
			    typedData.append(XebuUtil.putCompressedInt(i.intValue()));
			    result = true;
			}
		    } else if (name.equals("string")) {
			String s = (String) o;
			if (s != null) {
			    typedData.append(XebuUtil.putCompressedInt(s.length()));
			    typedData.append(s);
			    result = true;
			}
		    } else if (name.equals("dateTime")) {
			Calendar c = (Calendar) o;
			if (c != null) {
			    typedData.append(XebuUtil.putNormalLong(c.getTime().getTime()));
			    result = true;
			}
		    } else if (name.equals("hexBinary")
			       || name.equals("base64Binary")) {
			byte[] b = (byte[]) o;
			if (b != null) {
			    typedData.append(XebuUtil.putCompressedInt(b.length));
			    typedData.append(new String(b, "ISO-8859-1"));
			    result = true;
			}
		    } else if (name.equals("long")) {
			Long l = (Long) o;
			if (l != null) {
			    typedData.append(XebuUtil.putNormalLong(l.longValue()));
			    result = true;
			}
		    } else if (name.equals("short")) {
			Short s = (Short) o;
			if (s != null) {
			    typedData.append(XebuUtil.putNormalShort(s.shortValue()));
			    result = true;
			}
		    } else if (name.equals("byte")) {
			Byte b = (Byte) o;
			if (b != null) {
			    typedData.append(new String(new byte[]
				{ b.byteValue() },
							"ISO-8859-1"));
			    result = true;
			}
		    }
		}
		if (result) {
		    String prefix = ser.getPrefix(namespace, false);
		    ser.attribute(XasUtil.XSI_NAMESPACE, "type",
				  prefix + ":" + name);
		    //System.out.println("Typed data currently: "
		    //+ Util.toPrintable(typedData.toString()));
		    isPrimitive = true;
		}
	    }
	}
	//System.out.println("Primitive returning " + result);
	return result;
    }

}
