/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.index;

import fc.xml.xas.MutableFragmentPointer;
import fc.xml.xas.Pointer;
import fc.xml.xas.Queryable;
import fc.xml.xas.XasFragment;

/**
 * A class representing a complete document. Essentially this class is just a wrapper around a
 * fragment, and its benefit comes from the {@link GlobalPointer} that is used to access and mutate
 * it.
 */
public class Document implements Queryable {

    private XasFragment fragment;

    private static final int[] ROOT_PATH = new int[0];


    public Document(XasFragment fragment) {
        this.fragment = fragment;
    }


    public XasFragment getFragment() {
        return fragment;
    }


    protected Pointer constructPointer(DeweyKey key, MutableFragmentPointer pointer) {
        return new GlobalPointer(this, key, pointer);
    }


    public GlobalPointer getRoot() {
        return (GlobalPointer) query(ROOT_PATH);
    }


    public Pointer query(int[] path) {
        Pointer pointer = fragment.query(path);
        if (pointer != null) {
            return constructPointer(DeweyKey.construct(path), (MutableFragmentPointer) pointer);
        } else {
            return null;
        }
    }

}

// arch-tag: a8960d97-e9b8-4eab-bd5d-d0bc8d5a3d2e
