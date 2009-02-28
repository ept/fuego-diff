/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util;

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Jaakko Kangasharju
 */
public class ByteArrayStream extends ByteArrayOutputStream {

    public void writeContents(OutputStream out) throws IOException {
        out.write(buf, 0, count);
    }

}

// arch-tag: f3fab20f-8ab4-420a-98b6-a3989c6f60a6
