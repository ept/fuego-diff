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
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;

import fc.util.Measurer;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.FragmentItem;
import fc.xml.xas.Item;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.MutableFragmentPointer;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;
import fc.xml.xas.XasUtil;
import fc.xml.xas.typing.ParsedPrimitive;

public class EncryptedKey extends FragmentItem {

    public static final int KEY = 0x2904;

    private CipherParameters publicParam;
    private AsymmetricBlockCipher cipher;
    private StartTag methodStart;
    private StartTag cdataStart;
    private StartTag cvalueStart;
    private StartTag refStart;
    private ParsedPrimitive data = null;
    private List<XencDataReference> references;

    public EncryptedKey (CipherParameters publicParam, StartTag context) {
	this(publicParam, context, SecUtil.RSA_CIPHER);
    }

    public EncryptedKey (CipherParameters publicParam, StartTag context,
	    String cipherType) {
	super(KEY, 1);
	this.publicParam = publicParam;
	this.cipher = SecUtil.getPublicCipher(cipherType);
	Verifier.checkNotNull(this.cipher);
	firstItem = new StartTag(SecUtil.XENC_KEY_NAME, context);
	((StartTag) firstItem).ensurePrefix(SecUtil.XENC_NS, "xenc");
	methodStart =
	    new StartTag(SecUtil.XENC_METHOD_NAME, (StartTag) firstItem);
	methodStart.addAttribute(SecUtil.ALGO_ATT_NAME, cipherType);
	cdataStart =
	    new StartTag(SecUtil.XENC_CDATA_NAME, (StartTag) firstItem);
	cvalueStart = new StartTag(SecUtil.XENC_CVALUE_NAME, cdataStart);
	refStart =
	    new StartTag(SecUtil.XENC_REFLIST_NAME, (StartTag) firstItem);
	references = new ArrayList<XencDataReference>();
    }

    public void addDataReference (MutableFragmentPointer pointer, int start,
	    int length) {
	addDataReference(pointer, start, length, false);
    }

    public void addDataReference (MutableFragmentPointer pointer, int start,
	    int length, boolean isGzip) {
	Verifier.checkNull(data);
	if (Log.isEnabled(Log.DEBUG)) {
	    Log.debug("addDataReference(" + pointer + "," + start + ","
		+ length + "," + isGzip + ")");
	}
	XencDataReference ref = new XencDataReference(refStart);
	ref.setContent(pointer, start, length, isGzip);
	references.add(ref);
    }

    public void setKey (byte[] key) throws IOException {
	cipher.init(true, publicParam);
	try {
	    Object token = Measurer.get(Measurer.TIMING).start();
	    data =
		new ParsedPrimitive(XasUtil.BASE64_BINARY_TYPE, cipher
		    .processBlock(key, 0, key.length));
	    Measurer.get(Measurer.TIMING).finish(token, "Key encryption");
	} catch (InvalidCipherTextException ex) {
	    Util.throwWrapped(new IOException(ex.getMessage()), ex);
	}
	for (XencDataReference ref : references) {
	    ref.setKey(key);
	}
    }

    public void appendTo (ItemTarget target) throws IOException {
	if (data != null) {
	    target.append(firstItem);
	    target.append(methodStart);
	    target.append(SecUtil.XENC_METHOD_END);
	    target.append(cdataStart);
	    target.append(cvalueStart);
	    target.append(data);
	    target.append(SecUtil.XENC_CVALUE_END);
	    target.append(SecUtil.XENC_CDATA_END);
	    target.append(refStart);
	    for (Item item : references) {
		target.append(item);
	    }
	    target.append(SecUtil.XENC_REFLIST_END);
	    target.append(SecUtil.XENC_KEY_END);
	}
    }

}

// arch-tag: 0bb72ed1-4aa2-4c58-9feb-5b8a04201dd9
