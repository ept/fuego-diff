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
import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.io.DigestOutputStream;

import fc.util.Measurer;
import fc.util.Util;
import fc.xml.xas.FormatFactory;
import fc.xml.xas.FragmentItem;
import fc.xml.xas.Item;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.MutableFragmentPointer;
import fc.xml.xas.SerializableItem;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.StartTag;
import fc.xml.xas.XasFragment;
import fc.xml.xas.XasUtil;
import fc.xml.xas.typing.ParsedPrimitive;

public class DsSignature extends FragmentItem implements SerializableItem {

    public static final int SIGNATURE = 0x2902;

    private CipherParameters privateParam;
    private StartTag infoStart;
    private StartTag sigMethodStart;
    private StartTag signatureStart;
    private SignatureItem signature = null;
    private String cipherType;
    private List<Item> infoFragment;
    private boolean hasDigests = false;

    public DsSignature (CipherParameters privateParam, StartTag context,
	    String cipherType) {
	super(SIGNATURE, 1);
	this.privateParam = privateParam;
	this.cipherType = cipherType;
	firstItem = new StartTag(SecUtil.DS_SIGNATURE_NAME, context);
	((StartTag) firstItem).ensurePrefix(SecUtil.DS_NS, "ds");
	infoStart = new StartTag(SecUtil.DS_SIGINFO_NAME, (StartTag) firstItem);
	sigMethodStart = new StartTag(SecUtil.DS_SIGMETHOD_NAME, infoStart);
	sigMethodStart.addAttribute(SecUtil.ALGO_ATT_NAME, cipherType);
	signatureStart =
	    new StartTag(SecUtil.DS_SIGVALUE_NAME, (StartTag) firstItem);
	infoFragment = new ArrayList<Item>();
	infoFragment.add(infoStart);
	infoFragment.add(sigMethodStart);
	infoFragment.add(SecUtil.DS_SIGMETHOD_END);
    }

    public void addSignature (MutableFragmentPointer pointer, int start,
	    int length, String digestType) {
	infoFragment.add(new DsReference(pointer, start, length, infoStart,
	    digestType));
	hasDigests = true;
    }

    private void commit () {
	infoFragment.add(SecUtil.DS_SIGINFO_END);
	signature = new SignatureItem(privateParam, cipherType);
	XasFragment frag = new XasFragment(infoFragment, infoStart);
	signature.setContent(frag.pointer(), 0, frag.length());
    }

    public void appendTo (ItemTarget target) throws IOException {
	if (hasDigests) {
	    if (signature == null) {
		commit();
	    }
	    target.append(firstItem);
	    for (Item item : infoFragment) {
		target.append(item);
	    }
	    target.append(signatureStart);
	    target.append(signature);
	    target.append(SecUtil.DS_SIGVALUE_END);
	    target.append(SecUtil.DS_SIGNATURE_END);
	}
    }

    public void serialize (String type, SerializerTarget target)
	    throws IOException {
	if (hasDigests) {
	    if (signature == null) {
		commit();
	    }
	    target.append(firstItem);
	    SerializerTarget infoTarget = target;
	    String encoding = target.getEncoding();
	    if (encoding.equals("UTF-8")) {
		infoTarget =
		    new SignatureTarget(type, target, privateParam, cipherType);
	    }
	    for (Item item : infoFragment) {
		infoTarget.append(item);
	    }
	    target.append(signatureStart);
	    if (encoding.equals("UTF-8")) {
		target.append(((SignatureTarget) infoTarget).getValue());
	    } else {
		target.append(signature);
	    }
	    target.append(SecUtil.DS_SIGVALUE_END);
	    target.append(SecUtil.DS_SIGNATURE_END);
	}
    }

    public void setFragmentContent (List<Item> items) {
	throw new UnsupportedOperationException("Arbitrary setting of "
	    + "signature element content" + " not allowed");
    }

    private static class SignatureTarget implements SerializerTarget {

	private SerializerTarget target;
	private SerializerTarget digestTarget;
	private AsymmetricBlockCipher cipher;
	private Digest digest;
	private CipherParameters privateParam;

	public SignatureTarget (String type, SerializerTarget target,
		CipherParameters privateParam, String cipherType)
		throws IOException {
	    this.target = target;
	    this.cipher = SecUtil.getSignature(cipherType);
	    if (cipher == null) {
		throw new IllegalArgumentException("Signature type "
		    + cipherType + " unrecognized");
	    }
	    this.digest = SecUtil.getDigestFromSignature(cipherType);
	    if (digest == null) {
		throw new IllegalArgumentException(
		    "No digest available for cipher " + cipherType);
	    }
	    this.privateParam = privateParam;
	    ByteArrayOutputStream bout = new ByteArrayOutputStream();
	    DigestOutputStream dout = new DigestOutputStream(bout, digest);
	    FormatFactory factory = XasUtil.getFactory(type);
	    if (factory == null) {
		throw new IOException("Type " + type
		    + " not understood for signature computation");
	    }
	    digestTarget = factory.createCanonicalTarget(dout);
	}

	public StartTag getContext () {
	    return target.getContext();
	}

	public String getEncoding () {
	    return target.getEncoding();
	}

	public OutputStream getOutputStream () {
	    return target.getOutputStream();
	}

	public void append (Item item) throws IOException {
	    target.append(item);
	    digestTarget.append(item);
	}

	public void flush () throws IOException {
	    target.flush();
	    digestTarget.flush();
	}

	public ParsedPrimitive getValue () throws IOException {
	    ParsedPrimitive value = null;
	    digestTarget.flush();
	    byte[] bytes = new byte[digest.getDigestSize()];
	    digest.doFinal(bytes, 0);
	    Object token = Measurer.get(Measurer.TIMING).start();
	    cipher.init(true, privateParam);
	    try {
		value =
		    new ParsedPrimitive(XasUtil.BASE64_BINARY_TYPE, cipher
			.processBlock(bytes, 0, bytes.length));
	    } catch (InvalidCipherTextException ex) {
		Util.throwWrapped(new IOException(ex.getMessage()), ex);
	    }
	    Measurer.get(Measurer.TIMING).finish(token,
		"Signature value computation");
	    return value;
	}

    }

}

// arch-tag: b4474e6b-eee2-4d0a-bffa-8d222bad7fbe
