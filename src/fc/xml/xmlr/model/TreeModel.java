/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr.model;

import java.io.IOException;

import fc.xml.xas.Item;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.StartTag;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.xas.PeekableItemSource;

/*
 * Ideas: a TreeModel is a combination of A KeyDeserialization: Object->Key Typical StringKey,
 * Custom
 * 
 * B IdentificationModel: Item->Key (uses KeyDeserialization) Typical "id", position (path), custom
 * attr, custom
 * 
 * C Codec: How is nItem decoded and encoded into a Node content Uses IdentificationModel Optionally
 * includes RefTag encode/decode Typical 1 ST or other item = one content object C.1 RefCodec:
 * 
 * D NodeModel: How is a node built; i.e. attach new Node(Content,Key,refs) to tree Typical case
 * RefTreeNodeImpl Typical combos Old reftee: A=StringKey, B="id", C=custom/GenericCC,
 * D=RefTreeNodeImpl New typed reftree: A=custom, B="id", C=custom/GenericCC, D=RefTreeNodeImpl
 * id/target split: A=StringKey, B="id"(1, C=custom/GenericCC, D=RefTreeNodeImpl Dewey tree:
 * A=DeweyKey/AUTO, B=pathpos/AUTO, C=custom, D=DeweyRefTreeNode
 * 
 * 1) RefItem uses (should use) attrs in order target,id,... so, if there is an target attr, that is
 * used for target; then the Idmodel uses id for key How do we know if id or target is used during
 * encode? Well, if String(target) = String(id), then put only one :), else put many (saves some
 * space, too :) ). That's the way RT ser/deser should be specsed (For always target attr, use
 * custom refcodec)
 * 
 * Note that ABD seem to make Tree<->TypedItems mapping, and C TypedItems<->Items
 */

/**
 * Model for XMLR trees. Combines key, identification and node models, as well as XAS encode and
 * decode.
 */
public class TreeModel extends KeyIdentificationModel implements KeyModel, IdentificationModel,
        XasCodec, NodeModel {

    protected XasCodec codec;
    protected NodeModel nm;


    /**
     * Create a model.
     * @param km
     *            key model
     * @param im
     *            identification model
     * @param codec
     *            XAS codec
     * @param nm
     *            node model
     */
    public TreeModel(KeyModel km, IdentificationModel im, XasCodec codec, NodeModel nm) {
        super(km, im);
        this.codec = codec;
        this.nm = nm;
    }


    /**
     * Create string keys from "id" attribute with given codec.
     * @param c
     *            codec
     * @return tree model
     */
    public static TreeModel createIdAsStringKey(XasCodec c) {
        return new TreeModel(KeyModel.STRINGKEY, IdentificationModel.ID_ATTRIBUTE, c,
                             NodeModel.DEFAULT);
    }


    /** @inheritDoc */
    @Override
    public Key makeKey(Object s) throws IOException {
        return km.makeKey(s);
    }


    /** @inheritDoc */
    @Override
    public Key identify(Item i, KeyModel km) throws IOException {
        return im.identify(i, km);
    }


    /** @inheritDoc */
    @Override
    public Key identify(Item i) throws IOException {
        return im.identify(i, km);
    }


    /** @inheritDoc */
    public Object decode(PeekableItemSource is, KeyIdentificationModel kim) throws IOException {
        return codec.decode(is, kim);
    }


    /** @inheritDoc */
    public void encode(ItemTarget t, RefTreeNode n, StartTag context) throws IOException {
        codec.encode(t, n, context);
    }


    /** @inheritDoc */
    public RefTreeNode build(RefTreeNode parent, Key key, Object content, int pos) {
        return nm.build(parent, key, content, pos);
    }


    /**
     * Get model codec.
     * @return codec
     */
    public XasCodec getCodec() {
        return codec;
    }


    /**
     * Get model identification model.
     * @return identification model
     */
    public IdentificationModel getIdentificationModel() {
        return im;
    }


    /**
     * Get model key model.
     * @return key model
     */
    public KeyModel getKeyModel() {
        return km;
    }


    /**
     * Get model node model.
     * @return node model
     */
    public NodeModel getNodeModel() {
        return nm;
    }


    /**
     * Replace codec.
     * @param c
     *            new codec
     * @return model with codec <i>c</i>
     */
    public TreeModel swapCodec(XasCodec c) {
        return new TreeModel(km, im, c, nm);
    }


    /**
     * Replace key model.
     * @param km
     *            new key model
     * @return model with key model <i>km</i>
     */
    public TreeModel swapKeyModel(KeyModel km) {
        return new TreeModel(km, im, codec, nm);
    }


    /**
     * Replace identification model.
     * @param im
     *            new identification model
     * @return model with identification model <i>im</i>
     */
    public TreeModel swapIdentificationModel(IdentificationModel im) {
        return new TreeModel(km, im, codec, nm);
    }


    /**
     * Replace node model.
     * @param nm
     *            new node model
     * @return model with node model <i>nm</i>
     */
    public TreeModel swapNodeModel(NodeModel nm) {
        return new TreeModel(km, im, codec, nm);
    }


    /** Return string representation. For debug purposes. */
    @Override
    public String toString() {
        return "TreeModel(" + (km == null ? "<null>" : km.getClass().toString()) + "," +
               (im == null ? "<null>" : im.getClass().toString()) + "," +
               (codec == null ? "<null>" : codec.getClass().toString()) + "," +
               (nm == null ? "<null>" : nm.getClass().toString()) + ")";
    }
}

// arch-tag: c5774c8e-d81c-49fd-8b5a-f57b928a3f8b

