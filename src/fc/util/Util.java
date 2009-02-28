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
import java.io.OutputStream;
import java.util.Iterator;

/**
 * A class of static general utility methods. This class collects miscellaneous useful methods to
 * one class. No instances of this class can be created.
 * @author Jaakko Kangasharju
 * @author Tancred Lindholm
 */
public final class Util {

    private static final Runtime runtime = Runtime.getRuntime();

    private final static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                                             'A', 'B', 'C', 'D', 'E', 'F' };


    /*
     * Private constructor to prevent instantiation.
     */
    private Util() {
    }


    private static void _runGc() {
        long isFree = runtime.freeMemory();
        long wasFree;
        do {
            wasFree = isFree;
            runtime.runFinalization();
            runtime.gc();
            Thread.yield();
            isFree = runtime.freeMemory();
        } while (isFree > wasFree);
    }


    /**
     * Compare two objects for equality. This method does the right thing in case one, or both, of
     * the objects is <code>null</code>.
     */
    public static boolean equals(Object o1, Object o2) {
        return o1 != null ? o1.equals(o2) : o2 == null;
    }


    /**
     * Get a hash code for an object. The return value is <code>0</code> in case the argument is
     * <code>null</code>.
     */
    public static int hashCode(Object o) {
        return o != null ? o.hashCode() : 0;
    }


    /**
     * Compare two strings for equality, ignoring case distinctions. The argument strings are
     * considered equal if the result of applying {@link String#toLowerCase()} to both of them
     * results in strings that compare equal.
     */
    public static boolean equalsIgnoreCase(String s1, String s2) {
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
     * Run the garbage collector. This method runs calls the garbage collection several times to try
     * to make sure all garbage is actually collected after the method finishes.
     * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip130.html">Java Tip
     *      130</a>
     */
    public static void runGc() {
        for (int i = 0; i < 4; i++) {
            _runGc();
        }
    }


    /**
     * Return the total memory currently in use. This method returns the amount of memory (in bytes)
     * currently used of the heap. The possibility of heap size increase is taken into account.
     * @see <a href="http://www.javaworld.com/javaworld/javatips/jw-javatip130.html">Java Tip
     *      130</a>
     */
    public static long usedMemory() {
        return runtime.totalMemory() - runtime.freeMemory();
    }


    /**
     * Convert a byte to its hexadecimal representation.
     * @param b
     *            the byte value to convert
     * @return the representation of <code>b</code> as a pair of hexadecimal digits.
     */
    public static String toHex(byte b) {
        return (new Character(hexDigits[(b & 0xF0) >> 4])).toString() + hexDigits[b & 0x0F];
    }


    /**
     * Check whether a byte value is a printable character. Currently returns <code>true</code> only
     * for printable ASCII characters.
     * @param b
     *            the byte value to check
     * @return whether it is safe to print the character
     */
    public static boolean isPrintable(byte b) {
        return b >= 0x20 && b < 0x7F;
    }


    /**
     * Convert an array of bytes to a printable string. All non-printable byte values are converted
     * to their hexadecimal representations (preceded by <code>%</code> like in URLs).
     * @param buffer
     *            the array containing the bytes to convert
     * @param offset
     *            the starting index of the bytes to convert
     * @param length
     *            the number of the bytes to convert
     * @return a printable string representing the arguments
     */
    public static String toPrintable(byte[] buffer, int offset, int length) {
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
     * Convert an array of bytes to a printable string. All non-printable byte values are converted
     * to their hexadecimal representations (preceded by <code>%</code> like in URLs).
     * @param buffer
     *            the array containing the bytes to convert
     * @return a printable string representing the arguments
     */
    public static String toPrintable(byte[] buffer) {
        return toPrintable(buffer, 0, buffer.length);
    }


    /**
     * Convert a string to a printable string.
     * @param string
     *            the string to convert
     * @return a printable string representing the arguments
     * @see #toPrintable(byte[],int,int)
     */
    public static String toPrintable(String string) {
        return toPrintable(string.getBytes());
    }


    public static <T extends Throwable> void throwWrapped(T throwed, Throwable wrapped) throws T {
        throw (T) throwed.initCause(wrapped);
    }


    /**
     * Make sure an array has enough space. This method will return an array that has at least the
     * specified size and the contents of the given array, preferring to return the array passed to
     * it. Typical usage is <code>array = ensureCapacity(array,
     * newSize)</code>.
     * @param array
     *            the array to enlarge
     * @param needed
     *            the minimum size for the returned array
     * @return an array with size at least <code>needed</code> and containing all the elements of
     *         <code>array</code> in the same locations
     */
    public static Object[] ensureCapacity(Object[] array, int needed) {
        if (array.length >= needed) { return array; }
        Object[] newArray = new Object[needed + 16];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }


    /**
     * Make sure an array has enough space. This method will return an array that has at least the
     * specified size and the contents of the given array, preferring to return the array passed to
     * it. Typical usage is <code>array = ensureCapacity(array,
     * newSize)</code>.
     * @param array
     *            the array to enlarge
     * @param needed
     *            the minimum size for the returned array
     * @return an array with size at least <code>needed</code> and containing all the elements of
     *         <code>array</code> in the same locations
     */
    public static String[] ensureCapacity(String[] array, int needed) {
        if (array.length >= needed) return array;
        String[] newArray = new String[needed + 16];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }


    /**
     * Make sure an array has enough space. This method will return an array that has at least the
     * specified size and the contents of the given array, preferring to return the array passed to
     * it. Typical usage is <code>array = ensureCapacity(array,
     * newSize)</code>.
     * @param array
     *            the array to enlarge
     * @param needed
     *            the minimum size for the returned array
     * @return an array with size at least <code>needed</code> and containing all the elements of
     *         <code>array</code> in the same locations
     */
    public static int[] ensureCapacity(int[] array, int needed) {
        if (array.length >= needed) return array;
        int[] newArray = new int[needed + 16];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }


    public static final boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }


    /**
     * Get iterable out of iterator. Convenience method to bypass JDK5 brain damage.
     */
    public static final <T> Iterable<T> iterable(final Iterator<T> i) {
        return new Iterable<T>() {

            public Iterator<T> iterator() {
                return i;
            }
        };
    }

    public static final OutputStream SINK = new OutputStream() {

        // NOTE: Overriding all variants to avoid calling write(byte) n times
        @Override
        public void write(int b) throws IOException {
        }


        @Override
        public void write(byte[] b, int off, int len) throws IOException {
        }


        @Override
        public void write(byte[] b) throws IOException {
        }

    };
}

/*
 * arch-tag: 53e35c98-17ff-45c8-a1cf-b28d06959f67
 */
