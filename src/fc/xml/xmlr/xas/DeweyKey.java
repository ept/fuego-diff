/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.xas;

import java.io.IOException;

import fc.util.Util;
import fc.xml.xas.Item;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.model.IdentificationModel;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.KeyModel;

// NOTE: For the implementation, two alternatives as int arrays
// or int + backPointer; it seems that the former is better for few keys
// with fast computations, and the latter for many keys with less memory
// In the reftree case, I think few key+fast computation is more important,
// so perhaps i should rewrite using int[]
// NOTE2: Root key is one that has a null k

/**
 * XMLR-Compatible Dewey key. Works as {@link fc.xml.xas.index.DeweyKey}, but implements the
 * {@link fc.xml.xmlr.Key} interface.
 * <p>
 * <b>NOTE</b>: This class will probably be merged with {@link fc.xml.xas.index.DeweyKey} at some
 * point.
 */
public class DeweyKey implements Key {

    private final int[] EMPTY_PATH = new int[] {};
    public static final DeweyKey ROOT_KEY = new DeweyKey(fc.xml.xas.index.DeweyKey.root());

    protected fc.xml.xas.index.DeweyKey k;


    public DeweyKey(fc.xml.xas.index.DeweyKey key) {
        this.k = key;
    }


    public fc.xml.xas.index.DeweyKey getXasDeweyKey() {
        return k;
    }


    public static DeweyKey createKey(String s) throws IOException {
        // Count the number of numbers (and check delimiters)
        int nums = 0;
        int lastWasDelim = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isDigit(ch)) {
                nums += lastWasDelim;
                lastWasDelim = 0;
            } else if ('/' == ch) lastWasDelim = 1;
            else throw new IOException("Bad Dewey key " + s);
        }
        int[] path = new int[nums];
        nums = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (Character.isDigit(ch)) {
                // Assuming ASCII digits only...
                assert '0' <= ch && ch <= '9' : "Cannot handle digit " + ch;
                nums += lastWasDelim;
                lastWasDelim = 0;
                path[nums - 1] = path[nums - 1] * 10 + (ch - '0');
            } else if ('/' == ch) lastWasDelim = 1;
            else assert false; // Should never get here
        }
        return construct(path);
    }


    public int length() {
        fc.xml.xas.index.DeweyKey n = k;
        if (n.isRoot()) return 0;
        int length = 0;
        for (; !n.isRoot(); length++)
            n = n.up();
        return length;
    }


    public DeweyKey append(int[] path) {
        return append(path, 0, path.length);
    }


    public DeweyKey append(int[] path, int start) {
        return append(path, start, path.length);
    }


    public DeweyKey append(int[] path, int start, int end) {
        DeweyKey n = this;
        for (int i = start; i < end; i++)
            n = n.child(path[i]);
        return n;
    }


    public boolean equals(Object o) {
        return o instanceof DeweyKey && Util.equals(k, ((DeweyKey) o).k);
    }


    public int hashCode() {
        return k.isRoot() ? 0 : k.hashCode();
    }


    // Delegates BEGIN
    public DeweyKey child(int value) {
        return new DeweyKey(k.child(value));
    }


    public DeweyKey commonAncestor(DeweyKey key2) {
        return new DeweyKey(k.commonAncestor(key2.k));
    }


    public int[] deconstruct() {
        return k.deconstruct();
    }


    public boolean descendantFollowSibling(DeweyKey key2) {
        return k.descendantFollowSibling(key2.k);
    }


    public DeweyKey down() {
        return new DeweyKey(k.down());
    }


    public boolean followSibling(DeweyKey key2) {
        return k.followSibling(key2.k);
    }


    public boolean isChild(DeweyKey key2) {
        return k.isChild(key2.k);
    }


    public boolean isDescendant(DeweyKey key2) {
        return k.isDescendant(key2.k);
    }


    public boolean isDescendantSelf(DeweyKey key2) {
        return k.isDescendantSelf(key2.k);
    }


    public boolean isRoot() {
        return k.isRoot();
    }


    public DeweyKey next() {
        return new DeweyKey(k.next());
    }


    public DeweyKey next(DeweyKey ancestor) {
        return new DeweyKey(k.next(ancestor.k));
    }


    public boolean precedeSibling(DeweyKey key2) {
        return k.precedeSibling(key2.k);
    }


    public DeweyKey prev() {
        return new DeweyKey(k.prev());
    }


    public DeweyKey prev(DeweyKey ancestor) {
        return new DeweyKey(k.prev(ancestor.k));
    }


    public DeweyKey replaceAncestor(DeweyKey old, DeweyKey repl) {
        if (old.k.isRoot()) return this; // / is never an ancestor of anything, so this is a NOP
        return new DeweyKey(k.replaceAncestor(old.k, repl.k));
    }


    public DeweyKey replaceAncestorSelf(DeweyKey old, DeweyKey repl) {
        if (old.k.isRoot()) return repl.append(this.deconstruct());
        return new DeweyKey(k.replaceAncestorSelf(old.k, repl.k));
    }


    public String toString() {
        return k.toString();
    }


    public DeweyKey up() {
        if (k.isRoot()) return null; // throw new
        // IllegalArgumentException("root key has no .up()");
        fc.xml.xas.index.DeweyKey uk = k.up();
        return uk.isRoot() ? DeweyKey.ROOT_KEY : new DeweyKey(uk);
    }


    public int getLastStep() {
        return k.getLastStep();
    }


    // Static Delegates

    public static DeweyKey initial() {
        return topLevel(0);
    }


    public static DeweyKey topLevel(int value) {
        return new DeweyKey(fc.xml.xas.index.DeweyKey.topLevel(value));
    }


    public static DeweyKey construct(int[] path) {
        return new DeweyKey(fc.xml.xas.index.DeweyKey.construct(path));
    }

    // Delegates END

    /**
     * Default key and identification model for Dewey keys. The key model parses keys from strings
     * to Dewey keys. The identification model simply returns MutableRefTree.AUTO_KEY for any item,
     * and dow not tag output items in any way.
     */

    public static KeyIdentificationModel KEY_IDENTIFICATION_MODEL = new KeyIdentificationModel(
                                                                                               new KeyModel() {

                                                                                                   public Key makeKey(
                                                                                                                      Object s)
                                                                                                           throws IOException {
                                                                                                       return s == null ? null
                                                                                                               : DeweyKey.createKey(s.toString());
                                                                                                   }
                                                                                               },
                                                                                               new IdentificationModel() {

                                                                                                   public Key identify(
                                                                                                                       Item i,
                                                                                                                       KeyModel km)
                                                                                                           throws IOException {
                                                                                                       return MutableRefTree.AUTO_KEY;
                                                                                                   }


                                                                                                   public Item tag(
                                                                                                                   Item i,
                                                                                                                   Key k,
                                                                                                                   KeyModel km) {
                                                                                                       return i;
                                                                                                   }
                                                                                               });
}

// arch-tag: 896db162-4ed8-4d4a-99cb-4ba8599ff676
