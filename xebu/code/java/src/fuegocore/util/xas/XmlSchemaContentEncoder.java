/*
 * Copyright 2006 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import java.util.Calendar;
import java.util.TimeZone;
import java.io.IOException;

import fuegocore.util.Base64;

/**
 * A basic content encoder for textual XML.  This {@link
 * ContentEncoder} implementation encodes some basic data types of XML
 * Schema in accordance with how they are represented in textual XML.
 * Supported types have mostly been chosen based on perceived utility
 * and simple correspondence with Java native types.
 */
public class XmlSchemaContentEncoder implements ContentEncoder {

    public boolean encode (Object o, String namespace, String name,
			   TypedXmlSerializer ser)
	throws IOException {
	boolean result = false;
	if (XasUtil.XSD_NAMESPACE.equals(namespace)) {
	    String output = null;
	    if (name != null) {
		if (name.equals("boolean")) {
		    Boolean b = (Boolean) o;
		    if (b != null) {
			output = b.booleanValue() ? "true" : "false";
		    }
		} else if (name.equals("int")) {
		    Integer i = (Integer) o;
		    if (i != null) {
			output = i.toString();
		    }
		} else if (name.equals("string")) {
		    output = (String) o;
		} else if (name.equals("dateTime")) {
		    Calendar c = (Calendar) o;
		    if (c != null) {
			c.setTimeZone(TimeZone.getTimeZone("GMT"));
			StringBuffer buf = new StringBuffer();
			int n = c.get(Calendar.YEAR);
			if (n < 0) {
			    buf.append('-');
			    n = -n;
			}
			if (n < 1000) {
			    buf.append('0');
			}
			if (n < 100) {
			    buf.append('0');
			}
			if (n < 10) {
			    buf.append('0');
			}
			buf.append(n);
			buf.append('-');
			n = c.get(Calendar.MONTH) + 1;
			if (n < 10) {
			    buf.append('0');
			}
			buf.append(n);
			buf.append('-');
			n = c.get(Calendar.DAY_OF_MONTH);
			if (n < 10) {
			    buf.append('0');
			}
			buf.append(n);
			buf.append('T');
			n = c.get(Calendar.HOUR_OF_DAY);
			if (n < 10) {
			    buf.append('0');
			}
			buf.append(n);
			buf.append(':');
			n = c.get(Calendar.MINUTE);
			if (n < 10) {
			    buf.append('0');
			}
			buf.append(n);
			buf.append(':');
			n = c.get(Calendar.SECOND);
			if (n < 10) {
			    buf.append('0');
			}
			buf.append(n);
			buf.append(".");
			n = c.get(Calendar.MILLISECOND);
			if (n < 100) {
			    buf.append('0');
			}
			if (n < 10) {
			    buf.append('0');
			}
			buf.append(n);
			output = buf.toString();
		    }
		} else if (name.equals("hexBinary")
			   || name.equals("base64Binary")) {
		    byte[] b = (byte[]) o;
		    if (b != null) {
			output = new String(Base64.encode(b));
		    }
		} else if (name.equals("long")) {
		    Long l = (Long) o;
		    if (l != null) {
			output = l.toString();
		    }
		} else if (name.equals("short")) {
		    Short s = (Short) o;
		    if (s != null) {
			output = s.toString();
		    }
		} else if (name.equals("byte")) {
		    Byte b = (Byte) o;
		    if (b != null) {
			output = b.toString();
		    }
		}
	    }
	    if (output != null) {
		String prefix = ser.getPrefix(namespace, false);
		ser.attribute(XasUtil.XSI_NAMESPACE, "type",
			      prefix + ":" + name);
		ser.text(output);
		result = true;
	    }
	}
	return result;
    }

}
