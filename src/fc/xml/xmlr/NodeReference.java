/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xmlr;

/**
 * A node reference.
 */
public class NodeReference implements Reference {

    private Key target;


    /**
     * Create new instance.
     * @param target
     *            reference target
     */
    public NodeReference(Key target) {
        this.target = target;
    }


    /**
     * Create new instance.
     * @param k
     *            reference target
     * @return instance
     */
    public static NodeReference create(Key k) {
        return new NodeReference(k);
    }


    /** @inheritDoc */
    public Key getTarget() {
        return target;
    }


    /** Returns false. */
    public final boolean isTreeReference() {
        return false;
    }

}

// arch-tag: c916d3c2-ac53-466a-94ce-70f9391ba19c
