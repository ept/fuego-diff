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
import java.io.Writer;

import fc.util.Util;

public class Doctype extends Item {

    private String name;
    private String publicId;
    private String systemId;
    private int systemIdQuote = '"';


    public Doctype(String name, String systemId) {
        this(name, null, systemId);
    }


    public Doctype(String name, String publicId, String systemId) {
        super(DOCTYPE);
        Verifier.checkName(name);
        Verifier.checkNotNull(systemId);
        if (systemId.indexOf('"') >= 0) {
            if (systemId.indexOf('\'') >= 0) {
                throw new IllegalArgumentException("System ID " + systemId +
                                                   " contains both kinds of quotes");
            } else {
                systemIdQuote = '\'';
            }
        }
        this.name = name;
        this.publicId = publicId;
        this.systemId = systemId;
    }


    public String getName() {
        return name;
    }


    public String getPublicId() {
        return publicId;
    }


    public String getSystemId() {
        return systemId;
    }


    public void outputSystemLiteral(Writer writer) throws IOException {
        writer.write(systemIdQuote);
        writer.write(systemId);
        writer.write(systemIdQuote);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Doctype)) {
            return false;
        } else {
            Doctype dtd = (Doctype) o;
            return name.equals(dtd.name) && systemId.equals(dtd.systemId) &&
                   Util.equals(publicId, dtd.publicId);
        }
    }


    @Override
    public int hashCode() {
        return 37 * (37 * name.hashCode() + systemId.hashCode()) + Util.hashCode(publicId);
    }


    @Override
    public String toString() {
        return "DTD(" + name + (publicId != null ? " PUBLIC=" + publicId : "") + " SYSTEM=" +
               systemId + ")";
    }

}

// arch-tag: 24548477-15a8-4a20-9b6a-f8c0932983a1
