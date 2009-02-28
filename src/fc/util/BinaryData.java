/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Jaakko Kangasharju
 */
public class BinaryData {

    private BinaryData() {

    }


    public static long readNumber(InputStream in) throws IOException {
        return readNumber(in, in.read());
    }


    public static long readNumber(InputStream in, int first) throws IOException {
        if (first == 0) { return 0; }
        long value = first & 0x7F;
        int i = 7;
        while ((first & 0x80) != 0) {
            first = in.read();
            value |= (((long) (first & 0x7F)) << i);
            i += 7;
        }
        return value;
    }


    public static long readNumber(byte[] b, int off, int len) {
        if (b == null) { throw new NullPointerException("Received null byte array"); }
        if (off < 0 || off >= b.length) { throw new IllegalArgumentException("Invalid offset " +
                                                                             off + " not in [0," +
                                                                             b.length + ")"); }
        if (len < 0 || off + len > b.length) { throw new IllegalArgumentException(
                                                                                  "Invalid length " +
                                                                                          len +
                                                                                          " not in [0," +
                                                                                          (b.length - off) +
                                                                                          "]"); }
        int l = len;
        int o = off;
        long result = 0;
        int i = 0;
        while (l > 0) {
            byte value = b[o];
            result |= (((long) (value & 0x7F)) << i);
            i += 7;
            o += 1;
            l -= 1;
            if ((value & 0x80) == 0) {
                break;
            }
        }
        if (l == 0 && (b[o - 1] & 0x80) != 0) { throw new IllegalArgumentException(
                                                                                   "Array " +
                                                                                           Util.toPrintable(
                                                                                                            b,
                                                                                                            off,
                                                                                                            len) +
                                                                                           " does not start with a valid number"); }
        return result;
    }


    public static int numberLength(byte[] b, int off, int len) {
        if (b == null) { throw new NullPointerException("Received null byte array"); }
        if (off < 0 || off >= b.length) { throw new IllegalArgumentException("Invalid offset " +
                                                                             off + " not in [0," +
                                                                             b.length + ")"); }
        if (len < 0 || off + len > b.length) { throw new IllegalArgumentException(
                                                                                  "Invalid length " +
                                                                                          len +
                                                                                          " not in [0," +
                                                                                          (b.length - off) +
                                                                                          "]"); }
        int result = 0;
        int o = off;
        int l = len;
        do {
            result += 1;
            o += 1;
            l -= 1;
        } while (l > 0 && (b[o - 1] & 0x80) != 0);
        if (l == 0 && (b[o - 1] & 0x80) != 0) { throw new IllegalArgumentException(
                                                                                   "Array " +
                                                                                           Util.toPrintable(
                                                                                                            b,
                                                                                                            off,
                                                                                                            len) +
                                                                                           " does not contain a complete number"); }
        return result;
    }


    public static String readString(InputStream in) throws IOException {
        return readString(in, in.read());
    }


    private static String readString(InputStream in, int first) throws IOException {
        int len = (int) readNumber(in, first);
        byte[] bytes = new byte[len];
        int offset = 0;
        while (len > 0) {
            int n = in.read(bytes, offset, len);
            if (n < 0) { throw new IOException("Premature end of stream, expected to read " + len +
                                               " more bytes"); }
            offset += n;
            len -= n;
        }
        return new String(bytes, "US-ASCII");
    }


    public static void writeNumber(long value, OutputStream out) throws IOException {
        if (value == 0) {
            out.write(0x00);
        } else if (value > 0) {
            do {
                long next = value >>> 7;
                int b = (int) (value & 0x7F);
                if (next > 0) {
                    b |= 0x80;
                }
                out.write(b);
                value = next;
            } while (value > 0);
        } else {
            throw new IOException("Negative value " + value + " not supported");
        }
    }


    public static void writeString(String value, OutputStream out) throws IOException {
        byte[] bytes = value.getBytes("US-ASCII");
        writeNumber(bytes.length, out);
        out.write(bytes);
    }

}

// arch-tag: 84eb2a30-fd05-4522-8f3b-378f9abeaf43
