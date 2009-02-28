/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

public class AttributeNode implements Comparable<AttributeNode> {

    private Qname name;
    private Object value;
    private AttributeNode next;


    public AttributeNode(Qname name, Object value) {
        this(name, value, null);
    }


    public AttributeNode(Qname name, Object value, AttributeNode next) {
        Verifier.checkNotNull(name);
        Verifier.checkValue(value);
        this.name = name;
        this.value = value;
        this.next = next;
    }


    public Qname getName() {
        return name;
    }


    public Object getValue() {
        return value;
    }


    public void setValue(Object value) {
        Verifier.checkNotNull(value);
        this.value = value;
    }


    // XXX Access to next should be private
    AttributeNode getNext() {
        return next;
    }


    // XXX Access to next should be private
    void setNext(AttributeNode next) {
        this.next = next;
    }


    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof AttributeNode)) {
            return false;
        } else {
            AttributeNode a = (AttributeNode) o;
            if (name.equals(a.name)) {
                return value == null ? a.value == null : value.equals(a.value);
            } else {
                return false;
            }
        }
    }


    public int hashCode() {
        return 37 * name.hashCode() + (value == null ? 0 : value.hashCode());
    }


    public int compareTo(AttributeNode a) {
        Verifier.checkNotNull(a);
        return name.compareTo(a.name);
    }


    public String toString() {
        StringBuilder result = new StringBuilder(String.valueOf(name) + "=\"" + value + "\"");
        if (next != null) {
            result.append(" ");
            result.append(String.valueOf(next));
        }
        return result.toString();
    }

}

// arch-tag: 6a6b3d79-8a53-4f90-a803-c226414b7f8d
