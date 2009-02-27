/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

public final class Qname implements Comparable<Qname> {

    private String namespace;
    private String name;

    public Qname (String namespace, String name) {
	Verifier.checkNamespace(namespace);
	Verifier.checkName(name);
	this.namespace = namespace.intern();
	this.name = name.intern();
    }

    public String getNamespace () {
	return namespace;
    }

    public String getName () {
	return name;
    }

    public boolean equals (Object o) {
	if (this == o) {
	    return true;
	} else if (!(o instanceof Qname)) {
	    return false;
	} else {
	    Qname q = (Qname) o;
	    return name.equals(q.name) && namespace.equals(q.namespace);
	}
    }

    public int hashCode () {
	return name.hashCode() ^ namespace.hashCode();
    }

    public int compareTo (Qname q) {
	Verifier.checkNotNull(q);
	int c = namespace.compareTo(q.namespace);
	if (c == 0) {
	    c = name.compareTo(q.name);
	}
	return c;
    }

    public String toString () {
	return "{" + namespace + "}" + name;
    }

}

// arch-tag: 15e8b7c1-7eae-4096-ba3a-6788e4583023
