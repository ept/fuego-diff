/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import java.util.Calendar;
import java.util.TimeZone;

import fuegocore.util.Base64;

/**
 * A basic content decoder for textual XML. This {@link ContentDecoder} implementation decodes some
 * basic data types of XML Schema in accordance with how they are represented in textual XML.
 * Supported types have mostly been chosen based on perceived utility and simple correspondence with
 * Java native types.
 */
public class XmlSchemaContentDecoder extends ContentDecoder {

    private static int getTwoInt(String string, int index, char last) {
        int i = index + 2;
        if (last != '$') {
            i = string.indexOf(last, index);
        }
        if (i - index == 2) {
            return Integer.parseInt(string.substring(index, i));
        } else {
            throw new NumberFormatException(string + ":" + index + ":" + last);
        }
    }


    @Override
    public Object decode(String typeNs, String typeName, XmlReader reader, EventList attributes) {
        // System.out.println("XSCD.internalDecode called with");
        // System.out.println("typeNs=" + typeNs);
        // System.out.println("typeName=" + typeName);
        // System.out.println("reader=" + reader);
        Object result = null;
        int pos = reader.getCurrentPosition();
        if (XasUtil.XSD_NAMESPACE.equals(typeNs) && typeName != null) {
            Event ev = reader.advance();
            if (ev != null && ev.getType() == Event.CONTENT) {
                String content = (String) ev.getValue();
                if (content != null) {
                    if (typeName.equals("boolean")) {
                        if (content.equals("true") || content.equals("1")) {
                            result = new Boolean(true);
                        } else if (content.equals("false") || content.equals("0")) {
                            result = new Boolean(false);
                        }
                    } else if (typeName.equals("int")) {
                        if (content.charAt(0) == '+') {
                            content = content.substring(1);
                        }
                        result = new Integer(Integer.parseInt(content));
                    } else if (typeName.equals("string")) {
                        result = content;
                    } else if (typeName.equals("dateTime")) {
                        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                        int i = content.indexOf('-');
                        if (i > 0) {
                            int year = Integer.parseInt(content.substring(0, i++));
                            int month = getTwoInt(content, i, '-');
                            i += 3;
                            int day = getTwoInt(content, i, 'T');
                            i += 3;
                            int hour = getTwoInt(content, i, ':');
                            i += 3;
                            int minute = getTwoInt(content, i, ':');
                            i += 3;
                            int second = getTwoInt(content, i, '$');
                            i += 2;
                            int ms = 0;
                            if (content.length() > i && content.charAt(i) == '.') {
                                i += 1;
                                int j = i;
                                while (j < content.length() && Character.isDigit(content.charAt(j))) {
                                    j += 1;
                                }
                                ms = Integer.parseInt(content.substring(i, j));
                            }
                            c.set(Calendar.YEAR, year);
                            c.set(Calendar.MONTH, month - 1);
                            c.set(Calendar.DAY_OF_MONTH, day);
                            c.set(Calendar.HOUR_OF_DAY, hour);
                            c.set(Calendar.MINUTE, minute);
                            c.set(Calendar.SECOND, second);
                            c.set(Calendar.MILLISECOND, ms);
                        }
                        result = c;
                    } else if (typeName.equals("hexBinary") || typeName.equals("base64Binary")) {
                        result = Base64.decode(content.toCharArray());
                    } else if (typeName.equals("long")) {
                        if (content.charAt(0) == '+') {
                            content = content.substring(1);
                        }
                        result = new Long(Long.parseLong(content));
                    } else if (typeName.equals("short")) {
                        if (content.charAt(0) == '+') {
                            content = content.substring(1);
                        }
                        result = new Short(Short.parseShort(content));
                    } else if (typeName.equals("byte")) {
                        if (content.charAt(0) == '+') {
                            content = content.substring(1);
                        }
                        result = new Byte(Byte.parseByte(content));
                    }
                }
            }
        }
        if (result == null) {
            reader.setCurrentPosition(pos);
        }
        return result;
    }

}
