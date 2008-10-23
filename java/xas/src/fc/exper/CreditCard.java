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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import fc.xml.xas.EndTag;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.XasUtil;
import fc.xml.xas.typing.Codec;
import fc.xml.xas.typing.ParsedPrimitive;
import fc.xml.xas.typing.TypingUtil;
import fc.xml.xas.typing.ValueCodec;

public class CreditCard {

    public static final String EXPER_NS = "http://www.hiit.fi/fuego/fc/exper";
    public static final Qname CARD_TYPE = new Qname(EXPER_NS, "card");
    public static final Qname CARD_LIST_TYPE = new Qname(EXPER_NS, "cardList");

    private String name;
    private String number;
    private int expYear;
    private int expMonth;

    private static Random random = new Random(19061975L);
    private static List<String> firsts = null;
    private static List<String> lasts = null;

    static {
	ClassLoader loader = CreditCard.class.getClassLoader();
	try {
	    initLists(loader);
	} catch (IOException ex) {
	    throw new Error(ex);
	}
	Codec.registerValueCodec(new CreditCardCodec());
    }

    private static List<String> initList (ClassLoader loader, String resource)
	    throws IOException {
	BufferedReader r =
	    new BufferedReader(new InputStreamReader(loader
		.getResourceAsStream(resource)));
	List<String> value = new ArrayList<String>();
	String line;
	while ((line = r.readLine()) != null) {
	    value.add(line);
	}
	return value;
    }

    private static void initLists (ClassLoader loader) throws IOException {
	if (firsts == null) {
	    firsts = initList(loader, "exper/firsts");
	}
	if (lasts == null) {
	    lasts = initList(loader, "exper/lasts");
	}
    }

    private static String getRandomName () {
	int fi = random.nextInt(firsts.size());
	int li = random.nextInt(lasts.size());
	return firsts.get(fi) + " " + lasts.get(li);
    }

    private static String getRandomNumber () {
	StringBuilder result = new StringBuilder();
	for (int i = 0; i < 4; i++) {
	    if (result.length() > 0) {
		result.append("-");
	    }
	    result.append(random.nextInt(9000) + 1000);
	}
	return result.toString();
    }

    private static int getRandomYear () {
	return random.nextInt(5) + 2008;
    }

    private static int getRandomMonth () {
	return random.nextInt(12) + 1;
    }

    private CreditCard (String name, String number, int expYear, int expMonth) {
	this.name = name;
	this.number = number;
	this.expYear = expYear;
	this.expMonth = expMonth;
    }

    public CreditCard () {
	name = getRandomName();
	number = getRandomNumber();
	expYear = getRandomYear();
	expMonth = getRandomMonth();
    }

    public String getName () {
	return name;
    }

    public String getNumber () {
	return number;
    }

    public int getExpYear () {
	return expYear;
    }

    public int getExpMonth () {
	return expMonth;
    }

    public static class CreditCardCodec implements ValueCodec {

	private static final Qname CARD_NAME = new Qname(EXPER_NS, "card");
	private static final EndTag CARD_END = new EndTag(CARD_NAME);
	private static final ParsedPrimitive CARD_ATTRIBUTE =
	    new ParsedPrimitive(XasUtil.QNAME_TYPE, CARD_TYPE);
	private static final Qname MONTH_NAME = new Qname(EXPER_NS, "expMonth");
	private static final Qname MONTH_TYPE = XasUtil.INT_TYPE;
	private static final EndTag MONTH_END = new EndTag(MONTH_NAME);
	private static final Qname YEAR_NAME = new Qname(EXPER_NS, "expYear");
	private static final Qname YEAR_TYPE = XasUtil.INT_TYPE;
	private static final EndTag YEAR_END = new EndTag(YEAR_NAME);
	private static final Qname HOLDER_NAME = new Qname(EXPER_NS, "name");
	private static final Qname HOLDER_TYPE = XasUtil.STRING_TYPE;
	private static final EndTag HOLDER_END = new EndTag(HOLDER_NAME);
	private static final Qname NUMBER_NAME = new Qname(EXPER_NS, "number");
	private static final Qname NUMBER_TYPE = XasUtil.STRING_TYPE;
	private static final EndTag NUMBER_END = new EndTag(NUMBER_NAME);

	public Object decode (Qname typeName, ItemSource source)
		throws IOException {
	    if (CARD_TYPE.equals(typeName)) {
		String name =
		    (String) TypingUtil
			.expect(HOLDER_NAME, HOLDER_TYPE, source);
		String number =
		    (String) TypingUtil
			.expect(NUMBER_NAME, NUMBER_TYPE, source);
		Integer year =
		    (Integer) TypingUtil.expect(YEAR_NAME, YEAR_TYPE, source);
		Integer month =
		    (Integer) TypingUtil.expect(MONTH_NAME, MONTH_TYPE, source);
		if (name != null && number != null && year != null
		    && month != null) {
		    return new CreditCard(name, number, year, month);
		} else {
		    return null;
		}
	    } else if (CARD_LIST_TYPE.equals(typeName)) {
		ArrayList<CreditCard> result = new ArrayList<CreditCard>();
		CreditCard card;
		while ((card =
		    (CreditCard) TypingUtil
			.expect(CARD_NAME, CARD_TYPE, source)) != null) {
		    result.add(card);
		}
		if (source.next() != null) {
		    result = null;
		}
		return result;
	    } else {
		return null;
	    }
	}

	public void encode (Qname typeName, Object value, ItemTarget target,
		StartTag parent) throws IOException {
	    if (CARD_TYPE.equals(typeName)) {
		CreditCard card = (CreditCard) value;
		TypingUtil.appendPrimitiveTo(HOLDER_NAME, HOLDER_TYPE,
		    card.name, parent, TypingUtil.STRING_ATTRIBUTE, HOLDER_END,
		    target);
		TypingUtil.appendPrimitiveTo(NUMBER_NAME, NUMBER_TYPE,
		    card.number, parent, TypingUtil.STRING_ATTRIBUTE,
		    NUMBER_END, target);
		TypingUtil.appendPrimitiveTo(YEAR_NAME, YEAR_TYPE,
		    card.expYear, parent, TypingUtil.INT_ATTRIBUTE, YEAR_END,
		    target);
		TypingUtil.appendPrimitiveTo(MONTH_NAME, MONTH_TYPE,
		    card.expMonth, parent, TypingUtil.INT_ATTRIBUTE, MONTH_END,
		    target);
	    } else if (CARD_LIST_TYPE.equals(typeName)) {
		List list = (List) value;
		for (Object o : list) {
		    TypingUtil.appendComplexTo(CARD_NAME, CARD_TYPE, o, parent,
			CARD_ATTRIBUTE, CARD_END, target);
		}
	    }
	}

	public boolean isKnown (Qname typeName) {
	    return CARD_TYPE.equals(typeName)
		|| CARD_LIST_TYPE.equals(typeName);
	}

    }

}

// arch-tag: c8280ba9-83f5-4975-9a5a-3cbf59712112
