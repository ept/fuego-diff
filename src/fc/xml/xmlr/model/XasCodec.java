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
import fc.xml.xmlr.NodeReference;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.TreeReference;
import fc.xml.xmlr.xas.PeekableItemSource;
import fc.xml.xmlr.xas.RefItem;

/**
 * Codec for translating between reftree node content and XAS items. A XasCodec is not normally
 * responsible for translating reference nodes; for that see {@link XasCodec.ReferenceCodec}.
 */
public interface XasCodec {

    /**
     * Decode XAS item(s) as node content.
     * @param is
     *            items to decode
     * @param kim
     *            used key identification model
     * @return node content
     * @throws IOException
     *             if an I/O error occurs
     */
    public Object decode(PeekableItemSource is, KeyIdentificationModel kim) throws IOException;


    /**
     * Encode content of node as XAS items. <b>Note:</b> If yo do not use the passed XAS context to
     * when creating a start tag, that tag, and any of its descendants, won't have access to any
     * prefix mappings defined previously in the document. (This is a feature of the XAS API).
     * @param t
     *            item target
     * @param n
     *            node to encode
     * @param context
     *            Current context of the item target
     * @throws IOException
     *             if an I/O error occurs
     */
    public void encode(ItemTarget t, RefTreeNode n, StartTag context) throws IOException;

    // Tagging interface to indicate that the tree model handles reference
    // decode/encode
    /**
     * Tagging interface that indicates that the codec handles decoding and encoding of reference
     * nodes.
     */
    public interface ReferenceCodec {}

    /**
     * Encode-only codec. The <code>decode()</code> method always fails.
     */
    public abstract static class EncoderOnly implements XasCodec {

        /** Item decoder. The method fails. */
        public final Object decode(PeekableItemSource is, KeyIdentificationModel kim)
                throws IOException {
            assert false : "This is not a decoder";
            return null;
        }
    }

    /**
     * Decode-only codec. The <code>encode()</code> method always fails.
     */
    public abstract static class DecoderOnly implements XasCodec {

        /** Node encoder. The method fails. */
        public void encode(ItemTarget t, RefTreeNode n, StartTag context) throws IOException {
            assert false : "This is not a encoder";
        }
    }

    /**
     * Default delegating codec for references. The codec handles reference item encoding/decoding,
     * and delegates the rest to the underlying codec.
     */

    public static class DefaultReferenceCodec implements XasCodec, ReferenceCodec {

        private XasCodec c;


        /**
         * Create new codec.
         * @param c
         *            delegate codec
         */
        public DefaultReferenceCodec(XasCodec c) {
            this.c = c;
        }


        /** @inheritDoc */
        public Object decode(PeekableItemSource is, KeyIdentificationModel kim) throws IOException {
            Item first = RefItem.decode(is.peek());
            if (first == null) throw new IOException("Unexpected end of item stream");
            Object content = null;
            if (RefItem.isRefItem(first)) {
                RefItem ri = (RefItem) first;
                is.next();
                content = ri.isTreeRef() ? new TreeReference(kim.makeKey(ri.getTarget()))
                        : new NodeReference(kim.makeKey(ri.getTarget()));
            } else {
                content = c.decode(is, kim);
            }
            return content;
        }


        /** @inheritDoc */
        public void encode(ItemTarget t, RefTreeNode n, StartTag context) throws IOException {
            if (n.isReference()) {
                RefItem ri = RefItem.makeStartItem(n.getReference(), context);
                t.append(ri);
            } else c.encode(t, n, context);
        }

    }

}
// arch-tag: ee044fc1-9123-4992-8605-82f0da050b1a
