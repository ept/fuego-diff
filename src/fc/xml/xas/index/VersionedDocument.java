/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.index;

import fc.xml.xas.FragmentPointer;
import fc.xml.xas.MutableFragmentPointer;
import fc.xml.xas.Pointer;
import fc.xml.xas.XasFragment;

/**
 * A document that supports mutations while preserving pointers to itself. Any pointers acquired
 * from such a document with {@link #pointer()} or {@link #query(int[])} implement
 * {@link fc.xml.xas.MutablePointer} so they can be used to mutate the document. Also, any such
 * pointers will always follow their pointed-to item throughout these changes (note that a delete
 * may invalidate such a pointer).
 */
public class VersionedDocument extends Document {

    private VersionNode version = new VersionNode();


    public VersionedDocument(XasFragment fragment) {
        super(fragment);
    }


    public Pointer pointer() {
        return new VersionedPointer(this, DeweyKey.initial());
    }


    @Override
    protected Pointer constructPointer(DeweyKey key, MutableFragmentPointer pointer) {
        return new VersionedPointer(this, key, pointer);
    }


    VersionNode getVersion() {
        return version;
    }


    void insertAfter(DeweyKey key, FragmentPointer pointer) {
        version = version.insertAfter(key, pointer);
    }


    void insertAt(DeweyKey key, FragmentPointer pointer) {
        version = version.insertAt(key, pointer);
    }


    void delete(DeweyKey key, FragmentPointer pointer) {
        version = version.delete(key, pointer);
    }


    void moveAfter(DeweyKey source, FragmentPointer sourcePointer, DeweyKey target,
                   FragmentPointer targetPointer) {
        version = version.moveAfter(source, sourcePointer, target, targetPointer);
    }


    void moveTo(DeweyKey source, FragmentPointer sourcePointer, DeweyKey target,
                FragmentPointer targetPointer) {
        version = version.moveTo(source, sourcePointer, target, targetPointer);
    }

}

// arch-tag: acf84674-e04b-41b4-9f88-74420ab84034
