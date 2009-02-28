/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.model;

import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xmlr.Key;

/** String key. */
public class StringKey implements Key {

    private String key = null;


    protected StringKey() {
    }


    /**
     * Create a new key.
     * @param id
     *            String id
     */
    public StringKey(String id) {
        key = id;
    }


    /**
     * Create a new key. The returned key is the concatenation of the parent key and the suffix.
     * @param parent
     *            parent key
     * @param suffix
     *            suffix
     * @return new key
     */
    public static StringKey createKey(StringKey parent, String suffix) {
        return new StringKey(parent != null ? parent.key + suffix : suffix);
    }


    /**
     * Create a new key.
     * @param id
     *            String key
     * @return key
     */
    public static StringKey createKey(String id) {
        return new StringKey(id);
    }


    /**
     * Create unique string key.
     * @see UniqueStringKey
     * @return key
     */
    public static StringKey createUniqueKey() {
        // FIXME: Ensure truly unique key by having subclass that does
        // equals, hashCode by object address
        return new UniqueStringKey();
    }


    /**
     * Create a new key.
     * @param id
     *            key number
     * @return key
     */
    public static StringKey createKey(long id) {
        return new StringKey(String.valueOf(id));
    }


    /** @inheritDoc */
    public boolean equals(Object obj) {
        // NOTE: We don't want equals to succeed with String, because we can't
        // make
        // String.equals succeed with StringKey -- i.e., we could make things
        // more convenient one way, but at the risk of introducing strange
        // behavior
        if (obj instanceof String) Log.warning("StringKey being compared to String", obj);
        return obj instanceof StringKey && Util.equals(((StringKey) obj).key, key);
    }


    /** @inheritDoc */
    public int hashCode() {
        return key == null ? 0x0 : key.hashCode() ^ 0xdead;
    }


    /** @inheritDoc */
    public String toString() {
        return key == null ? null : key;
    }

    /**
     * A unique string key. Instances of this key will always be unique, assuming
     * <code>System.identityHashCode(o)</code> is unique for any Object <i>o</i>.
     */
    public static class UniqueStringKey extends StringKey {

        public boolean equals(Object obj) {
            return obj == this;
        }


        public int hashCode() {
            return System.identityHashCode(this);
        }


        // NOTE: Serialized form is not necessarily unique
        public String toString() {
            return "#" + hashCode();
        }

    }
}
// arch-tag: b99ce6f7-d440-45b4-8087-71d7df252eb9
