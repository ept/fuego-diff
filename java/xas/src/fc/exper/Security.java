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

package fc.exper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.kxml2.io.KXmlParser;

import fc.test.junit.XmlData;
import fc.util.Measurer;
import fc.util.Util;
import fc.xml.xas.EndDocument;
import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.MutableFragmentPointer;
import fc.xml.xas.ParserSource;
import fc.xml.xas.Qname;
import fc.xml.xas.StartDocument;
import fc.xml.xas.StartTag;
import fc.xml.xas.XasFragment;
import fc.xml.xas.XasUtil;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.XmlPullSource;
import fc.xml.xas.security.DecryptSource;
import fc.xml.xas.security.DsSignature;
import fc.xml.xas.security.EncryptedKey;
import fc.xml.xas.security.SecUtil;
import fc.xml.xas.security.TypeAttributeSource;
import fc.xml.xas.security.VerifySource;
import fc.xml.xas.typing.Codec;
import fc.xml.xas.typing.ParsedPrimitive;
import fc.xml.xas.typing.TypedItem;
import fc.xml.xas.typing.TypingUtil;
import fc.xml.xas.typing.XmlCodec;

public class Security {

    private static final String TEST_NS = "http://www.hiit.fi/fuego/fc/xml/wss";
    private static final Qname CARD_NAME = new Qname(TEST_NS, "item");
    private static final EndTag CARD_END = new EndTag(CARD_NAME);

    private static final String SOAP_NS =
	"http://schemas.xmlsoap.org/soap/envelope/";
    private static final Qname ENVELOPE_NAME = new Qname(SOAP_NS, "Envelope");
    private static final EndTag ENVELOPE_END = new EndTag(ENVELOPE_NAME);
    private static final Qname HEADER_NAME = new Qname(SOAP_NS, "Header");
    private static final EndTag HEADER_END = new EndTag(HEADER_NAME);
    private static final Qname BODY_NAME = new Qname(SOAP_NS, "Body");
    private static final EndTag BODY_END = new EndTag(BODY_NAME);

    private static final String WSSE_NS =
	"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final Qname SECURITY_NAME = new Qname(WSSE_NS, "Security");
    private static final EndTag SECURITY_END = new EndTag(SECURITY_NAME);
    private static final Qname BINARY_NAME =
	new Qname(WSSE_NS, "BinarySecurityToken");
    private static final EndTag BINARY_END = new EndTag(BINARY_NAME);

    private static final int CUTOFF = 5;
    private static final int ITERS = 10;

    private static byte[] userBytes =
	{ 48, -126, 3, 12, 48, -126, 1, -12, -96, 3, 2, 1, 2, 2, 16, 51, -90,
	    4, 127, -79, 85, 99, 31, -19, 103, 33, 23, -127, 80, -88, -103, 48,
	    13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 5, 5, 0, 48, 48, 49,
	    14, 48, 12, 6, 3, 85, 4, 10, 12, 5, 79, 65, 83, 73, 83, 49, 30, 48,
	    28, 6, 3, 85, 4, 3, 12, 21, 79, 65, 83, 73, 83, 32, 73, 110, 116,
	    101, 114, 111, 112, 32, 84, 101, 115, 116, 32, 67, 65, 48, 30, 23,
	    13, 48, 53, 48, 51, 49, 57, 48, 48, 48, 48, 48, 48, 90, 23, 13, 49,
	    56, 48, 51, 49, 57, 50, 51, 53, 57, 53, 57, 90, 48, 66, 49, 14, 48,
	    12, 6, 3, 85, 4, 10, 12, 5, 79, 65, 83, 73, 83, 49, 32, 48, 30, 6,
	    3, 85, 4, 11, 12, 23, 79, 65, 83, 73, 83, 32, 73, 110, 116, 101,
	    114, 111, 112, 32, 84, 101, 115, 116, 32, 67, 101, 114, 116, 49,
	    14, 48, 12, 6, 3, 85, 4, 3, 12, 5, 65, 108, 105, 99, 101, 48, -127,
	    -97, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3,
	    -127, -115, 0, 48, -127, -119, 2, -127, -127, 0, -94, -88, -67,
	    -12, 28, -75, 85, -118, 52, 104, 122, -28, 40, 35, 83, -32, 57, 8,
	    -128, -14, -1, 74, 6, -95, 109, -30, -99, 26, 26, -37, -69, 114,
	    -74, 92, 110, -38, -83, -16, -3, -74, -94, 1, 32, -93, 31, -83,
	    126, 6, -89, -35, 13, 37, -81, 57, 1, 93, 114, -120, -37, 4, -54,
	    -6, -66, -103, 73, -52, -92, -41, 10, -100, -75, -53, 58, 57, 19,
	    -104, -64, -86, 13, -122, -37, -111, -24, 73, -107, 115, -25, 105,
	    98, -13, -67, -17, -90, 91, 74, 73, 116, -4, 109, 127, 122, -37,
	    15, -125, -46, -3, -64, 80, 34, -5, 105, -114, 106, -61, 1, 79, 21,
	    -79, -110, -109, -16, 51, 121, 43, 44, -2, 17, -101, 2, 3, 1, 0, 1,
	    -93, -127, -109, 48, -127, -112, 48, 9, 6, 3, 85, 29, 19, 4, 2, 48,
	    0, 48, 51, 6, 3, 85, 29, 31, 4, 44, 48, 42, 48, 40, -94, 38, -122,
	    36, 104, 116, 116, 112, 58, 47, 47, 105, 110, 116, 101, 114, 111,
	    112, 46, 98, 98, 116, 101, 115, 116, 46, 110, 101, 116, 47, 99,
	    114, 108, 47, 99, 97, 46, 99, 114, 108, 48, 14, 6, 3, 85, 29, 15,
	    1, 1, -1, 4, 4, 3, 2, 4, -80, 48, 29, 6, 3, 85, 29, 14, 4, 22, 4,
	    20, 10, -30, 93, 19, 80, 118, 117, 65, 93, -43, -39, 11, 101, 44,
	    -48, -26, -8, -6, 49, -120, 48, 31, 6, 3, 85, 29, 35, 4, 24, 48,
	    22, -128, 20, -64, -99, 40, -4, -63, -21, 53, -95, 29, -42, -86,
	    -86, -96, 28, 26, 77, -62, 73, 15, 15, 48, 13, 6, 9, 42, -122, 72,
	    -122, -9, 13, 1, 1, 5, 5, 0, 3, -126, 1, 1, 0, 5, 58, -87, 58,
	    -101, -42, -5, -84, -85, 45, 124, -108, -108, -3, -79, 37, -79, 36,
	    -94, 21, -57, 35, -109, -106, -64, -91, -91, 120, -26, -3, -122,
	    89, 33, 90, 123, 84, 106, 87, -36, 20, -30, 64, -127, 76, -121,
	    -33, 68, -45, -92, 121, 93, -49, 29, 126, -68, -83, -117, 63, 56,
	    104, 80, 82, -104, 28, 77, 72, -55, 11, -24, -112, 19, 118, 38, 49,
	    78, -21, -128, 80, -70, 64, -118, -99, -104, 101, 116, -76, 79,
	    -60, 104, 95, -111, 55, 113, 66, 7, 95, -26, -80, -118, -1, 8, 119,
	    -59, 91, 25, 8, -44, -33, -72, -61, 102, -108, 3, 58, -108, -32,
	    32, -15, 7, 110, 13, -88, 119, 88, 86, 105, 16, 13, 44, -38, 124,
	    16, 104, 35, 80, 76, -45, 35, 36, 47, 45, 106, -3, 66, 6, -66, -66,
	    19, -111, 64, -101, 3, -41, 63, -70, 73, -58, 127, 69, -61, -45,
	    -105, 71, 42, -101, 68, 14, 47, -37, 46, -40, -91, -79, -92, 27,
	    97, -120, -11, 75, -50, 31, 14, -110, 26, -9, -8, 21, 20, -83,
	    -124, -61, 14, -33, -106, 27, -90, 41, 6, -80, -72, -79, -34, -16,
	    44, 93, 101, -94, -30, -68, 31, 84, -26, 51, 64, -83, 14, 20, -13,
	    98, -11, 19, 2, 81, -44, -121, -45, -87, -67, -72, -75, -100, -110,
	    32, -79, -114, -117, 52, 21, 3, -125, 100, 79, 101, -32, 4, -58,
	    -53, -37, 61, -124, -43, 112, -81, 35, -119, 79 };

    private static Random random = new Random(19061975L);
    private static AsymmetricCipherKeyPair keyPair;
    private static byte[] key;

    private static void initSecurity () {
	BigInteger modulus =
	    new BigInteger(
		"114223138481062383818743472854345446729415521820579721478671411120076686448918620302220709845014730631814478188984612465856840519852245805751892337820823145686311359090114506894126920369485682523288840662511969165284005936035859388250282090838456799125935768645086141305672131191153192376610593502582005436827");
	BigInteger pub = new BigInteger("65537");
	BigInteger priv =
	    new BigInteger(
		"70156155961948275567326563815950795233214260644274158546789757111501088844901677266662957312531515817361615431606536850758127492036749476157451855811245452516036408423715290481767027536732741413397901727264890487785997545700283017129062803344411768517700818077537872436050914359831030152123534899150431708577");
	RSAKeyParameters privParam = new RSAKeyParameters(true, modulus, priv);
	RSAKeyParameters pubParam = new RSAKeyParameters(true, modulus, pub);
	keyPair = new AsymmetricCipherKeyPair(pubParam, privParam);
	key = new byte[16];
	random.nextBytes(key);
    }

    private static void addCard (List<Item> list, StartTag parent,
	    ClassLoader loader) throws IOException {
	StartTag card = new StartTag(CARD_NAME, parent);
	list.add(card);
	list.add(new TypedItem(CreditCard.CARD_TYPE, new CreditCard()));
	list.add(CARD_END);
    }

    private static XasFragment buildMessage (int size, ClassLoader loader)
	    throws IOException {
	List<Item> list = new ArrayList<Item>();
	list.add(StartDocument.instance());
	StartTag envelope = new StartTag(ENVELOPE_NAME);
	envelope.addPrefix(SOAP_NS, "soap");
	envelope.addPrefix(XasUtil.XSI_NS, "xsi");
	envelope.addPrefix(XasUtil.XSD_NS, "xsd");
	envelope.addPrefix(WSSE_NS, "wsse");
	envelope.addPrefix(SecUtil.DS_NS, "ds");
	envelope.addPrefix(SecUtil.XENC_NS, "xenc");
	list.add(envelope);
	StartTag header = new StartTag(HEADER_NAME, envelope);
	list.add(header);
	StartTag security = new StartTag(SECURITY_NAME, header);
	list.add(security);
	StartTag binaryToken = new StartTag(BINARY_NAME, security);
	list.add(binaryToken);
	list.add(new ParsedPrimitive(XasUtil.BASE64_BINARY_TYPE, userBytes));
	list.add(BINARY_END);
	DsSignature sigItem =
	    new DsSignature(keyPair.getPrivate(), security,
		SecUtil.RSA_SIGNATURE);
	list.add(sigItem);
	EncryptedKey encKey = new EncryptedKey(keyPair.getPublic(), security);
	list.add(encKey);
	list.add(SECURITY_END);
	list.add(HEADER_END);
	StartTag body = new StartTag(BODY_NAME, envelope);
	list.add(body);
	List<Item> bodyList = new ArrayList<Item>();
	Qname rootName = new Qname(TEST_NS, "message");
	StartTag root = new StartTag(rootName, body);
	root.addPrefix(TEST_NS, "fc");
	bodyList.add(root);
	int sigStart = bodyList.size();
	Qname cardsName = new Qname(TEST_NS, "cards");
	StartTag cards = new StartTag(cardsName, root);
	cards.addPrefix(TEST_NS, "fc");
	cards.addPrefix(CreditCard.EXPER_NS, "exp");
	cards.addPrefix(XasUtil.XSI_NS, "xsi");
	bodyList.add(cards);
	for (int i = 0; i < size; i++) {
	    addCard(bodyList, cards, loader);
	}
	bodyList.add(new EndTag(cardsName));
	int sigEnd = bodyList.size();
	int encStart = bodyList.size();
	cards = new StartTag(cardsName, root);
	cards.addPrefix(TEST_NS, "fc");
	cards.addPrefix(CreditCard.EXPER_NS, "exp");
	cards.addPrefix(XasUtil.XSI_NS, "xsi");
	bodyList.add(cards);
	for (int i = 0; i < size; i++) {
	    addCard(bodyList, cards, loader);
	}
	bodyList.add(new EndTag(cardsName));
	int encEnd = bodyList.size();
	bodyList.add(new EndTag(rootName));
	XasFragment fragment = new XasFragment(bodyList, bodyList.get(0));
	MutableFragmentPointer pointer = fragment.pointer();
	sigItem.addSignature(pointer, sigStart, sigEnd - sigStart,
	    SecUtil.SHA_1_DIGEST);
	encKey.addDataReference(pointer, encStart, encEnd - encStart);
	encKey.setKey(key);
	list.add(fragment);
	list.add(BODY_END);
	list.add(ENVELOPE_END);
	list.add(EndDocument.instance());
	return new XasFragment(list, list.get(0));
    }

    public static void main (String[] args) {
	try {
	    if (args.length > 1) {
		System.err.println("Usage: Security [<size>]");
		System.exit(1);
	    }
	    int size = args.length > 0 ? Integer.parseInt(args[0]) : 10;
	    initSecurity();
	    ClassLoader loader = Security.class.getClassLoader();
	    Codec.registerPrimitiveCodec(new XmlCodec());
	    Measurer timer = Measurer.get(Measurer.TIMING);
	    Measurer memory = Measurer.get(Measurer.MEMORY);
	    ByteArrayOutputStream bout = new ByteArrayOutputStream();
	    int end = ITERS + CUTOFF;
	    int totalSize = 0;
	    for (int i = 0; i < end; i++) {
		if (i == end - ITERS) {
		    Measurer.initAll();
		}
		XasFragment fragment = buildMessage(size, loader);
		bout.reset();
		XmlOutput target = new XmlOutput(bout, "UTF-8");
		Util.runGc();
		Object memoryToken = memory.start();
		Object timeToken = timer.start();
		fragment.appendTo(target);
		target.flush();
		if (i >= end - ITERS) {
		    timer.finish(timeToken, "Total serializing time");
		    memory.finish(memoryToken, "Total serializing memory");
		    totalSize += bout.size();
		}
		byte[] result = bout.toByteArray();
		ByteArrayInputStream bin = new ByteArrayInputStream(result);
		KXmlParser parser = new KXmlParser();
		XmlPullSource source = new XmlPullSource(parser, bin);
		ParserSource typeSource = new TypeAttributeSource(source);
		ItemSource decodeSource =
		    TypingUtil.typedSource(typeSource, XasUtil.XML_MIME_TYPE,
			"UTF-8");
		ItemSource decryptSource =
		    new DecryptSource(decodeSource, XmlData.getKeyPair()
			.getPrivate());
		VerifySource verifySource =
		    new VerifySource(XasUtil.XML_MIME_TYPE, decryptSource,
			XmlData.getKeyPair().getPublic());
		Util.runGc();
		memoryToken = memory.start();
		timeToken = timer.start();
		while (verifySource.next() != null) {
		    // do nothing
		}
		if (i >= end - ITERS) {
		    timer.finish(timeToken, "Total parsing time");
		    memory.finish(memoryToken, "Total parsing memory");
		}
		int numVerify = verifySource.numberSuccesses();
		if (numVerify != 2) {
		    System.err.println("Verified " + numVerify
			+ " signatures, expected 2");
		}
	    }
	    timer.outputFull(System.out);
	    memory.outputFull(System.out);
	    System.out.println("Total size: " + totalSize + " ("
		+ (end - CUTOFF) + ")");
	} catch (Exception ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
    }

}

// arch-tag: 6517aaa4-b213-4968-a186-2680ab51da3a
