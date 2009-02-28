/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import fc.util.Util;

public class SerializedFragment extends FragmentItem implements SerializableItem {

    public static final int SERIALIZED = 0x4201;

    private byte[] content;
    private int offset;
    private int length;
    private String type;
    private String encoding;


    public SerializedFragment(String type, String encoding, byte[] content) {
        this(type, encoding, content, 0, content.length);
    }


    public SerializedFragment(String type, String encoding, byte[] content, int offset, int length) {
        super(SERIALIZED, 1);
        Verifier.checkNotNull(type);
        Verifier.checkNotNull(content);
        Verifier.checkOffsetLength(0, offset, length, content.length);
        this.type = type;
        this.encoding = encoding;
        this.content = content;
        this.offset = offset;
        this.length = length;
    }


    public InputStream read() {
        return new ByteArrayInputStream(content, offset, length);
    }


    public void serialize(String type, SerializerTarget target) throws IOException {
        if (this.type.equals(type)) {
            if (encoding == null || encoding.equals(target.getEncoding())) {
                OutputStream out = target.getOutputStream();
                out.write(content, offset, length);
            } else {
                throw new IOException("Encoding " + encoding + "not compatible" +
                                      " with target encoding " + target.getEncoding());
            }
        } else {
            throw new IOException("Requested type " + type + " not compatible" +
                                  " with serialized type " + this.type);
        }
    }


    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("SerF(type=");
        sb.append(type);
        sb.append(",encoding=");
        sb.append(encoding);
        sb.append(",content=");
        sb.append(Util.toPrintable(content, offset, length));
        sb.append(")");
        return sb.toString();
    }

}

// arch-tag: 083bba5e-2c4b-4de9-9cfd-2662add810d6
