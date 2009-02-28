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

package fuegocore.util;

/**
 * A utility class collecting various useful <code>static</code>
 * methods.  This class can only be used for its static methods; no
 * instances can be created.
 *
 * <p>This class should be J2ME-friendly.  Place any methods not
 * compatible with J2ME into the {@link ExtUtil} class.
 */
public final class Util {

    private final static char hexDigits[] = { '0', '1', '2', '3', '4', '5',
					      '6', '7', '8', '9', 'A', 'B',
					      'C', 'D', 'E', 'F' };

    private Util () {
    }

    /**
     * Convert a byte to its hexadecimal representation.
     *
     * @param b the byte value to convert
     * @return the representation of <code>b</code> as a pair of
     * hexadecimal digits.
     */
    public static String toHex (byte b) {
	return (new Character(hexDigits[(b & 0xF0) >> 4])).toString()
	    + hexDigits[b & 0x0F];
    }

    /**
     * Check whether a byte value is a printable character.  Currently
     * returns <code>true</code> only for printable ASCII characters.
     *
     * @param b the byte value to check
     * @return whether it is safe to print the character
     */
    public static boolean isPrintable (byte b) {
	return b >= 0x20 && b < 0x7F;
    }

    /**
     * Convert an array of bytes to a printable string.  All
     * non-printable byte values are converted to their hexadecimal
     * representations (preceded by <code>%</code> like in URLs).
     *
     * @param buffer the array containing the bytes to convert
     * @param offset the starting index of the bytes to convert
     * @param length the number of the bytes to convert
     * @return a printable string representing the arguments
     */
    public static String toPrintable (byte[] buffer, int offset, int length) {
	StringBuffer value = new StringBuffer();
	for (int i = offset; i < offset + length; i++) {
	    if (isPrintable(buffer[i])) {
		value.append((char) buffer[i]);
	    } else {
		value.append("%" + toHex(buffer[i]));
	    }
	}
	return value.toString();
    }

    /**
     * Convert an array of bytes to a printable string.  All
     * non-printable byte values are converted to their hexadecimal
     * representations (preceded by <code>%</code> like in URLs).
     *
     * @param buffer the array containing the bytes to convert
     * @return a printable string representing the arguments
     */
    public static String toPrintable (byte[] buffer) {
	return toPrintable(buffer, 0, buffer.length);
    }

    /**
     * Convert a string to a printable string.
     *
     * @param string the string to convert
     * @return a printable string representing the arguments
     *
     * @see #toPrintable(byte[],int,int)
     */
    public static String toPrintable (String string) {
	return toPrintable(string.getBytes());
    }

    /**
     * Compare two objects for equality.  This method does the right
     * thing in case one, or both, of the objects is
     * <code>null</code>.
     */
    public static boolean equals (Object o1, Object o2) {
	return o1 != null ? o1.equals(o2) : o2 == null;
    }

    /**
     * Get a hash code for an object.  The return value is
     * <code>0</code> in case the argument is <code>null</code>.
     */
    public static int hashCode (Object o) {
	return o != null ? o.hashCode() : 0;
    }

    /**
     * Compare two strings for equality, ignoring case distinctions.
     * The argument strings are considered equal if the result of
     * applying {@link String#toLowerCase} to both of them results in
     * strings that compare equal.
     */
    public static boolean equalsIgnoreCase (String s1, String s2) {
	boolean result = false;
	if (s1 == null) {
	    result = s2 == null;
	} else if (s2 != null) {
	    int n1 = s1.length();
	    int n2 = s2.length();
	    if (n1 == n2) {
		int i;
		for (i = 0; i < n1; i++) {
		    char c1 = s1.charAt(i);
		    char c2 = s2.charAt(i);
		    if (c1 != c2) {
			c1 = Character.toLowerCase(c1);
			c2 = Character.toLowerCase(c2);
			if (c1 != c2) {
			    break;
			}
		    }
		}
		if (i == n1) {
		    result = true;
		}
	    }
	}
	return result;
    }

    /**
     * Make sure an array has enough space.  This method will return
     * an array that has at least the specified size and the contents
     * of the given array, preferring to return the array passed to
     * it.  Typical usage is <code>array = ensureCapacity(array,
     * newSize)</code>.
     *
     * @param array the array to enlarge
     * @param needed the minimum size for the returned array
     * @return an array with size at least <code>needed</code> and
     * containing all the elements of <code>array</code> in the same
     * locations
     */
    public static Object[] ensureCapacity (Object[] array, int needed) {
	if (array.length >= needed) {
	    return array;
	}
	Object[] newArray = new Object[needed + 16];
	System.arraycopy(array, 0, newArray, 0, array.length);
	return newArray;
    }

    /**
     * Make sure an array has enough space.  This method will return
     * an array that has at least the specified size and the contents
     * of the given array, preferring to return the array passed to
     * it.  Typical usage is <code>array = ensureCapacity(array,
     * newSize)</code>.
     *
     * @param array the array to enlarge
     * @param needed the minimum size for the returned array
     * @return an array with size at least <code>needed</code> and
     * containing all the elements of <code>array</code> in the same
     * locations
     */
    public static String[] ensureCapacity (String[] array, int needed) {
	if (array.length >= needed)
	    return array;
	String[] newArray = new String[needed + 16];
	System.arraycopy(array, 0, newArray, 0, array.length);
	return newArray;
    }

    /**
     * Make sure an array has enough space.  This method will return
     * an array that has at least the specified size and the contents
     * of the given array, preferring to return the array passed to
     * it.  Typical usage is <code>array = ensureCapacity(array,
     * newSize)</code>.
     *
     * @param array the array to enlarge
     * @param needed the minimum size for the returned array
     * @return an array with size at least <code>needed</code> and
     * containing all the elements of <code>array</code> in the same
     * locations
     */
    public static int[] ensureCapacity (int[] array, int needed) {
	if (array.length >= needed)
	    return array;
	int[] newArray = new int[needed + 16];
	System.arraycopy(array, 0, newArray, 0, array.length);
	return newArray;
    }

    /**
     * Make sure an array has enough space.  This method will return
     * an array that has at least the specified size, given as a
     * multiplier and number of multiples, and the contents of the
     * given array, preferring to return the array passed to it.
     *
     * <p>Use of this method indicates unwarranted chumminess with the
     * implementation.
     *
     * @param array the array to enlarge
     * @param needed the minimum size for the returned array
     * @return an array with size at least <code>needed</code> and
     * containing all the elements of <code>array</code> in the same
     * locations
     */
    public static int[] ensureCapacityMultiply (int[] array, int multiplier,
						int needed) {
	needed *= multiplier;
	if (array.length >= needed)
	    return array;
	int[] newArray = new int[needed + 16 * multiplier];
	System.arraycopy(array, 0, newArray, 0, array.length);
	return newArray;
    }

}
