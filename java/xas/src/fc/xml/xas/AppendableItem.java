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
 * An {@link Item} that can be appended to an {@link ItemTarget}.
 */
public interface AppendableItem {

    /**
         * Append this item into the provided target. This method should produce
         * a sequence of "simpler" items so that infinite recursion is avoided.
         * In particular, an implementation that does
         * <code>target.append(this)</code> is incorrect, as that method call
         * will simply reinvoke this method.
         * 
         * @param target The {@link ItemTarget} to which the produced sequence
         *        is to be appended
         * @throws IOException if the sequence appending fails
         */
    void appendTo (ItemTarget target) throws IOException;

}

// arch-tag: 255aa3c7-3002-4d44-ae12-00415b8822f9
