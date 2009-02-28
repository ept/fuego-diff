/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.typing;

import java.io.IOException;
import java.util.ArrayList;

import fc.util.Measurer;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.AttributeNode;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;
import fc.xml.xas.XasUtil;

public class DecodeSource implements ItemSource {

    private ItemSource source;
    private ArrayList<Item> buffer = new ArrayList<Item>();
    private int index = 0;
    private int end = 0;
    private int[] tagStack = new int[16];
    private int stackTop = -1;

    private int pop () {
	Verifier.checkNotNegative(stackTop);
	return tagStack[stackTop--];
    }

    private void push (int value) {
	tagStack = Util.ensureCapacity(tagStack, stackTop + 2);
	tagStack[ ++stackTop] = value;
    }

    private void addToBuffer (Item item) {
	int size = buffer.size();
	if (end == size) {
	    buffer.add(item);
	} else if (end < size) {
	    buffer.set(end, item);
	} else {
	    throw new IllegalStateException("Computed buffer end " + end
		+ " > buffer size " + size);
	}
	end += 1;
    }

    private void parseToBuffer (Item item) throws IOException {
	push(end);
	addToBuffer(item);
	int depth = 1;
	while (depth > 0) {
	    item = source.next();
	    if (isProperStartTag(item)) {
		parseToBuffer(item);
		continue;
	    } else if (Item.isStartTag(item)) {
		depth += 1;
	    } else if (Item.isEndTag(item)) {
		depth -= 1;
	    }
	    addToBuffer(item);
	}
	if (Log.isEnabled(Log.TRACE)) {
	    Log.trace("Stack state, top = " + stackTop, tagStack);
	    Log.trace("Buffer state, index=" + index + ",end=" + end, buffer);
	}
	int top = pop();
	StartTag st = (StartTag) buffer.get(top);
	AttributeNode an = st.getAttribute(XasUtil.XSI_TYPE);
	ParsedPrimitive pp = (ParsedPrimitive) an.getValue();
	Qname typeName = (Qname) pp.getValue();
	if (Log.isEnabled(Log.DEBUG)) {
	    Log.debug("Trying to decode type", typeName);
	}
	ValueCodec codec = Codec.getValueCodec(typeName);
	if (codec != null) {
	    Object token = Measurer.get(Measurer.TIMING).start();
	    Object result =
		codec.decode(typeName, new XasUtil.ArraySource(buffer, top + 1,
		    end - 1));
	    Measurer.get(Measurer.TIMING)
		.finish(token, "Complex type decoding");
	    if (Log.isEnabled(Log.DEBUG)) {
		Log.debug("Decoder found with result", result);
	    }
	    if (result != null) {
		buffer.set(top + 1, new TypedItem(typeName, result));
		buffer.set(top + 2, item);
		end = top + 3;
	    }
	}
    }

    private boolean isProperStartTag (Item item) {
	boolean result = false;
	if (Item.isStartTag(item)) {
	    StartTag st = (StartTag) item;
	    AttributeNode an = st.getAttribute(XasUtil.XSI_TYPE);
	    if (an != null) {
		result = true;
	    }
	}
	return result;
    }

    public DecodeSource (ItemSource source) {
	this.source = source;
    }

    public Item next () throws IOException {
	if (index < end) {
	    Item item = buffer.get(index++);
	    if (index >= end) {
		buffer.clear();
		index = end = 0;
	    }
	    if (Log.isEnabled(Log.TRACE)) {
		Log.trace("Returning buffered item", item);
	    }
	    return item;
	} else {
	    Item item = source.next();
	    if (isProperStartTag(item)) {
		parseToBuffer(item);
		index = 1;
	    }
	    if (Log.isEnabled(Log.TRACE)) {
		Log.trace("Returning normal item", item);
	    }
	    return item;
	}
    }

}

// arch-tag: eecc4e06-5396-47f7-87ee-2b536f24da80
