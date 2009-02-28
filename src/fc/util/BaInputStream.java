/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author Jaakko Kangasharju
 */
public class BaInputStream extends ByteArrayInputStream implements SeekableInputStream {

    public BaInputStream(byte[] buf) {
        super(buf);
    }


    public BaInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
    }


    public void seek(long pos) throws IOException {
        if (pos < 0 || pos > count) { throw new IOException(String.valueOf(pos) +
                                                            " not between 0 and " + count); }
        this.pos = (int) pos;
    }

}

// arch-tag: 5d98518f-0785-4b99-9184-825aea2e0d82
