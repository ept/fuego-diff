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

package fc.xml.xas.security;

import java.io.IOException;

import fc.xml.xas.Item;
import fc.xml.xas.MutableFragmentPointer;
import fc.xml.xas.SerializableItem;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;

class XencDataReference extends Item implements SerializableItem {

    public static final int DATA_REFERENCE = 0x2905;

    private StartTag firstItem;
    private EncryptedData data;
    private MutableFragmentPointer pointer;
    private int start;
    private String uri;

    public XencDataReference (StartTag context) {
	super(DATA_REFERENCE);
	firstItem = new StartTag(SecUtil.XENC_DATAREF_NAME, context);
    }

    public void setKey (byte[] key) {
	data.setKey(key);
    }

    public void setContent (MutableFragmentPointer pointer, int start,
	    int length, boolean isGzip) {
	// XXX - has to be converted to something else
	pointer.setPosition(start);
	uri = "#Enc-" + SecUtil.nextId();
	firstItem.addAttribute(SecUtil.URI_ATT_NAME, uri);
	data = new EncryptedData(uri.substring(1), SecUtil.AES_128_CIPHER,
	    pointer.getContext());
	data.setContent(pointer, start, length);
	data.setGzip(isGzip);
	this.pointer = pointer;
	this.start = start;
    }

    public void serialize (String type, SerializerTarget target)
	    throws IOException {
	Verifier.checkNotNull(uri);
	pointer.setPosition(start);
	pointer.set(data);
	target.append(firstItem);
	target.append(SecUtil.XENC_DATAREF_END);
    }

}

// arch-tag: f2adf1bb-1078-42eb-969f-2776c11ced74
