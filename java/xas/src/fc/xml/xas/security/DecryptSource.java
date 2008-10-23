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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import fc.util.Measurer;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.EndTag;
import fc.xml.xas.FormatFactory;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;
import fc.xml.xas.XasUtil;
import fc.xml.xas.typing.ParsedPrimitive;
import fc.xml.xas.typing.TypingUtil;

public class DecryptSource implements ItemSource {

    private ItemSource source;
    private CipherParameters privateParam;
    private LinkedList<Item> storage = new LinkedList<Item>();
    private Map<String, byte[]> keys = new HashMap<String, byte[]>();

    public DecryptSource (ItemSource source) {
	this(source, null);
    }

    public DecryptSource (ItemSource source, CipherParameters privateParam) {
	Verifier.checkNotNull(source);
	this.source = source;
	this.privateParam = privateParam;
    }

    public void setPrivateParam (CipherParameters privateParam) {
	this.privateParam = privateParam;
    }

    private byte[] processCipherValue (boolean isKey) throws IOException {
	byte[] result = null;
	Item item;
	while ((item = source.next()) != null) {
	    if (isKey) {
		storage.add(item);
	    }
	    if (ParsedPrimitive.isParsedPrimitive(item)) {
		ParsedPrimitive pp = (ParsedPrimitive) item;
		if (pp.getTypeName().equals(XasUtil.BASE64_BINARY_TYPE)
		    || pp.getTypeName().equals(XasUtil.HEX_BINARY_TYPE)) {
		    result = (byte[]) pp.getValue();
		}
	    } else if (Item.isEndTag(item)) {
		EndTag et = (EndTag) item;
		if (et.getName().equals(SecUtil.XENC_CVALUE_NAME)) {
		    break;
		}
	    }
	}
	return result;
    }

    private byte[] decryptKey (byte[] bytes, AsymmetricBlockCipher cipher)
	    throws IOException {
	Verifier.checkNotNull(privateParam);
	if (Log.isEnabled(Log.TRACE)) {
	    Log.log("getKey(" + java.util.Arrays.toString(bytes) + ")",
		Log.TRACE);
	}
	byte[] result = null;
	Object token = Measurer.get(Measurer.TIMING).start();
	cipher.init(false, privateParam);
	try {
	    result = cipher.processBlock(bytes, 0, bytes.length);
	} catch (InvalidCipherTextException ex) {
	    Util.throwWrapped(new IOException(ex.getMessage()), ex);
	}
	Measurer.get(Measurer.TIMING).finish(token, "Key decryption");
	return result;
    }

    private void processEncryptedKey () throws IOException {
	Item item;
	AsymmetricBlockCipher cipher = null;
	byte[] key = null;
	while ((item = source.next()) != null) {
	    storage.add(item);
	    if (Item.isStartTag(item)) {
		StartTag st = (StartTag) item;
		Qname name = st.getName();
		if (name.equals(SecUtil.XENC_METHOD_NAME)) {
		    String cipherName =
			(String) st.getAttributeValue(SecUtil.ALGO_ATT_NAME);
		    if (cipherName == null) {
			throw new IOException("Start tag for "
			    + SecUtil.XENC_METHOD_NAME
			    + " did not contain an algorithm");
		    }
		    cipher = SecUtil.getPublicCipher(cipherName);
		    if (cipher == null) {
			throw new IOException("Algorithm " + cipherName
			    + " not recognized");
		    }
		} else if (name.equals(SecUtil.XENC_CVALUE_NAME)) {
		    byte[] bytes = processCipherValue(true);
		    if (bytes == null) {
			throw new IOException(
			    "Unable to acquire encryption key");
		    }
		    if (cipher == null) {
			throw new IOException(
			    "Encryption key did not contain a "
				+ SecUtil.XENC_METHOD_NAME + " element");
		    }
		    key = decryptKey(bytes, cipher);
		} else if (name.equals(SecUtil.XENC_DATAREF_NAME)) {
		    if (key == null) {
			throw new IOException(SecUtil.XENC_KEY_NAME.getName()
			    + " element missing either a "
			    + SecUtil.XENC_METHOD_NAME.getName()
			    + " element or a "
			    + SecUtil.XENC_CDATA_NAME.getName() + " element");
		    }
		    String uri =
			(String) st.getAttributeValue(SecUtil.URI_ATT_NAME);
		    if (uri == null) {
			throw new IOException(SecUtil.XENC_DATAREF_NAME
			    .getName()
			    + " element missing a "
			    + SecUtil.URI_ATT_NAME.getName() + " attribute");
		    }
		    if (uri.charAt(0) != '#') {
			throw new IOException("URI reference " + uri
			    + " not a relative one");
		    }
		    keys.put(uri.substring(1), key);
		}
	    } else if (Item.isEndTag(item)) {
		EndTag et = (EndTag) item;
		Qname name = et.getName();
		if (name.equals(SecUtil.XENC_KEY_NAME)) {
		    return;
		}
	    }
	}
    }

    private void decryptAndParseBytes (byte[] data, byte[] key,
	    BufferedBlockCipher cipher, StartTag context, String mimeType,
	    boolean isGzip) throws IOException {
	if (Log.isEnabled(Log.DEBUG)) {
	    Log.debug("Decrypting bytes", Util.toPrintable(data));
	}
	Object token = Measurer.get(Measurer.TIMING).start();
	byte[] iv = new byte[cipher.getBlockSize()];
	System.arraycopy(data, 0, iv, 0, iv.length);
	cipher.init(false, new ParametersWithIV(new KeyParameter(key), iv));
	int size = cipher.getOutputSize(data.length - iv.length);
	byte[] result = new byte[size];
	int off =
	    cipher.processBytes(data, iv.length, data.length - iv.length,
		result, 0);
	try {
	    off += cipher.doFinal(result, off);
	} catch (InvalidCipherTextException ex) {
	    Util.throwWrapped(new IOException(ex.getMessage()), ex);
	}
	Measurer.get(Measurer.TIMING)
	    .finish(token, "Symmetric decryption time");
	if (Log.isEnabled(Log.DEBUG)) {
	    Log.debug("Decrypted bytes", Util.toPrintable(result, 0, off));
	}
	FormatFactory factory = XasUtil.getFactory(mimeType);
	if (factory == null) {
	    throw new IOException("MIME type " + mimeType + " not recognized");
	}
	token = Measurer.get(Measurer.TIMING).start();
	ByteArrayInputStream bin = new ByteArrayInputStream(result, 0, off);
	InputStream in = isGzip ? new GZIPInputStream(bin) : bin;
	ItemSource itemSource =
	    TypingUtil.typedSource(factory.createSource(in), mimeType, "UTF-8");
	XasUtil.copyFragment(itemSource, storage);
	Measurer.get(Measurer.TIMING).finish(token, "Parsing decrypted data");
    }

    private void processEncryptedData (StartTag dataStart) throws IOException {
	String id = (String) dataStart.getAttributeValue(SecUtil.ID_ATT_NAME);
	if (id == null) {
	    throw new IOException("Element " + SecUtil.XENC_DATA_NAME.getName()
		+ " not identified by " + SecUtil.ID_ATT_NAME.getName()
		+ " attribute");
	}
	byte[] key = keys.get(id);
	if (key == null) {
	    // TODO Handle case where key comes after data
	    throw new IOException("No key found for data with identifier " + id);
	}
	String mimeType =
	    (String) dataStart.getAttributeValue(SecUtil.MIMETYPE_ATT_NAME);
	if (mimeType == null) {
	    mimeType = XasUtil.XML_MIME_TYPE;
	}
	String contentEncoding =
	    (String) dataStart
		.getAttributeValue(SecUtil.CONTENT_ENCODING_ATT_NAME);
	boolean isGzip =
	    contentEncoding != null
		&& contentEncoding.equals(SecUtil.GZIP_ENCODING_NAME);
	Item item;
	BufferedBlockCipher cipher = null;
	while ((item = source.next()) != null) {
	    if (Item.isStartTag(item)) {
		StartTag st = (StartTag) item;
		Qname name = st.getName();
		if (name.equals(SecUtil.XENC_CVALUE_NAME)) {
		    if (cipher == null) {
			throw new IOException("Encountered "
			    + SecUtil.XENC_CVALUE_NAME.getName()
			    + " element before encryption method");
		    }
		    // TODO Access input stream directly for the cipher
                        // value and produce items one by one using streaming
                        // parsing
		    byte[] encrypted = processCipherValue(false);
		    decryptAndParseBytes(encrypted, key, cipher, dataStart
			.getContext(), mimeType, isGzip);
		} else if (name.equals(SecUtil.XENC_METHOD_NAME)) {
		    String cipherName =
			(String) st.getAttributeValue(SecUtil.ALGO_ATT_NAME);
		    if (cipherName == null) {
			throw new IOException("Element " + st
			    + " does not contain a "
			    + SecUtil.ALGO_ATT_NAME.getName() + " attribute");
		    }
		    cipher = SecUtil.getCipher(cipherName);
		    if (cipher == null) {
			throw new IOException("Cipher " + cipherName
			    + " not recognized");
		    }
		}
	    } else if (Item.isEndTag(item)) {
		EndTag et = (EndTag) item;
		if (et.getName().equals(SecUtil.XENC_DATA_NAME)) {
		    return;
		}
	    }
	}
    }

    public Item next () throws IOException {
	if (!storage.isEmpty()) {
	    return storage.remove();
	} else {
	    Item item = source.next();
	    if (Item.isStartTag(item)) {
		StartTag st = (StartTag) item;
		Qname name = st.getName();
		if (name.equals(SecUtil.XENC_KEY_NAME)) {
		    processEncryptedKey();
		    return item;
		} else if (name.equals(SecUtil.XENC_DATA_NAME)) {
		    processEncryptedData(st);
		    return next();
		}
	    }
	    return item;
	}
    }

}

// arch-tag: 4662130c-ce06-43ba-b1b6-33b52b887194
