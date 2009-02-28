/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

public class EndTag extends Item {

    private Qname name;


    public EndTag(Qname name) {
        super(END_TAG);
        Verifier.checkNotNull(name);
        this.name = name;
    }


    public Qname getName() {
        return name;
    }


    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof EndTag)) {
            return false;
        } else {
            EndTag et = (EndTag) o;
            return name.equals(et.name);
        }
    }


    public int hashCode() {
        return 37 * END_TAG + name.hashCode();
    }


    public String toString() {
        return "ET(" + name + ")";
    }

}

// arch-tag: f1fe34e1-becb-40ea-bb42-0f86539f9a9a
