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

package fc.xml.xas.index;

import java.io.IOException;

import fc.util.log.Log;
import fc.xml.xas.FragmentPointer;
import fc.xml.xas.Item;
import fc.xml.xas.Pointer;

public class LazyPointer extends FragmentPointer {

    private LazyFragment lazyFragment;

    LazyPointer (LazyFragment fragment, int index) {
	super(fragment, index);
	lazyFragment = fragment;
    }

    public Item get () {
	return fragment.get(index);
    }

    public Pointer query (int[] path) {
	return lazyFragment.query(new LazyPointer(lazyFragment, index), path);
    }

    public void canonicalize () {
	Item item = get();
	while (item instanceof LazyFragment) {
	    lazyFragment = (LazyFragment) item;
	    try {
		lazyFragment.force(1);
	    } catch (IOException ex) {
		Log.warning("Lazy pointer canonicalization failed", ex);
		throw (RuntimeException) new RuntimeException(ex.getMessage())
		    .initCause(ex);
	    }
	    fragment = lazyFragment;
	    index = 0;
	    item = get();
	}
    }

}

// arch-tag: ecd10740-a044-475f-a058-ae60f632c04f
