/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xmldiff-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.diff.encode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import fc.xml.diff.Segment;
import fc.xml.xas.Item;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;

public class RefTreeByIdEncoder extends RefTreeEncoder {

    private static final String UNKNOWN_ID_PREFIX = "UNKNOWN_ID+";
    private static final Qname ID_ATTR = new Qname("", "id");
    protected List<Item> base = null;


    @Override
    protected String getBaseRefTarget(int branchPos, Segment<Item> match, MultiXPath xp) {
        int steps = -1, depth = 0;
        int offset = branchPos - match.getPosition();
        int basePos = match.getOffset() + offset;
        // Log.debug("<<<Start identify scan at pos "+basePos);
        // Scan backwards to a parent tag with id. At the same time, we count
        // the
        // position in the child list of the parent.
        // NOTE: We only scan up to the parent, i.e. the tag <x> in
        // <t id="i"><t><x> won't get an id. (We could use a recursive scheme
        // here,
        // to get something like i.0.0)
        while (basePos > 0 && depth >= 0 && (depth > 0 || !Item.isStartTag(base.get(basePos)))) {
            // Log.debug("Identifyscan "+base.get(basePos)+",depth="+depth+",basePos="+
            // basePos+",steps="+steps);
            if (Item.isEndTag(base.get(basePos))) {
                depth++;
            } else if (Item.isStartTag(base.get(basePos))) {
                depth--;
            }
            steps += depth == 0 ? 1 : 0;
            basePos--;
        }
        // Log.debug("===Item to identify is "+base.get(basePos));
        String baseId = null;
        if (basePos > 0 && ((StartTag) base.get(basePos)).getAttribute(ID_ATTR) != null) {
            baseId = ((StartTag) base.get(basePos)).getAttribute(ID_ATTR).getValue().toString();
        }
        if (baseId == null) return UNKNOWN_ID_PREFIX + (branchPos - match.getPosition());
        if (steps >= 0) return baseId + "." + steps;
        return baseId;
    }


    @Override
    public void encodeDiff(List<Item> base, List<Item> doc, List<Segment<Item>> matches,
                           List<Item> preamble, OutputStream out) throws IOException {
        this.base = base;
        super.encodeDiff(base, doc, matches, preamble, out);
    }

}
// arch-tag: aaf67f13-25b5-4141-a404-fe2ed6f0e5c4
//
