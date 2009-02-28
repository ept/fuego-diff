/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

// $Id: Patch.java,v 1.6 2004/11/24 16:08:22 ctl Exp $
// History:
// This file has contained F IXMESaxAdapter and F IXMEXasAdapter in r1.4
package fc.xml.xmlr.tdm;

import java.io.IOException;

import fc.xml.xas.ItemTarget;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.MutableRefTree;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.model.XasCodec;
import fc.xml.xmlr.xas.XasSerialization;

/**
 * Patch a refree with a <code>3dm</code> diff. The implementation is limited to diffs which only
 * refers to full subtrees, such as those produced by {@link Diff}, see {@link Diff#decode
 * Diff.decode} for details.
 * <p>
 * Note that patch is a trivial operation due to the great similarity between <code>3dm</code> diffs
 * and reftrees. In particular, if no node expansion is desired, the process is essentially
 * equivalent to <code>RefTrees.apply(diff.decode(),base)</code>
 */

public class Patch {

    protected Patch() {
    }


    /**
     * Patch a refree with a diff. The output is streamed to a <code>TypedXmlSerializer</code>.
     * @param base
     *            base tree to patch
     * @param diff
     *            diff to apply
     * @param expandDiffRefs
     *            set to <code>true</code> if tree references in the diff shall be expanded to the
     *            corresponding nodes in <code>base</code>.
     * @param out
     *            item target for patched reftree
     * @param xc
     *            XasCodec for output
     * @throws IOException
     *             if an i/O error occurs
     * @throws NodeNotFoundException
     *             if the diff contains an invalid reference
     */
    public static void patch(IdAddressableRefTree base, Diff diff, boolean expandDiffRefs,
                             ItemTarget out, XasCodec xc) throws IOException, NodeNotFoundException {
        RefTree patched = patch(base, diff, expandDiffRefs);
        XasSerialization.writeTree(patched, out, xc);
    }


    /**
     * Patch a refree with a diff.
     * @param base
     *            base tree to patch
     * @param diff
     *            diff to apply
     * @param expandDiffRefs
     *            set to <code>true</code> if tree references in the diff shall be expanded to the
     *            corresponding nodes in <code>base</code>.
     * @return patched reftree (may share contents and structure with <code>base</code>)
     * @throws IOException
     *             if an i/O error occurs
     * @throws NodeNotFoundException
     *             if the diff contains an invalid reference
     */
    public static RefTree patch(IdAddressableRefTree base, Diff diff, boolean expandDiffRefs)
            throws IOException, NodeNotFoundException {
        RefTree patch = diff.decode(base);
        if (expandDiffRefs) {
            MutableRefTree patched = RefTrees.getMutableTree(base);
            RefTrees.apply(patch, patched);
            patch = patched;
        }
        return patch;
    }
}
// arch-tag: 6f8962aa9227fe33d43d7bda4b1b42c5 *-
