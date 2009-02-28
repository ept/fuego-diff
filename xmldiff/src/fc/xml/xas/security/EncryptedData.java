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
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import fc.util.Measurer;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.FormatFactory;
import fc.xml.xas.FragmentItem;
import fc.xml.xas.MutableFragmentPointer;
import fc.xml.xas.SerializableItem;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;
import fc.xml.xas.XasFragment;
import fc.xml.xas.XasUtil;
import fc.xml.xas.typing.ParsedPrimitive;

public class EncryptedData extends FragmentItem implements SerializableItem {

    public static final int DATA = 0x2906;

    private byte[] key;
    private BufferedBlockCipher cipher;
    private byte[] iv;
    private XasFragment fragment;
    private StartTag firstItem;
    private StartTag cdataStart;
    private StartTag cvalueStart;
    private StartTag methodStart;
    private ParsedPrimitive data = null;
    private String dataType = null;
    private boolean isGzip = false;
    private boolean savedGzip = false;

    public EncryptedData (String id, String cipherType, StartTag context) {
	super(DATA, 1);
	this.cipher = SecUtil.getCipher(cipherType);
	Verifier.checkNotNull(this.cipher);
	iv = new byte[cipher.getBlockSize()];
	firstItem = new StartTag(SecUtil.XENC_DATA_NAME, context);
	((StartTag) firstItem).ensurePrefix(SecUtil.XENC_NS, "xenc");
	firstItem.addAttribute(SecUtil.ID_ATT_NAME, id);
	firstItem
	    .addAttribute(SecUtil.TYPE_ATT_NAME, SecUtil.XENC_ELEMENT_TYPE);
	methodStart = new StartTag(SecUtil.XENC_METHOD_NAME, firstItem);
	methodStart.addAttribute(SecUtil.ALGO_ATT_NAME, cipherType);
	cdataStart = new StartTag(SecUtil.XENC_CDATA_NAME, firstItem);
	cvalueStart = new StartTag(SecUtil.XENC_CVALUE_NAME, cdataStart);
    }

    public void setContent (MutableFragmentPointer pointer, int start,
	    int length) {
	// pointer.setPosition(start, null);
	this.fragment = pointer.subFragment(length);
    }

    public void setKey (byte[] key) {
	if (Log.isEnabled(Log.TRACE)) {
	    Log
		.log("setKey(" + java.util.Arrays.toString(key) + ")",
		    Log.TRACE);
	}
	Verifier.checkNotNull(key);
	this.key = key;
    }

    public void setGzip (boolean isGzip) {
	this.isGzip = isGzip;
    }

    private void commit (String type) throws IOException {
	Object token = Measurer.get(Measurer.TIMING).start();
	ByteArrayOutputStream bout = new ByteArrayOutputStream();
	OutputStream os = bout;
	if (isGzip) {
	    os = new GZIPOutputStream(os);
	}
	FormatFactory factory = XasUtil.getFactory(type);
	if (factory == null) {
	    throw new IOException("Type " + type
		+ " not understood for encryption");
	}
	SerializerTarget target = factory.createTarget(os, "UTF-8");
	fragment.appendTo(target);
	target.flush();
	if (isGzip) {
	    os.close();
	}
	Measurer.get(Measurer.TIMING).finish(token,
	    "Serializing for symmetric encryption");
	token = Measurer.get(Measurer.TIMING).start();
	byte[] plainText = bout.toByteArray();
	if (Log.isEnabled(Log.DEBUG)) {
	    Log.debug("Encrypting bytes", Util.toPrintable(plainText));
	}
	SecUtil.getRandom().nextBytes(iv);
	CipherParameters params = new ParametersWithIV(new KeyParameter(key),
	    iv);
	cipher.init(true, params);
	int size = cipher.getOutputSize(plainText.length);
	byte[] cipherText = new byte[size + iv.length];
	System.arraycopy(iv, 0, cipherText, 0, iv.length);
	int off = cipher.processBytes(plainText, 0, plainText.length,
	    cipherText, iv.length);
	try {
	    int fin = cipher.doFinal(cipherText, off + iv.length);
	    if (off + fin != size) {
		throw new IOException("Real output amount " + (off + fin)
		    + " different from expected " + size);
	    }
	} catch (InvalidCipherTextException ex) {
	    Util.throwWrapped(new IOException(ex.getMessage()), ex);
	}
	Measurer.get(Measurer.TIMING)
	    .finish(token, "Symmetric encryption time");
	data = new ParsedPrimitive(XasUtil.BASE64_BINARY_TYPE, cipherText);
	firstItem.setAttribute(SecUtil.MIMETYPE_ATT_NAME, type);
	if (isGzip) {
	    firstItem.setAttribute(SecUtil.CONTENT_ENCODING_ATT_NAME,
		SecUtil.GZIP_ENCODING_NAME);
	} else {
	    firstItem.removeAttribute(SecUtil.CONTENT_ENCODING_ATT_NAME);
	}
	dataType = type;
	savedGzip = isGzip;
    }

    public void serialize (String type, SerializerTarget target)
	    throws IOException {
	if (fragment != null) {
	    if (data == null || savedGzip != isGzip || !dataType.equals(type)) {
		commit(type);
	    }
	    target.append(firstItem);
	    target.append(methodStart);
	    target.append(SecUtil.XENC_METHOD_END);
	    target.append(cdataStart);
	    target.append(cvalueStart);
	    target.append(data);
	    target.append(SecUtil.XENC_CVALUE_END);
	    target.append(SecUtil.XENC_CDATA_END);
	    target.append(SecUtil.XENC_DATA_END);
	}
    }

}

// arch-tag: 54ba486e-269d-4c86-86f2-dd8c75b9d21d
