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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.io.DigestOutputStream;

import fc.util.Measurer;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.FormatFactory;
import fc.xml.xas.Item;
import fc.xml.xas.MutableFragmentPointer;
import fc.xml.xas.SerializableItem;
import fc.xml.xas.SerializedFragment;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;
import fc.xml.xas.XasUtil;
import fc.xml.xas.typing.ParsedPrimitive;

public class DigestItem extends Item implements SerializableItem {

    public static final int DIGEST = 0x2900;

    private MutableFragmentPointer pointer;
    private int start;
    private int length;
    private String id;
    protected Digest digest;
    protected ParsedPrimitive value;
    protected String valueType;

    private void init (String id, Digest digest) {
	this.id = id;
	this.digest = digest;
    }

    protected DigestItem (String id, int type) {
	this(id, type, SecUtil.getDigest(SecUtil.SHA_1_DIGEST));
    }

    protected DigestItem (String id, int type, Digest digest) {
	super(type);
	init(id, digest);
    }

    public DigestItem (String id) {
	this(id, DIGEST);
    }

    public DigestItem (String id, String digestType) {
	super(DIGEST);
	Digest digest = SecUtil.getDigest(digestType);
	if (digest == null) {
	    throw new IllegalArgumentException("Digest type " + digestType + " not recognized");
	}
	init(id, digest);
    }

    public void setContent (MutableFragmentPointer pointer, int start,
	    int length) {
	if (Log.isEnabled(Log.TRACE)) {
	    Log.log("setContent(" + pointer + "," + start + "," + length + ")",
		Log.TRACE);
	}
	Verifier.checkNull(this.pointer);
	Verifier.checkNotNull(pointer);
	Verifier.checkPositive(start + length);
	this.pointer = pointer;
	this.start = start;
	this.length = length;
    }

    private void fillValue (String type, String encoding) throws IOException {
	Object token = Measurer.get(Measurer.TIMING).start();
	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	DigestOutputStream dout = new DigestOutputStream(bout, digest);
	FormatFactory factory = XasUtil.getFactory(type);
	if (factory == null) {
	    throw new IOException("Type " + type
		+ " not understood for digest computation");
	}
	SerializerTarget target = factory.createCanonicalTarget(dout);
	// XXX - has to be converted to something else
	pointer.setPosition(start);
	Iterator<Item> it = pointer.iterator(length);
	Item item = it.next();
	if (id != null && id.length() > 0) {
	    if (Item.isStartTag(item)) {
		StartTag tag = (StartTag) item;
		tag.setAttribute(SecUtil.ID_ATT_NAME, id);
	    }
	}
	target.append(item);
	while (it.hasNext()) {
	    target.append(it.next());
	}
	target.flush();
	if (Log.isEnabled(Log.TRACE)) {
	    Log.log("fillValue(), value="
		+ Util.toPrintable(bout.toByteArray()), Log.TRACE);
	}
	Measurer.get(Measurer.TIMING).finish(token, "Digest computation");
	setValue();
	valueType = type;
	// pointer.setPosition(start, null);
	if (encoding.equals("UTF-8")) {
	    pointer.set(new SerializedFragment(type, encoding, bout
		.toByteArray()));
	}
    }

    protected void setValue () throws IOException {
	byte[] bytes = new byte[digest.getDigestSize()];
	digest.doFinal(bytes, 0);
	value = new ParsedPrimitive(XasUtil.BASE64_BINARY_TYPE, bytes);
    }

    public void serialize (String type, SerializerTarget target)
	    throws IOException {
	Verifier.checkNotNull(pointer);
	if (value == null || !valueType.equals(type)) {
	    fillValue(type, target.getEncoding());
	}
	target.append(value);
    }

}

// arch-tag: f940d01a-b4a2-4d60-96a5-66f6971b8f6e
