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

package fc.xml.xas.typing;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import net.iharder.Base64;

import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.Qname;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.StartTag;
import fc.xml.xas.XasUtil;

/**
 * A primitive codec for XML that understands some common XML Schema data types.
 */
public class XmlCodec implements PrimitiveCodec {

    private static final String[] types = { "QName", "base64Binary", "boolean",
	    "byte", "dateTime", "double", "float", "hexBinary", "int", "long",
	    "short", "string" };
    private static final DateFormat dateTimeFormatter = new SimpleDateFormat(
	"yyyy-MM-dd'T'kk:mm:ss.SSSZ");
    private static final byte[] hex = { '0', '1', '2', '3', '4', '5', '6', '7',
	    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    static {
	dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private byte fromHex (char c) throws IOException {
	if (c >= '0' && c <= '9') {
	    return (byte) (c - '0');
	} else if (c >= 'a' && c <= 'f') {
	    return (byte) (c - 'a' + 10);
	} else if (c >= 'A' && c <= 'F') {
	    return (byte) (c - 'A' + 10);
	} else {
	    throw new IOException("Character " + c
		    + " not a valid hex character");
	}
    }

    public String getType () {
	return XasUtil.XML_MIME_TYPE;
    }

    public boolean isKnown (Qname typeName) {
	return (typeName.getNamespace().equals(XasUtil.XSD_NS) || typeName
	    .getNamespace().equals(XasUtil.XSDT_NS))
		&& Arrays.binarySearch(types, typeName.getName()) >= 0;
    }

    public void encode (Qname typeName, Object value, SerializerTarget target)
	    throws IOException {
	OutputStream out = target.getOutputStream();
	String encoding = target.getEncoding();
	String name = typeName.getName();
	if (name.equals("QName")) {
	    Qname n = (Qname) value;
	    StartTag context = target.getContext();
	    if (context != null) {
		String prefix = context.getPrefix(n.getNamespace());
		if (prefix != null && prefix.length() > 0) {
		    out.write(prefix.getBytes(encoding));
		    out.write(":".getBytes(encoding));
		}
	    }
	    out.write(n.getName().getBytes(encoding));
	} else if (name.equals("base64Binary")) {
	    byte[] b = (byte[]) value;
	    Base64.OutputStream bout = new Base64.OutputStream(out);
	    bout.write(b);
	    bout.flushBase64();
	} else if (name.equals("boolean")) {
	    Boolean b = (Boolean) value;
	    out.write(b ? "true".getBytes(encoding) : "false"
		.getBytes(encoding));
	} else if (name.equals("byte")) {
	    Byte b = (Byte) value;
	    out.write(String.valueOf(b).getBytes(encoding));
	} else if (name.equals("dateTime")) {
	    Calendar c = (Calendar) value;
	    out.write(dateTimeFormatter.format(c.getTime()).getBytes(encoding));
	} else if (name.equals("double")) {
	    Double d = (Double) value;
	    out.write(String.valueOf(d).getBytes(encoding));
	} else if (name.equals("float")) {
	    Float f = (Float) value;
	    out.write(String.valueOf(f).getBytes(encoding));
	} else if (name.equals("hexBinary")) {
	    byte[] b = (byte[]) value;
	    for (int i = 0; i < b.length; i++) {
		out.write(hex[(b[i] & 0xF0) >>> 4]);
		out.write(hex[b[i] & 0x0F]);
	    }
	} else if (name.equals("int")) {
	    Integer i = (Integer) value;
	    out.write(String.valueOf(i).getBytes(encoding));
	} else if (name.equals("long")) {
	    Long l = (Long) value;
	    out.write(String.valueOf(l).getBytes(encoding));
	} else if (name.equals("short")) {
	    Short s = (Short) value;
	    out.write(String.valueOf(s).getBytes(encoding));
	} else if (name.equals("string")) {
	    out.write(((String) value).getBytes(encoding));
	} else {
	    throw new IOException("Type name " + typeName + " not recognized");
	}
    }

    public Object decode (Qname typeName, byte[] value, int offset, int length,
	    String encoding, StartTag context) throws IOException {
	Object result = null;
	String name = typeName.getName();
	String content = new String(value, offset, length, encoding);
	if (Log.isEnabled(Log.TRACE)) {
	    Log.log("decode(" + typeName + ", " + content + ", " + encoding
		    + ")", Log.TRACE);
	}
	if (name.equals("QName")) {
	    int index = content.indexOf(':');
	    String prefix = "";
	    if (index >= 0) {
		prefix = content.substring(0, index);
		content = content.substring(index + 1);
	    }
	    String namespace = context.getNamespace(prefix);
	    if (namespace == null && prefix.length() == 0) {
		namespace = "";
	    }
	    if (namespace != null) {
		result = new Qname(namespace, content);
	    }
	} else if (name.equals("base64Binary")) {
	    result = Base64.decode(value, offset, length, Base64.NO_OPTIONS);
	} else if (name.equals("boolean")) {
	    if (content.equals("true") || content.equals("1")) {
		result = Boolean.TRUE;
	    } else if (content.equals("false") || content.equals("0")) {
		result = Boolean.FALSE;
	    }
	} else if (name.equals("byte")) {
	    if (content.charAt(0) == '+') {
		content = content.substring(1);
	    }
	    result = Byte.valueOf(content);
	} else if (name.equals("dateTime")) {
	    try {
		Date d = dateTimeFormatter.parse(content);
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		c.setTime(d);
		result = c;
	    } catch (ParseException ex) {
		Util.throwWrapped(new IOException(ex.getMessage()), ex);
	    }
	} else if (name.equals("double")) {
	    result = Double.valueOf(content);
	} else if (name.equals("float")) {
	    result = Float.valueOf(content);
	} else if (name.equals("hexBinary")) {
	    int len = content.length();
	    if (len % 2 == 0) {
		byte[] val = new byte[len / 2];
		for (int i = 0; i < val.length; i++) {
		    char c1 = content.charAt(2 * i);
		    char c2 = content.charAt(2 * i + 1);
		    val[i] = (byte) (fromHex(c1) << 4 | fromHex(c2));
		}
		result = val;
	    }
	} else if (name.equals("int")) {
	    if (content.charAt(0) == '+') {
		content = content.substring(1);
	    }
	    result = Integer.valueOf(content);
	} else if (name.equals("long")) {
	    if (content.charAt(0) == '+') {
		content = content.substring(1);
	    }
	    result = Long.valueOf(content);
	} else if (name.equals("short")) {
	    if (content.charAt(0) == '+') {
		content = content.substring(1);
	    }
	    result = Short.valueOf(content);
	} else if (name.equals("string")) {
	    result = content;
	} else {
	    throw new IOException("Type name " + typeName + " not recognized");
	}
	return result;
    }

}

// arch-tag: 91b90408-2839-41b5-a303-11ae35d7d184
