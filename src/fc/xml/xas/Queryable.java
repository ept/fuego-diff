/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

/**
 * An interface for XAS structures that support querying. This interface should be implemented by
 * any class that supports tree-structured access to an XML document. The methods are a variety of
 * different ways to access substructures by queries.
 */
public interface Queryable {

    /**
     * Query structure by Dewey key. This method returns a pointer to the item located by following
     * the Dewey key given as an argument. The search is relative, i.e., treats the root of this
     * structure as the overall root.
     * @param path
     *            the key to search for, represented in [@link DeweyKey} array form
     * @return a pointer accessed through the key, or <code>null</code> if the structure does not
     *         contain the argument path
     */
    Pointer query(int[] path);

}

// arch-tag: e4744354-eeb0-4ce4-85e2-970ebf44b4bb
