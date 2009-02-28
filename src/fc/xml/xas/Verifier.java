/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

import java.io.IOException;
import java.util.Collection;

/**
 * A collection of general purpose verification methods. This class provides a collection of methods
 * useful in verifying method arguments. In addition to very generic methods, several methods are
 * provided to verify XML-related constraints.
 */
public class Verifier {

    private Verifier() {
    }


    public static void check(boolean condition, String message) {
        if (!condition) { throw new IllegalStateException("Verification of condition <" + message +
                                                          "> failed"); }
    }


    public static void checkNamespace(String namespace) {
        checkNotNull(namespace);
    }


    public static void checkName(String name) {
        checkNotNull(name);
    }


    public static void checkValue(Object value) {
        checkNotNull(value);
    }


    public static void checkText(String text) {
        checkNotNull(text);
    }


    public static void checkNull(Object o) {
        if (o != null) { throw new IllegalStateException("Attempting to write a write-once"
                                                         + " value"); }
    }


    public static void checkNotNull(Object o) {
        if (o == null) { throw new NullPointerException("Null argument"); }
    }


    public static void checkNotEquals(Object o1, Object o2) {
        if (o1.equals(o2)) { throw new IllegalArgumentException(String.valueOf(o1) + " == " + o2); }
    }


    public static void checkPositive(int n) {
        if (n <= 0) { throw new IllegalArgumentException(String.valueOf(n) + " <= 0"); }
    }


    public static void checkNotNegative(int n) {
        if (n < 0) { throw new IllegalArgumentException(String.valueOf(n) + " < 0"); }
    }


    public static void checkSmallerEqual(int x, int y) {
        if (x > y) { throw new IllegalArgumentException(String.valueOf(x) + " > " + y); }
    }


    public static void checkSmaller(int x, int y) {
        if (x >= y) { throw new IllegalArgumentException(String.valueOf(x) + " >= " + y); }
    }


    public static void checkInstance(Object o, Class<?> c) {
        if (!c.isInstance(o)) { throw new IllegalArgumentException("Object " + o + " of class " +
                                                                   o.getClass() + " not an " +
                                                                   "instance of " + c); }
    }


    public static void checkInterval(int low, int value, int high) {
        checkSmallerEqual(low, value);
        checkSmaller(value, high);
    }


    public static void checkBetween(int low, int value, int high) {
        checkSmallerEqual(low, value);
        checkSmallerEqual(value, high);
    }


    public static void checkOffsetLength(int min, int offset, int length, int max) {
        checkSmallerEqual(min, offset);
        checkNotNegative(length);
        checkSmallerEqual(offset + length, max);
    }


    public static void checkNotFragment(Item item) {
        if (FragmentItem.isFragment(item)) { throw new IllegalArgumentException("Item " + item +
                                                                                " is not allowed to be a fragment"); }
    }


    public static void checkFragment(Collection<Item> items) {
        checkNotNull(items);
        checkPositive(items.size());
        // checkInstance(items.iterator().next(), StartTag.class);
    }


    public static StartTag verifyStartTag(Item item) throws IOException {
        return verifyStartTag(item, null);
    }


    public static StartTag verifyStartTag(Item item, Qname name) throws IOException {
        if (!Item.isStartTag(item)) { throw new IOException("Expected start tag, got " + item); }
        StartTag st = (StartTag) item;
        if (name != null && !st.getName().equals(name)) { throw new IOException(
                                                                                "Expected start tag with name " +
                                                                                        name +
                                                                                        ", got " +
                                                                                        st.getName()); }
        return st;
    }


    public static EndTag verifyEndTag(Item item, Qname name) throws IOException {
        if (!Item.isEndTag(item)) { throw new IOException("Expected end tag, got " + item); }
        EndTag et = (EndTag) item;
        if (name != null && !et.getName().equals(name)) { throw new IOException(
                                                                                "Expected end tag with name " +
                                                                                        name +
                                                                                        ", got " +
                                                                                        et.getName()); }
        return et;
    }

}

// arch-tag: c42ca2ea-9ab6-4370-863c-97cb087af496
