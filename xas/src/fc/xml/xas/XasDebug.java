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
import java.io.OutputStream;
import java.io.PrintWriter;

public class XasDebug {
    
    private XasDebug() {
	
    }

    public static ItemTarget itemDump() {
	return itemDump(System.out);
    }
    
    public static ItemTarget itemDump( final OutputStream out ) {
	return new ItemTarget() {

	    int no=0;
	    PrintWriter pw = new PrintWriter(out);
	    
	    public void append(Item item) throws IOException {
		String ns = String.valueOf(no++);
		pw.print("         ".substring(ns.length())+ns+": ");
		pw.println(item);
		pw.flush();
		
	    }
	    
	};

    }
}

// arch-tag: e19e2e2d-79aa-4979-aee9-cac922bbf84e
