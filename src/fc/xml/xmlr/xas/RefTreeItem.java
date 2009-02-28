/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.xas;

import java.io.IOException;
import java.util.Iterator;

import fc.xml.xas.AttributeNode;
import fc.xml.xas.EndTag;
import fc.xml.xas.Qname;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.StartTag;
import fc.xml.xmlr.Reference;
import fc.xml.xmlr.TreeReference;
import fc.xml.xmlr.model.KeyModel;

/** Reference item holding a tree reference. */
public class RefTreeItem extends RefItem {

    public static final int TREE_REFERENCE = 0x3101;
    private Object target;
    private StartTag st;
    private static final EndTag REF_TREE_END = new EndTag(REF_TAG_TREE);


    RefTreeItem(StartTag t, StartTag ctx) throws IOException {
        super(TREE_REFERENCE);
        st = new StartTag(REF_TAG_TREE, ctx);
        st.ensurePrefix(REF_NS, "ref");
        AttributeNode n = t.getAttribute(REF_ATT_TARGET);
        n = n != null ? n : t.getAttribute(REF_ATT_ID);
        if (n == null) throw new IOException("Missing reference target " + REF_ATT_ID);
        st.addAttribute(n.getName(), n.getValue());
        target = n.getValue();
    }


    RefTreeItem(Object t, StartTag ctx) {
        super(TREE_REFERENCE);
        st = new StartTag(REF_TAG_TREE, ctx);
        st.ensurePrefix(REF_NS, "ref");
        target = t;
    }


    @Override
    public Object getTarget() {
        return target;
    }


    public void serialize(String type, SerializerTarget ser) throws IOException {
        st.ensurePrefix(REF_NS, "ref");
        if (!st.ensureAttribute(REF_ATT_ID, target.toString()).equals(target.toString())) {
            st.addAttribute(REF_ATT_TARGET, target.toString());
        }
        ser.append(st);
        // BUGFIX-2006114-1: End tag was not emitted
        ser.append(REF_TREE_END);
    }


    @Override
    public boolean isTreeRef() {
        return true;
    }


    @Override
    public Iterator<AttributeNode> getExtraAttributes() {
        return st.attributes();
    }


    @Override
    public AttributeNode getAttribute(Qname n) {
        return st.getAttribute(n);
    }


    @Override
    public void addExtraAttribute(AttributeNode aan) {
        st.addAttribute(aan.getName(), aan.getValue());
    }


    @Override
    public Reference createReference(KeyModel km) throws IOException {
        return new TreeReference(km.makeKey(target));
    }


    @Override
    public StartTag getContext() {
        return st;
    }


    @Override
    public String toString() {
        return "RT(" + getTarget() + ")";
    }

}

// arch-tag: 374869b3-5e60-4c60-b56c-7b74545eee85
