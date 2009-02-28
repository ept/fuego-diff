/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.model;

import java.io.IOException;

import fc.xml.xmlr.Key;

/**
 * Interface for serialization and deserialization of keys.
 */

// What the hell a key model exactly consists of is not clear yet
// We know we want to support
// id attribute keys
// --in this case keys are (statically?) determined during serialize/deserialize
// Numeric XPath keys
// --in this case the key is independent of node content, and depends on the
// node position in the tree
// maybe position-dependent keys really do not fit into the whole thing-- better
// to use some smart key object that can tell its dynamic DeweyKey by computing
// it...
public interface KeyModel {

    // How do keys survive move and serialization?

    /**
     * Make key from object.
     */
    public Key makeKey(Object s) throws IOException;

    // Not at all (i.e. memory addresses)
    /** Transient, i.e. non-serializable keys. */
    public interface Transient extends KeyModel {

    }

    // Any change can break the key, but keys of read-only docs survive
    // E.g. Numeric XPath
    /** Keys that may change when tree is modified. */
    public interface TransientOnChange extends Transient {

    }

    // Keys that survive and edits (and disappear on delete, and new keys appear
    // on insert. E.g. the id attribute model
    /** Persistent node keys. */
    public interface Persistent extends KeyModel {

    }

    /** Model with string keys. */
    public static final KeyModel STRINGKEY = new KeyModel() {

        // NOTE: null = we could not find any id data for the id in question
        public Key makeKey(Object s) {
            return s == null ? null : new StringKey(s.toString());
        }

    };

}

// arch-tag: e9d34af1-fac2-4a12-9f28-f9fe874987bc
