/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

public class EntityRef extends Item {

    private String name;


    public EntityRef(String name) {
        super(ENTITY_REF);
        Verifier.checkName(name);
        this.name = name;
    }


    public String getName() {
        return name;
    }


    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof EntityRef)) {
            return false;
        } else {
            EntityRef e = (EntityRef) o;
            return name.equals(e.name);
        }
    }


    @Override
    public int hashCode() {
        return name == null ? 0 : 37 * name.hashCode();
    }


    public String toString() {
        return "ER(" + name + ")";
    }

}

// arch-tag: 1d847bc3-3d23-43e7-9b0b-414d499cb20a
