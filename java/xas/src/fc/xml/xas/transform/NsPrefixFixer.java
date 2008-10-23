/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.transform;

import java.io.IOException;
import java.util.Iterator;

import fc.util.log.Log;
import fc.xml.xas.Item;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.PrefixNode;
import fc.xml.xas.StartTag;

/** Fixes namespace prefixes. Sometimes, tracking the proper
 * namespace prefix mappings in an output document is very
 * involved, e.g., because fragments are combined in a 
 * random access fashion. This filter ensures that every
 * item gets a list of prefix mappings that includes every
 * namespace in-scope at the item's location in the output. The
 * prefixes to use are gathered from the contexts of the filtered items.
 * <p>By default, the first seen prefix for an URI is used. If you
 * really want the same URI to potentially appear with with several
 * prefixes, use {@link setBorderlineXml()}
 */

public class NsPrefixFixer implements ItemTransform {

    private Item item=null;
    private boolean borderline = false;
    private StartTag context = null;
    
    /** Enable the same URI to potentially appear with with several
     * prefixes. You should be aware that this has been called 
     * <a href="http://lists.xml.org/archives/xml-dev/200204/msg00170.html">
     * "borderline" use of XML</a>.
     */
    public void setBorderlineXml(boolean enable) {
	borderline = enable;
    }

    public void append(Item i) throws IOException {
	if( Item.isStartTag(i)) {
	    StartTag st = (StartTag) i;
	    context = st.withContext(context);
	    // Log.debug("Current context",context);
	    // Log.debug("StartTag",st);
	    for( Iterator<PrefixNode> pi = st.allPrefixes(); pi.hasNext(); ) {
		PrefixNode p = pi.next();
		// Log.debug("Adding prefix node",p);
		if( !borderline )
		    context.ensurePrefix(p.getNamespace(), p.getPrefix());
		else
		    context.addPrefix(p.getNamespace(), p.getPrefix());
	    }
	    // Log.debug("Fixed context",context);
	    item = context;
	} else if( Item.isEndTag(i)) {
	    context = context.getContext();
	    item = i; 
	} else {
	    item = i;
	}
    }


    public boolean hasItems() {
	return item!=null;
    }


    public Item next() throws IOException {
	Item i = item;
	item = null;
	return i;
    }
}
// arch-tag: 4cdab01d-8cbe-4c68-ad30-a0cd4e9eb3eb
//
