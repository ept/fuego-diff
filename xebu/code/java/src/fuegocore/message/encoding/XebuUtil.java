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

package fuegocore.message.encoding;

/**
 * General utilities used by Xebu processors.  This class collects
 * static methods to be used when encoding and decoding the Xebu
 * format.  Mostly this includes ways of encoding specific data types
 * to arrays of characters.
 */
public class XebuUtil {

    /*
     * Private constructor to prevent instantiation
     */
    private XebuUtil () {
    }

    /**
     * Encode a short integer into an array.  This method puts the two
     * bytes of the given <code>short</code> into the given array
     * starting at the given index.
     *
     * @param s the <code>short</code> to encode
     * @param target the array to encode <code>s</code> into
     * @param offset the first index in <code>target</code> of the
     * encoded result
     */
    public static void putNormalShort (short s, char[] target, int offset) {
	for (int i = 0; i < 2; i++) {
	    target[offset + i] = (char) ((s >>> (8 - 8 * i)) & 0xFF);
	}
    }

    /**
     * Return an encoded form of a short integer.  This method returns
     * a two-element character array, each element of which is a byte
     * of the given <code>short</code>.
     *
     * @param s the <code>short</code> to encode
     * @return the encoded form of <code>s</code>
     */
    public static char[] putNormalShort (short s) {
	char[] result = new char[2];
	putNormalShort(s, result, 0);
	return result;
    }

    /**
     * Encode an integer into an array.  This method puts the four
     * bytes of the given <code>int</code> into the given array
     * starting at the given index.
     *
     * @param i the <code>int</code> to encode
     * @param target the array to encode <code>i</code> into
     * @param offset the first index in <code>target</code> of the
     * encoded result
     */
    public static void putNormalInt (int i, char[] target, int offset) {
	for (int j = 0; j < 4; j++) {
	    target[offset + j] = (char) ((i >>> (24 - 8 * j)) & 0xFF);
	}
    }

    /**
     * Return an encoded form of an integer.  This method returns a
     * four-element character array, each element of which is a byte
     * of the given <code>int</code>.
     *
     * @param i the <code>int</code> to encode
     * @return the encoded form of <code>i</code>
     */
    public static char[] putNormalInt (int i) {
	char[] result = new char[4];
	putNormalInt(i, result, 0);
	return result;
    }

    /**
     * Encode a long integer into an array.  This method puts the
     * eight bytes of the given <code>long</code> into the given array
     * starting at the given index.
     *
     * @param l the <code>long</code> to encode
     * @param target the array to encode <code>l</code> into
     * @param offset the first index in <code>target</code> of the
     * encoded result
     */
    public static void putNormalLong (long l, char[] target, int offset) {
	for (int i = 0; i < 8; i++) {
	    target[offset + i] = (char) ((l >>> (56 - 8 * i)) & 0xFF);
	}
    }

    /**
     * Return an encoded form of a long integer.  This method returns
     * an eight-element character array, each element of which is a
     * byte of the given <code>long</code>.
     *
     * @param l the <code>long</code> to encode
     * @return the encoded form of <code>l</code>
     */
    public static char[] putNormalLong (long l) {
	char[] result = new char[8];
	putNormalLong(l, result, 0);
	return result;
    }

    /**
     * Encode an integer in compressed format into an array.  This
     * method puts the given <code>int</code> into the given array
     * starting at the given index.  The encoding takes 1, 2, or 5
     * bytes depending on how large the integer is.
     *
     * @param i the <code>int</code> to encode
     * @param target the array to encode <code>i</code> into
     * @param offset the first index in <code>target</code> of the
     * encoded result
     */
    public static int putCompressedInt (int i, char[] target, int offset) {
	int result = 0;
	if (i >= 0 && i < 0x80) {
	    target[offset] = (char) i;
	    result = 1;
	} else if (i >= 0x80 && i < 0x4000) {
	    target[offset] = (char) (0x80 | ((i >>> 8) & 0x3F));
	    target[offset + 1] = (char) (i & 0xFF);
	    result = 2;
	} else {
	    target[offset] = (char) 0xC0;
	    for (int j = 1; j < 5; j++) {
		target[offset + j] = (char) ((i >>> (32 - 8 * j)) & 0xFF);
	    }
	    result = 5;
	}
	return result;
    }

    /**
     * Return a compressed form of an integer.  This method returns a
     * variable-sized character array representing an <code>int</code>
     * that uses one byte for small integers, two bytes for slightly
     * larger ones, and five bytes for others.
     *
     * @param i the <code>int</code> to encode
     * @return the encoded form of <code>i</code>
     */
    public static char[] putCompressedInt (int i) {
	char[] temp = new char[5];
	char[] result = new char[putCompressedInt(i, temp, 0)];
	System.arraycopy(temp, 0, result, 0, result.length);
	return result;
    }

    /**
     * Decode a short integer from a character array.  This method
     * reads two bytes from the given character array and constructs a
     * <code>short</code> from them.
     *
     * @param c the array containing the bytes
     * @param offset the starting index of the encoded
     * <code>short</code>
     * @return a <code>short</code> whose bytes are located in
     * <code>c</code> at index <code>offset</code>
     */
    public static short getNormalShort (char[] c, int offset) {
	short result = 0;
	for (int i = 0; i < 2; i++) {
	    result <<= 8;
	    result |= c[offset + i];
	}
	return result;
    }

    /**
     * Decode am integer from a character array.  This method reads
     * four bytes from the given character array and constructs an
     * <code>int</code> from them.
     *
     * @param c the array containing the bytes
     * @param offset the starting index of the encoded
     * <code>int</code>
     * @return an <code>int</code> whose bytes are located in
     * <code>c</code> at index <code>offset</code>
     */
    public static int getNormalInt (char[] c, int offset) {
	int result = 0;
	for (int i = 0; i < 4; i++) {
	    result <<= 8;
	    result |= c[offset + i];
	}
	return result;
    }

    /**
     * Decode a long integer from a character array.  This method
     * reads eight bytes from the given character array and constructs
     * a <code>long</code> from them.
     *
     * @param c the array containing the bytes
     * @param offset the starting index of the encoded
     * <code>long</code>
     * @return a <code>long</code> whose bytes are located in
     * <code>c</code> at index <code>offset</code>
     */
    public static long getNormalLong (char[] c, int offset) {
	long result = 0;
	for (int i = 0; i < 8; i++) {
	    result <<= 8;
	    result |= c[offset + i];
	}
	return result;
    }

    /**
     * Decode a compressed integer from a character array.  This
     * method reads 1, 2, or 5 bytes, depending on their values, from
     * the given character array and constructs an <code>int</code>
     * from them.
     *
     * @param c the array containing the bytes
     * @param offset the starting index of the encoded
     * <code>int</code>
     * @return an <code>int</code> whose encoded form is located in
     * <code>c</code> at index <code>offset</code>
     */
    public static int getCompressedInt (char[] c, int offset) {
	int result = 0;
	if ((c[offset] & 0x80) == 0) {
	    result = c[offset];
	} else if ((c[offset] & 0xC0) == 0x80) {
	    result = (c[offset] & 0x3F) << 8;
	    result |= c[offset + 1];
	} else {
	    for (int i = 1; i < 5; i++) {
		result <<= 8;
		result |= c[offset + i];
	    }
	}
	return result;
    }

}
