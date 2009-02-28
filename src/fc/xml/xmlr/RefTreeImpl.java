/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

// $Id: RefTreeImpl.java,v 1.3 2004/11/22 21:18:16 ctl Exp $
package fc.xml.xmlr;

/**
 * Default implementation of {@link RefTree}.
 */

public class RefTreeImpl implements RefTree {

    protected RefTreeNode root;


    /**
     * Create a new reftree.
     * @param root
     *            root of the reftree
     */
    public RefTreeImpl(RefTreeNode root) {
        this.root = root;
    }


    public RefTreeNode getRoot() {
        return root;
    }
}
// arch-tag: db2b79aa0b4ca2a07e645fcaa608ba50 *-
