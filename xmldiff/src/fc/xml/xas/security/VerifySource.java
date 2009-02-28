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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.io.DigestOutputStream;

import fc.util.Measurer;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.EndTag;
import fc.xml.xas.FormatFactory;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.Qname;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;
import fc.xml.xas.XasUtil;
import fc.xml.xas.typing.ParsedPrimitive;

public class VerifySource implements ItemSource {

    public static final int IN_PROGRESS = 0;
    public static final int VERIFY_SUCCESS = 1;
    public static final int VERIFY_FAILURE = 2;

    private String type;
    private ItemSource source;
    private CipherParameters publicParam;
    private LinkedList<Item> storage = new LinkedList<Item>();
    private Map<String, Target> targets = new HashMap<String, Target>();
    private List<Target> activeTargets = new ArrayList<Target>();

    public VerifySource (String type, ItemSource source) {
	this(type, source, null);
    }

    public VerifySource (String type, ItemSource source,
	    CipherParameters publicParam) {
	Verifier.checkNotNull(source);
	this.type = type;
	this.source = source;
	this.publicParam = publicParam;
    }

    public boolean isFinished () {
	for (Target t : targets.values()) {
	    if (!t.isFinished()) {
		return false;
	    }
	}
	return targets.size() == 0 || targets.containsKey("");
    }

    public int numberSuccesses () {
	int count = 0;
	for (Target t : targets.values()) {
	    if (t.isSuccess()) {
		count += 1;
	    }
	}
	return count;
    }

    public int numberFailures () {
	int count = 0;
	for (Target t : targets.values()) {
	    if (t.isFailure()) {
		count += 1;
	    }
	}
	return count;
    }

    public void setPublicParam (CipherParameters publicParam) {
	this.publicParam = publicParam;
    }

    private void addTarget (Item item) throws IOException {
	for (int i = activeTargets.size() - 1; i >= 0; i--) {
	    Target target = activeTargets.get(i);
	    target.append(item);
	}
    }

    private void addStorage (Item item) throws IOException {
	addTarget(item);
	storage.add(item);
    }

    private void processDigestValue (String id, Digest digest)
	    throws IOException {
	Item item;
	while ((item = source.next()) != null) {
	    addStorage(item);
	    if (ParsedPrimitive.isParsedPrimitive(item)) {
		ParsedPrimitive pp = (ParsedPrimitive) item;
		if (pp.getTypeName().equals(XasUtil.BASE64_BINARY_TYPE)
		    || pp.getTypeName().equals(XasUtil.HEX_BINARY_TYPE)) {
		    byte[] bytes = (byte[]) pp.getValue();
		    Target target = new Target(id, digest, bytes);
		    targets.put(id, target);
		}
	    } else if (Item.isEndTag(item)) {
		EndTag et = (EndTag) item;
		Qname name = et.getName();
		if (name.equals(SecUtil.DS_DIGEST_VALUE_NAME)) {
		    break;
		}
	    }
	}
    }

    private void processReference (StartTag firstItem) throws IOException {
	String uri = (String) firstItem.getAttributeValue(SecUtil.URI_ATT_NAME);
	if (uri == null) {
	    throw new IOException("No " + SecUtil.URI_ATT_NAME.getName()
		+ " attribute in " + firstItem);
	}
	if (uri.charAt(0) != '#') {
	    throw new IOException("URI reference " + uri
		+ " not a relative one");
	}
	uri = uri.substring(1);
	Digest digest = null;
	Item item;
	while ((item = source.next()) != null) {
	    addStorage(item);
	    if (Item.isStartTag(item)) {
		StartTag st = (StartTag) item;
		Qname name = st.getName();
		if (name.equals(SecUtil.DS_DIGEST_METHOD_NAME)) {
		    String digestName =
			(String) st.getAttributeValue(SecUtil.ALGO_ATT_NAME);
		    if (digestName == null) {
			throw new IOException("Element "
			    + SecUtil.DS_DIGEST_METHOD_NAME
			    + " did not contain an algorithm");
		    }
		    digest = SecUtil.getDigest(digestName);
		    if (digest == null) {
			throw new IOException("Digest " + digestName
			    + " not recognized");
		    }
		} else if (name.equals(SecUtil.DS_DIGEST_VALUE_NAME)) {
		    if (digest == null) {
			throw new IOException("Element "
			    + SecUtil.DS_REFERENCE_NAME + " did not contain a "
			    + SecUtil.DS_DIGEST_METHOD_NAME + " element");
		    }
		    processDigestValue(uri, digest);
		}
	    } else if (Item.isEndTag(item)) {
		EndTag et = (EndTag) item;
		Qname name = et.getName();
		if (name.equals(SecUtil.DS_REFERENCE_NAME)) {
		    break;
		}
	    }
	}
    }

    private AsymmetricBlockCipher processSiginfo (StartTag firstItem)
	    throws IOException {
	int start = storage.size();
	AsymmetricBlockCipher cipher = null;
	Item item;
	while ((item = source.next()) != null) {
	    addStorage(item);
	    if (Item.isStartTag(item)) {
		StartTag st = (StartTag) item;
		Qname name = st.getName();
		if (name.equals(SecUtil.DS_SIGMETHOD_NAME)) {
		    String cipherName =
			(String) st.getAttributeValue(SecUtil.ALGO_ATT_NAME);
		    if (cipherName == null) {
			throw new IOException(
			    "Signature algorithm not found in "
				+ SecUtil.DS_SIGMETHOD_NAME + " element");
		    }
		    Digest digest = SecUtil.getDigestFromSignature(cipherName);
		    if (digest == null) {
			throw new IOException(
			    "Digest method for signature algorithm "
				+ cipherName + " not found");
		    }
		    Target target = new Target("", digest, firstItem);
		    targets.put("", target);
		    for (Iterator<Item> it = storage.listIterator(start); it
			.hasNext();) {
			target.append(it.next());
		    }
		    activeTargets.add(target);
		    cipher = SecUtil.getSignature(cipherName);
		    if (cipher == null) {
			throw new IOException(
			    "Cipher method for signature algorithm "
				+ cipherName + " not found");
		    }
		} else if (name.equals(SecUtil.DS_REFERENCE_NAME)) {
		    processReference(st);
		}
	    } else if (Item.isEndTag(item)) {
		EndTag et = (EndTag) item;
		Qname name = et.getName();
		if (name.equals(SecUtil.DS_SIGINFO_NAME)) {
		    break;
		}
	    }
	}
	return cipher;
    }

    private void processSignatureValue (AsymmetricBlockCipher signatureCipher)
	    throws IOException {
	if (signatureCipher == null) {
	    throw new IOException("Encountered "
		+ SecUtil.DS_SIGVALUE_NAME.getName() + " element before "
		+ SecUtil.DS_SIGMETHOD_NAME);
	}
	Item item;
	while ((item = source.next()) != null) {
	    addStorage(item);
	    if (ParsedPrimitive.isParsedPrimitive(item)) {
		ParsedPrimitive pp = (ParsedPrimitive) item;
		if (pp.getTypeName().equals(XasUtil.BASE64_BINARY_TYPE)
		    || pp.getTypeName().equals(XasUtil.HEX_BINARY_TYPE)) {
		    byte[] bytes = (byte[]) pp.getValue();
		    Target target = targets.get("");
		    if (target == null) {
			throw new IOException("Encountered "
			    + SecUtil.DS_SIGVALUE_NAME.getName()
			    + " element before " + SecUtil.DS_SIGINFO_NAME);
		    }
		    Verifier.checkNotNull(publicParam);
		    Object token = Measurer.get(Measurer.TIMING).start();
		    signatureCipher.init(false, publicParam);
		    try {
			bytes =
			    signatureCipher
				.processBlock(bytes, 0, bytes.length);
		    } catch (InvalidCipherTextException ex) {
			throw (IOException) new IOException(ex.getMessage())
			    .initCause(ex);
		    }
		    Measurer.get(Measurer.TIMING).finish(token,
			"Signature verification");
		    target.setClaimedDigest(bytes);
		}
	    } else if (Item.isEndTag(item)) {
		EndTag et = (EndTag) item;
		Qname name = et.getName();
		if (name.equals(SecUtil.DS_SIGVALUE_NAME)) {
		    break;
		}
	    }
	}
    }

    private void processSignature () throws IOException {
	AsymmetricBlockCipher signatureCipher = null;
	Item item;
	while ((item = source.next()) != null) {
	    addStorage(item);
	    if (Item.isStartTag(item)) {
		StartTag st = (StartTag) item;
		Qname name = st.getName();
		if (name.equals(SecUtil.DS_SIGINFO_NAME)) {
		    signatureCipher = processSiginfo(st);
		    if (signatureCipher == null) {
			throw new IOException("Element "
			    + SecUtil.DS_SIGINFO_NAME.getName()
			    + " did not contain a signature method");
		    }
		} else if (name.equals(SecUtil.DS_SIGVALUE_NAME)) {
		    processSignatureValue(signatureCipher);
		}
	    } else if (Item.isEndTag(item)) {
		EndTag et = (EndTag) item;
		Qname name = et.getName();
		if (name.equals(SecUtil.DS_SIGNATURE_NAME)) {
		    break;
		}
	    }
	}
    }

    public Item next () throws IOException {
	if (!storage.isEmpty()) {
	    return storage.remove();
	} else {
	    Item item = source.next();
	    if (item != null) {
		addTarget(item);
		if (Item.isStartTag(item)) {
		    StartTag st = (StartTag) item;
		    Qname name = st.getName();
		    if (name.equals(SecUtil.DS_SIGNATURE_NAME)) {
			processSignature();
		    } else {
			String id =
			    (String) st.getAttributeValue(SecUtil.ID_ATT_NAME);
			if (id != null) {
			    Target target = targets.get(id);
			    if (target != null) {
				target.start(st);
				activeTargets.add(target);
			    }
			}
		    }
		}
	    }
	    return item;
	}
    }

    public String toString () {
	StringBuffer sb = new StringBuffer("VerSource(");
	sb.append(type);
	sb.append(",");
	sb.append(targets.keySet());
	sb.append(",");
	sb.append(activeTargets);
	sb.append(")");
	return sb.toString();
    }

    private class Target {

	private int state = IN_PROGRESS;

	private String id;
	private SerializerTarget target;
	private Digest digest;
	private int depth;
	private byte[] claimedDigest;
	private byte[] computedDigest;

	private void build (String id, Digest digest, StartTag firstItem)
		throws IOException {
	    this.id = id;
	    this.digest = digest;
	    initializeTarget();
	    target.append(firstItem);
	    depth = 1;
	}

	public Target (String id, Digest digest, byte[] claimedDigest) {
	    Verifier.checkNotNull(id);
	    Verifier.checkNotNull(digest);
	    Verifier.checkNotNull(claimedDigest);
	    this.id = id;
	    this.digest = digest;
	    this.claimedDigest = claimedDigest;
	}

	public Target (String id, Digest digest, StartTag firstItem)
		throws IOException {
	    Verifier.checkNotNull(digest);
	    build(id, digest, firstItem);
	}

	public Target (String id, String digestType, StartTag firstItem)
		throws IOException {
	    if (Log.isEnabled(Log.TRACE)) {
		Log.log("Target(" + id + ", " + digestType + ")", Log.TRACE);
	    }
	    Digest digest = SecUtil.getDigest(digestType);
	    if (digest == null) {
		throw new IOException("Digest " + digestType
		    + " not recognized");
	    }
	    build(id, digest, firstItem);
	}

	public void start (StartTag firstItem) throws IOException {
	    initializeTarget();
	    target.append(firstItem);
	    depth = 1;
	}

	private void finish () {
	    state =
		Arrays.equals(claimedDigest, computedDigest) ? VERIFY_SUCCESS
		    : VERIFY_FAILURE;
	}

	public void setClaimedDigest (byte[] claimedDigest) {
	    Verifier.checkNull(this.claimedDigest);
	    this.claimedDigest = claimedDigest;
	    if (computedDigest != null) {
		finish();
	    }
	}

	public boolean isFinished () {
	    return state == VERIFY_SUCCESS || state == VERIFY_FAILURE;
	}

	public boolean isSuccess () {
	    return state == VERIFY_SUCCESS;
	}

	public boolean isFailure () {
	    return state == VERIFY_FAILURE;
	}

	private void appendTarget (Item item) throws IOException {
	    if (Item.isStartTag(item)) {
		StartTag st = (StartTag) item;
		if (st.getName().getNamespace().equals(SecUtil.DS_NS)) {
		    st.removeAttribute(XasUtil.XSI_TYPE);
		}
	    }
	    target.append(item);
	}

	public void append (Item item) throws IOException {
	    if (Log.isEnabled(Log.TRACE)) {
		Log.log("append(" + item + "), id=" + id, Log.TRACE);
	    }
	    appendTarget(item);
	    if (Item.isStartTag(item)) {
		depth += 1;
	    } else if (Item.isEndTag(item)) {
		depth -= 1;
	    }
	    if (depth == 0) {
		computedDigest = getDigest();
		activeTargets.remove(this);
		if (claimedDigest != null) {
		    finish();
		}
	    }
	}

	private void initializeTarget () throws IOException {
	    DigestOutputStream dout = new DigestOutputStream(Util.SINK, digest);
	    FormatFactory factory = XasUtil.getFactory(type);
	    if (factory == null) {
		throw new IOException("Type " + type
		    + " not understood for digest verification");
	    }
	    target = factory.createCanonicalTarget(dout);
	}

	private byte[] getDigest () throws IOException {
	    target.flush();
	    byte[] bytes = new byte[digest.getDigestSize()];
	    digest.doFinal(bytes, 0);
	    if (Log.isEnabled(Log.DEBUG)) {
		Log.log("getDigest(), id=" + id + ", value="
		    + Util.toPrintable(bytes), Log.DEBUG);
	    }
	    return bytes;
	}

	public String toString () {
	    return id;
	}

    }

}

// arch-tag: 05d758fb-3865-41ce-82c9-692a78bc0f2d
