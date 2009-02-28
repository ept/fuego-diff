/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util;

import java.io.IOException;

/**
 * @author Tancred Lindholm
 * @author Jaakko Kangasharju
 */

public interface SeekableInputStream {

    int read() throws IOException;


    int read(byte[] b) throws IOException;


    int read(byte[] b, int off, int len) throws IOException;


    long skip(long n) throws IOException;


    int available() throws IOException;


    void close() throws IOException;


    void mark(int readLimit);


    void reset() throws IOException;


    boolean markSupported();


    void seek(long pos) throws IOException;

}

// arch-tag: 8e2844f8-12e9-4a54-9f90-585faca6b8e5
