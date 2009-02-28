/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TargetOutputStream extends FilterOutputStream {

    private static final int STATE_ITEM = 0;
    private static final int STATE_BYTES = 1;

    private int state = STATE_BYTES;
    private SerializerTarget target;


    private void writingBytes() throws IOException {
        if (state == STATE_ITEM) {
            target.flush();
            state = STATE_BYTES;
        }
    }


    public TargetOutputStream(SerializerTarget target, OutputStream out) {
        super(out);
        this.target = target;
    }


    public void wroteItem() {
        state = STATE_ITEM;
    }


    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        writingBytes();
        // BUGFIX-20061212-3: Do not use super.write methods, as these will
        // re-route trough n*write(int), which will completely destroy
        // performance
        out.write(b, off, len);
    }


    @Override
    public void write(byte[] b) throws IOException {
        writingBytes();
        // BUGFIX-20061212-3
        out.write(b);
    }


    @Override
    public void write(int b) throws IOException {
        writingBytes();
        out.write(b);
    }

}

// arch-tag: ab7b512c-6384-4aab-9823-983ebac9a6eb
