/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

public class PrefixNode implements Comparable<PrefixNode> {

    private String namespace;
    private String prefix;
    private PrefixNode next;


    public PrefixNode(String namespace, String prefix) {
        this(namespace, prefix, null);
    }


    public PrefixNode(String namespace, String prefix, PrefixNode next) {
        Verifier.checkNamespace(namespace);
        Verifier.checkName(prefix);
        this.namespace = namespace;
        this.prefix = prefix;
        this.next = next;
    }


    public String getNamespace() {
        return namespace;
    }


    public String getPrefix() {
        return prefix;
    }


    // XXX Access to next should be private
    PrefixNode getNext() {
        return next;
    }


    // XXX Access to next should be private
    void setNext(PrefixNode next) {
        this.next = next;
    }


    public String getNamespace(String prefix) {
        String namespace = null;
        boolean emptyMapped = false;
        for (PrefixNode n = this; n != null; n = n.getNext()) {
            if (prefix.equals(n.getPrefix())) {
                namespace = n.getNamespace();
                break;
            } else if (!emptyMapped && n.getPrefix().length() == 0 && n.getNamespace().length() > 0) {
                emptyMapped = true;
            }
        }
        if (namespace == null) {
            if (!emptyMapped && prefix.length() == 0) {
                namespace = "";
            } else {
                throw new IllegalStateException("No namespace found for prefix " + prefix +
                                                " in mapping " + this);
            }
        }
        return namespace;
    }


    public String getPrefix(String namespace) {
        String prefix = null;
        boolean emptyMapped = false;
        for (PrefixNode n = this; n != null; n = n.getNext()) {
            if (namespace.equals(n.getNamespace())) {
                prefix = n.getPrefix();
                break;
            } else if (!emptyMapped && n.getPrefix().length() == 0 && n.getNamespace().length() > 0) {
                emptyMapped = true;
            }
        }
        if (prefix == null) {
            if (!emptyMapped && namespace.length() == 0) {
                prefix = "";
            } else if (namespace.equals(XasUtil.XML_NS)) {
                prefix = "xml";
            } else {
                throw new IllegalStateException("No prefix found for namespace " + namespace +
                                                " in mapping " + this);
            }
        }
        return prefix;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof PrefixNode)) {
            return false;
        } else {
            PrefixNode p = (PrefixNode) o;
            return prefix.equals(p.prefix);
        }
    }


    @Override
    public int hashCode() {
        return 0x1234 + namespace.hashCode();
    }


    public int compareTo(PrefixNode p) {
        Verifier.checkNotNull(p);
        return namespace.compareTo(p.namespace);
    }


    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("{" + prefix + "=" + namespace + "}");
        if (next != null) {
            result.append(" ");
            result.append(String.valueOf(next));
        }
        return result.toString();
    }

}

// arch-tag: 33af98a5-21ec-4e81-8104-a0dd10ad1ae9
