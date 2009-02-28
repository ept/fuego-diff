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

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;

import fc.util.Measurer;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.XasUtil;
import fc.xml.xas.typing.ParsedPrimitive;

public class SignatureItem extends DigestItem {

    public static final int SIGNATURE = 0x2901;

    private CipherParameters privateParam;
    private AsymmetricBlockCipher cipher;

    public SignatureItem (CipherParameters privateParam) {
	this(privateParam, SecUtil.RSA_SIGNATURE);
    }

    public SignatureItem (CipherParameters privateParam, String cipherType) {
	super("", SIGNATURE, SecUtil.getDigestFromSignature(cipherType));
	if (digest == null) {
	    throw new IllegalArgumentException(
		"No digest available for cipher " + cipherType);
	}
	this.privateParam = privateParam;
	this.cipher = SecUtil.getSignature(cipherType);
	if (cipher == null) {
	    throw new IllegalArgumentException("Signature type " + cipherType
		+ " unrecognized");
	}
    }

    protected void setValue () throws IOException {
	byte[] bytes = new byte[digest.getDigestSize()];
	digest.doFinal(bytes, 0);
	if (Log.isEnabled(Log.TRACE)) {
	    Log.log("setValue(), digest=" + java.util.Arrays.toString(bytes),
		Log.TRACE);
	}
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
    }

}

// arch-tag: 7632db9c-7492-4b79-9a62-1390b96ab07d
