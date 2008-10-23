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

package fc.xml.xas;

import java.io.IOException;

/**
 * A wrapper source for easy transformations. This class wraps an underlying
 * {@link ItemSource} and uses an {@link ItemTransform} to transform the items
 * coming from the source. While this class works in all transformation
 * situations, special-purpose wrappers may provide more efficiency.
 */
public class TransformSource implements ItemSource {

    private ItemSource source;
    private ItemTransform transform;

    public TransformSource (ItemSource source, ItemTransform transform) {
	Verifier.checkNotNull(source);
	Verifier.checkNotNull(transform);
	this.source = source;
	this.transform = transform;
    }

    public Item next () throws IOException {
	Item item = null;
	while (!transform.hasItems() && ((item = source.next()) != null)) {
	    transform.append(item);
	}
	if (transform.hasItems()) {
	    item = transform.next();
	}
	return item;
    }

}

// arch-tag: cbf4219f-7e70-4c5d-98fc-5daed024e00c
