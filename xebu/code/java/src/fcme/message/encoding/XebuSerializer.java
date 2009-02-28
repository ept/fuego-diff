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

package fcme.message.encoding;

import java.io.Writer;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.util.Calendar;

import org.xmlpull.v1.XmlSerializer;

import fuegocore.message.encoding.XebuConstants;
import fuegocore.message.encoding.XebuUtil;
import fuegocore.util.Util;
import fuegocore.util.xas.Event;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.ContentEncoder;
import fuegocore.util.xas.XasUtil;

/**
 * A serializer for Xebu documents.  This class implements the output
 * of Xebu documents as described in the Xebu specification.  The
 * serialization interface is {@link TypedXmlSerializer}, an extension
 * to XmlPull's <code>XmlSerializer</code>.  The serializer implements
 * tokenization of items and content of the Xebu format, each of which
 * can be turned on or off with the use of {@link #setFeature}.
 */
public class XebuSerializer implements TypedXmlSerializer, ContentEncoder {

    private boolean cacheItem = true;
    private boolean cacheContent = false;

    private Writer writer;
    private StringBuffer buffer = new StringBuffer();
    private int depth = 0;
    private int elemDepth = 1;
    private boolean inProgress = false;
    private boolean writingAttributes = false;

    private Object[] namespaceStack = new Object[8];
    private int[] namespaceCounts = new int[4];
    private Object[] nameStack = new Object[16];

    private ContentEncoder encoder = this;

    private OutCache[] caches;

    private void put (char c) throws IOException {
	buffer.append(c);
    }

    private void put (int i) throws IOException {
	put((char) i);
    }

    private void put (char[] c) {
	put(c, 0, c.length);
    }

    private void put (char[] c, int offset, int length) {
	buffer.append(c, offset, length);
    }

    private void put (String s) throws IOException {
	buffer.append(s);
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
    }

    private int insertIntoCache (int cacheIndex, Object key) {
	return caches[cacheIndex].insert(key);
    }

    private void checkInProgress () {
	if (inProgress) {
	    throw new IllegalStateException("Requested operation not "
					    + "permitted while processing "
					    + "a document");
	}
    }

    private void putContent (String content) throws IOException {
	int token = XebuConstants.DATA_START;
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

    XebuSerializer (OutCache[] caches) {
	this.caches = caches;
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
	}
	return result;
    }

    public void setProperty (String name, Object value) {
	if (value == null) {
	    throw new IllegalArgumentException("Property must be non-null");
	}
	if (XebuConstants.PROPERTY_INITIAL_CACHES.equals(name)) {
	    checkInProgress();
	    if (!cacheItem) {
		throw new IllegalStateException("Initial caches may not be "
						+ "set with item caching off");
	    }
	    if (!(value instanceof OutCache[])) {
		throw new IllegalArgumentException("Value " + value + " is "
						   + "not a valid initial "
						   + "cache");
	    }
	    caches = (OutCache[]) value;
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
	put(XebuConstants.DOCUMENT | flag);
    }

    public void endDocument () throws IOException {
	flush();
	inProgress = false;
    }

    public void setPrefix (String prefix, String namespace)
	throws IOException {
	putAttributeEnd();
	int current = namespaceCounts[depth + 1];
	namespaceStack = Util.ensureCapacity(namespaceStack,
					     2 * (current + 1));
	namespaceStack[2 * current] = namespace;
	namespaceStack[2 * current + 1] = prefix;
	namespaceCounts[depth + 1] += 1;
	putName(namespace, prefix, XebuConstants.NAMESPACE_START);
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
	increaseDepth(namespace, name);
	putName(namespace, name, XebuConstants.ELEMENT_START);
	writingAttributes = true;
	return this;
    }

    public XmlSerializer attribute (String namespace, String name,
				    String value)
	throws IOException {
	putValue(namespace, name, value, XebuConstants.ATTRIBUTE);
	writingAttributes = true;
	return this;
    }

    public XmlSerializer endTag (String namespace, String name)
	throws IOException {
	putAttributeEnd();
	put(XebuConstants.ELEMENT_END);
	decreaseDepth();
	return this;
    }

    public XmlSerializer text (String text) throws IOException {
	//System.out.println("Text called: " + text);
	putAttributeEnd();
	putContent(text);
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
	if (encoder == null
	    || !encoder.encode(content, namespace, name, this)) {
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
	int value = caches[XebuConstants.CONTENT_INDEX].fetch(text);
	if (value >= 0) {
	    put(XebuConstants.SPECIAL | XebuConstants.FLAG_ENTITY | 0x80);
	    put(value);
	} else {
	    put(XebuConstants.SPECIAL | XebuConstants.FLAG_ENTITY);
	    if (cacheItem) {
		value = insertIntoCache(XebuConstants.CONTENT_INDEX, text);
		put(value);
	    }
	    putDataEscape(text);
	}
    }

    public void processingInstruction (String text) throws IOException {
	if (text == null || text.length() == 0) {
	    throw new IllegalArgumentException("Processing instruction "
					       + "requires text");
	}
	putAttributeEnd();
	int value = caches[XebuConstants.CONTENT_INDEX].fetch(text);
	if (value >= 0) {
	    put(XebuConstants.SPECIAL | XebuConstants.FLAG_PI | 0x80);
	    put(value);
	} else {
	    put(XebuConstants.SPECIAL | XebuConstants.FLAG_PI);
	    if (cacheContent) {
		value = insertIntoCache(XebuConstants.CONTENT_INDEX, text);
		put(value);
	    }
	    putData(text);
	}
    }

    public void comment (String text) throws IOException {
	if (text == null) {
	    throw new IllegalArgumentException("Comment must be non-null");
	}
	putAttributeEnd();
	put(XebuConstants.SPECIAL | XebuConstants.FLAG_COMMENT);
	putData(text);
    }

    public void docdecl (String text) {
    }

    public void ignorableWhitespace (String text) throws IOException {
	this.text(text);
    }

    public void flush () throws IOException {
	if (writer != null) {
	    writer.write(buffer.toString());
	    writer.flush();
	    buffer.setLength(0);
	}
    }

    private String[] typeNames = { "boolean", "int", "string", "dateTime",
				   "hexBinary", "base64Binary", "long",
				   "short", "byte" };

    private int indexRecognized (String namespace, String name) {
	if (Util.equals(namespace, XasUtil.XSD_NAMESPACE)) {
	    for (int i = 0; i < typeNames.length; i++) {
		if (Util.equals(name, typeNames[i])) {
		    return i;
		}
	    }
	}
	return -1;
    }

    public boolean encode (Object o, String namespace, String name,
			   TypedXmlSerializer ser)
	throws IOException {
	//System.out.println("Primitive encode called:\n" + o + " of type {"
	//+ namespace + "}" + name);
	//System.out.println("Arg=" + ser + ", This=" + this);
	if (ser == this) {
	    int index = indexRecognized(namespace, name);
	    if (index >= 0) {
		String prefix = ser.getPrefix(namespace, false);
		ser.attribute(XasUtil.XSI_NAMESPACE, "type",
			      prefix + ":" + name);
		putAttributeEnd();
		put(XebuConstants.TYPED_DATA);
		switch (index) {
		case 0: {
		    Boolean b = (Boolean) o;
		    if (b != null) {
			put(b.booleanValue() ? 't' : 'f');
		    }
		    return true;
		}
		case 1: {
		    Integer i = (Integer) o;
		    if (i != null) {
			put(XebuUtil.putCompressedInt(i.intValue()));
		    }
		    return true;
		}
		case 2: {
		    String s = (String) o;
		    if (s != null) {
			put(XebuUtil.putCompressedInt(s.length()));
			put(s);
		    }
		    return true;
		}
		case 3: {
		    Calendar c = (Calendar) o;
		    if (c != null) {
			put(XebuUtil.putNormalLong(c.getTime().getTime()));
		    }
		    return true;
		}
		case 4:
		case 5: {
		    byte[] b = (byte[]) o;
		    if (b != null) {
			put(XebuUtil.putCompressedInt(b.length));
			put(new String(b, "ISO-8859-1"));
		    }
		    return true;
		}
		case 6: {
		    Long l = (Long) o;
		    if (l != null) {
			put(XebuUtil.putNormalLong(l.longValue()));
		    }
		    return true;
		}
		case 7: {
		    Short s = (Short) o;
		    if (s != null) {
			put(XebuUtil.putNormalShort(s.shortValue()));
		    }
		    return true;
		}
		case 8: {
		    Byte b = (Byte) o;
		    if (b != null) {
			put(b.byteValue());
		    }
		    return true;
		}
		}
	    }
	}
	//System.out.println("Primitive returning " + result);
	return false;
    }

}
