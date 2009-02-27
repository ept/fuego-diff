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

import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.ISO10126d2Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;

import fc.xml.xas.EndTag;
import fc.xml.xas.ItemSource;
import fc.xml.xas.Qname;
import fc.xml.xas.Verifier;
import fc.xml.xas.typing.TypingUtil;

public class SecUtil {

    public static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";
    public static final String XENC_NS = "http://www.w3.org/2001/04/xmlenc#";

    public static final String SHA_1_DIGEST = DS_NS + "sha1";
    public static final String RSA_SIGNATURE = DS_NS + "rsa-sha1";
    public static final String RSA_CIPHER = XENC_NS + "rsa-1_5";
    public static final String AES_128_CIPHER = XENC_NS + "aes128-cbc";

    public static final String XENC_ELEMENT_TYPE = XENC_NS + "Element";

    public static final Qname DS_SIGNATURE_NAME = new Qname(DS_NS, "Signature");
    public static final EndTag DS_SIGNATURE_END = new EndTag(DS_SIGNATURE_NAME);
    public static final Qname DS_SIGINFO_NAME = new Qname(DS_NS, "SignedInfo");
    public static final EndTag DS_SIGINFO_END = new EndTag(DS_SIGINFO_NAME);
    public static final Qname DS_SIGVALUE_NAME =
	new Qname(DS_NS, "SignatureValue");
    public static final EndTag DS_SIGVALUE_END = new EndTag(DS_SIGVALUE_NAME);
    public static final Qname DS_SIGMETHOD_NAME =
	new Qname(DS_NS, "SignatureMethod");
    public static final EndTag DS_SIGMETHOD_END = new EndTag(DS_SIGMETHOD_NAME);
    public static final Qname DS_KEYINFO_NAME = new Qname(DS_NS, "KeyInfo");
    public static final EndTag DS_KEYINFO_END = new EndTag(DS_KEYINFO_NAME);
    public static final Qname DS_REFERENCE_NAME = new Qname(DS_NS, "Reference");
    public static final EndTag DS_REFERENCE_END = new EndTag(DS_REFERENCE_NAME);
    public static final Qname DS_DIGEST_VALUE_NAME =
	new Qname(DS_NS, "DigestValue");
    public static final EndTag DS_DIGEST_VALUE_END =
	new EndTag(DS_DIGEST_VALUE_NAME);
    public static final Qname DS_DIGEST_METHOD_NAME =
	new Qname(DS_NS, "DigestMethod");
    public static final EndTag DS_DIGEST_METHOD_END =
	new EndTag(DS_DIGEST_METHOD_NAME);
    public static final Qname XENC_KEY_NAME =
	new Qname(XENC_NS, "EncryptedKey");
    public static final EndTag XENC_KEY_END = new EndTag(XENC_KEY_NAME);
    public static final Qname XENC_REFLIST_NAME =
	new Qname(XENC_NS, "ReferenceList");
    public static final EndTag XENC_REFLIST_END = new EndTag(XENC_REFLIST_NAME);
    public static final Qname XENC_DATA_NAME =
	new Qname(XENC_NS, "EncryptedData");
    public static final EndTag XENC_DATA_END = new EndTag(XENC_DATA_NAME);
    public static final Qname XENC_METHOD_NAME =
	new Qname(XENC_NS, "EncryptionMethod");
    public static final EndTag XENC_METHOD_END = new EndTag(XENC_METHOD_NAME);
    public static final Qname XENC_CDATA_NAME =
	new Qname(XENC_NS, "CipherData");
    public static final EndTag XENC_CDATA_END = new EndTag(XENC_CDATA_NAME);
    public static final Qname XENC_CVALUE_NAME =
	new Qname(XENC_NS, "CipherValue");
    public static final EndTag XENC_CVALUE_END = new EndTag(XENC_CVALUE_NAME);
    public static final Qname XENC_DATAREF_NAME =
	new Qname(XENC_NS, "DataReference");
    public static final EndTag XENC_DATAREF_END = new EndTag(XENC_DATAREF_NAME);

    public static final Qname ID_ATT_NAME = new Qname("", "Id");
    public static final Qname URI_ATT_NAME = new Qname("", "URI");
    public static final Qname TYPE_ATT_NAME = new Qname("", "Type");
    public static final Qname ALGO_ATT_NAME = new Qname("", "Algorithm");
    public static final Qname ENCODING_ATT_NAME = new Qname("", "Encoding");
    public static final Qname MIMETYPE_ATT_NAME = new Qname("", "MimeType");
    public static final Qname CONTENT_ENCODING_ATT_NAME =
	new Qname("", "ContentEncoding");

    public static final String GZIP_ENCODING_NAME = "gzip";

    private static SecureRandom random = new SecureRandom();
    private static int id = 0;

    private SecUtil () {
    }

    public static Digest getDigest (String id) {
	Verifier.checkNotNull(id);
	if (id.equals(SHA_1_DIGEST)) {
	    return new SHA1Digest();
	} else {
	    return null;
	}
    }

    public static Digest getDigestFromSignature (String id) {
	Verifier.checkNotNull(id);
	if (id.equals(RSA_SIGNATURE)) {
	    return new SHA1Digest();
	} else {
	    return null;
	}
    }

    public static AsymmetricBlockCipher getSignature (String id) {
	Verifier.checkNotNull(id);
	if (id.equals(RSA_SIGNATURE)) {
	    return new PKCS1Encoding(new RSAEngine());
	} else {
	    return null;
	}
    }

    public static AsymmetricBlockCipher getPublicCipher (String id) {
	Verifier.checkNotNull(id);
	if (id.equals(RSA_CIPHER)) {
	    return new PKCS1Encoding(new RSAEngine());
	} else {
	    return null;
	}
    }

    public static BufferedBlockCipher getCipher (String id) {
	Verifier.checkNotNull(id);
	if (id.equals(AES_128_CIPHER)) {
	    return new PaddedBufferedBlockCipher(new CBCBlockCipher(
		new AESEngine()), new ISO10126d2Padding());
	} else {
	    return null;
	}
    }

    public static byte[] generateKey (String cipherType) {
	Verifier.checkNotNull(cipherType);
	if (cipherType.equals(AES_128_CIPHER)) {
	    byte[] result = new byte[16];
	    random.nextBytes(result);
	    return result;
	} else {
	    return null;
	}
    }

    public static int nextId () {
	return id++;
    }

    public static SecureRandom getRandom () {
	return random;
    }

    public static ItemSource securitySource (ItemSource source, String type) {
	ItemSource decryptSource = new DecryptSource(source);
	ItemSource verifySource = new VerifySource(type, decryptSource);
	return verifySource;
    }

    public static ItemSource typedSecuritySource (ItemSource source,
	    String type, String encoding) {
	return securitySource(TypingUtil.typedSource(source, type, encoding),
	    type);
    }

}

// arch-tag: ab7bb8fc-8c72-40fd-99f2-136ec1bb69c3
