/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.compat;

import java.io.IOException;

import fc.xml.xas.Item;
import fc.xml.xas.XmlPullTarget;
import fc.xml.xas.typing.ParsedPrimitive;
import fc.xml.xas.typing.TypedItem;
import fuegocore.util.xas.TypedXmlSerializer;

public class XasBridgeTarget extends XmlPullTarget {

    private TypedXmlSerializer ser;


    public XasBridgeTarget(TypedXmlSerializer ser) {
        super(ser);
        this.ser = ser;
    }


    public void append(Item item) throws IOException {
        if (TypedItem.isTyped(item)) {
            TypedItem ti = (TypedItem) item;
            ser.typedContent(ti.getValue(), ti.getTypeName().getNamespace(),
                             ti.getTypeName().getName());
        } else if (ParsedPrimitive.isParsedPrimitive(item)) {
            ParsedPrimitive pp = (ParsedPrimitive) item;
            ser.typedContent(pp.getValue(), pp.getTypeName().getNamespace(),
                             pp.getTypeName().getName());
        } else {
            super.append(item);
        }
    }

}

// arch-tag: 7053229d-3665-4bb4-a8b7-103d28588c1a
