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
 * An item that can serialize itself. Like {@link AppendableItem}, this
 * interface is implemented by extension items to define how to output them, but
 * unlike it, a {@link SerializableItem} can serialize itself directly as bytes.
 * If this additional capability is not needed, it is better to implement
 * {@link AppendableItem}.
 */
public interface SerializableItem {

    /**
         * Serialize this item, possibly directly as bytes.
         * 
         * @param type the MIME type of the underlying stream, usually having an
         *        "xml" suffix for XML types
         * @param target the target to use for serialization
         * @throws IOException if output fails for some reason
         */
    void serialize (String type, SerializerTarget target) throws IOException;

}

// arch-tag: 55592b43-ae28-496e-a82a-d4eeb5f75f7c
