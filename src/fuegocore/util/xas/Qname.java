/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

/**
 * A class for representing XML names. A name in an XML document has two parts: a namespace URI and
 * a local name. This class is used to hold such a pair as a single object. It differs from other
 * such classes in that it does not contain the namespace prefix. Objects of this class are
 * immutable.
 */
public class Qname {

    private String namespace;
    private String name;


    public Qname(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }


    public String getNamespace() {
        return namespace;
    }


    public String getName() {
        return name;
    }

}
