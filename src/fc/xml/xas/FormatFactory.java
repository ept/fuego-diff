/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FormatFactory {

    SerializerTarget createCanonicalTarget(OutputStream out) throws IOException;


    ParserSource createSource(InputStream in) throws IOException;


    ParserSource createSource(InputStream in, StartTag context) throws IOException;


    SerializerTarget createTarget(OutputStream out, String encoding) throws IOException;

}

// arch-tag: 70b54d64-4b9b-43a4-a56f-04a3b4f4f959
