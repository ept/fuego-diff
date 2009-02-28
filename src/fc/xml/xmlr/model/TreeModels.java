/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.model;

import fc.xml.xmlr.RefTreeNodeImpl;
import fc.xml.xmlr.xas.DeweyKey;
import fc.xml.xmlr.xas.DeweyRefNode;
import fc.xml.xmlr.xas.UniformXasCodec;

/**
 * Useful tree models.
 */

public class TreeModels {

    /**
     * Tree of XAS items indexed by Dewey key. This is the most straightforward XMLR tree model for
     * a XAS document. Each XAS item corresponds to a node, and the nesting of tags determine the
     * tree hierarchy. All items from the underlying XAS source are present, including start and end
     * document items.
     * <p>
     * Typically, a tree has something like the following structure
     * 
     * <pre>
     *  Key  Node                 
     *  \    SD
     *  \0   CM(This is a comment)
     *  \1   C(%0a)
     *  \2   ST(root)
     *  \2\0 C(Text)
     *  .
     *  .
     *  .
     *  \2\9 ET(root)
     *  \3   C(%0a)
     * </pre>
     * 
     * More specifically, the model is <code>TreeModel(DeweyKey.KEY_IDENTIFICATION_MODEL,
     *  DeweyKey.KEY_IDENTIFICATION_MODEL,UniformXasCodec.ITEM_CODEC,
     *  DeweyRefNode.NODE_MODEL)</code>
     * @return a tree model as described above.
     */
    public static TreeModel xasItemTree() {
        return new TreeModel(DeweyKey.KEY_IDENTIFICATION_MODEL, DeweyKey.KEY_IDENTIFICATION_MODEL,
                             UniformXasCodec.ITEM_CODEC, DeweyRefNode.NODE_MODEL);
    }


    /**
     * XMLR API V1 Model. The codec is null, so you need to set it using <code>swapCodec()</code>.
     * More specifically, the model is <code>TreeModel(KeyIdentificationModel.ID_AS_STRINGKEY,
     * KeyIdentificationModel.ID_AS_STRINGKEY, null,
     * RefTreeNodeImpl.NODE_MODEL)</code>
     * @return a tree model as described above.
     */
    public static TreeModel xmlr1Model() {
        return XMLR1_MODEL;
    }

    static private final TreeModel XMLR1_MODEL = new TreeModel(
                                                               KeyIdentificationModel.ID_AS_STRINGKEY,
                                                               KeyIdentificationModel.ID_AS_STRINGKEY,
                                                               null, RefTreeNodeImpl.NODE_MODEL);
}

// arch-tag: 94949765-0a02-40f0-973e-ab24ae5ff2d6
