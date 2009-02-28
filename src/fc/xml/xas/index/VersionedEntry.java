/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.index;

public class VersionedEntry {

    private Index.Entry entry;
    private VersionNode current;


    VersionedEntry(VersionedDocument document, Index.Entry entry) {
        this.entry = entry;
        current = document.getVersion();
    }


    public Index.Entry getEntry() {
        update();
        return entry;
    }


    private boolean isValid() {
        directUpdate();
        return entry != null;
    }


    private void update() {
        if (!isValid()) { throw new IllegalStateException("Entry not valid"); }
        directUpdate();
    }


    private void directUpdate() {
        while (!current.isSentinel()) {
            if (entry != null) {
                entry = current.update(entry);
            }
            current = current.getNext();
        }
    }

}

// arch-tag: 1dca6e5e-2843-4960-b4e0-33f78291cb99
