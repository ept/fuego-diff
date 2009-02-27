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
 * A wrapper target for easy transformations. This class wraps an underlying
 * {@link ItemTarget} and uses an {@link ItemTransform} to transform the items
 * being output to the target. While this class works in all transformation
 * situations, special-purpose wrappers may provide more efficiency.
 */
public class TransformTarget implements ItemTarget {

    private ItemTarget target;
    private ItemTransform transform;

    public TransformTarget (ItemTarget target, ItemTransform transform) {
	Verifier.checkNotNull(target);
	Verifier.checkNotNull(transform);
	this.target = target;
	this.transform = transform;
    }

    public void append (Item item) throws IOException {
	transform.append(item);
	while (transform.hasItems()) {
	    target.append(transform.next());
	}
    }

}

// arch-tag: ed5aefb4-b784-47d9-83f0-2816a6a433eb
