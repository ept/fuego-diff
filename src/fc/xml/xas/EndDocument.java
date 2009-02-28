/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

public class EndDocument extends Item {

    private static final EndDocument instance = new EndDocument();


    public static EndDocument instance() {
        return instance;
    }


    private EndDocument() {
        super(END_DOCUMENT);
    }


    public String toString() {
        return "ED()";
    }


    public boolean equals(Object o) {
        return o == instance;
    }


    public int hashCode() {
        return END_DOCUMENT;
    }

}

// arch-tag: 04768e23-9131-4225-beaf-78331bfea89a
