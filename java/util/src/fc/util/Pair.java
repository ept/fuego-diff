/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util;

/**
 * 
 * @author Jaakko Kangasharju
 *
 * @param <T>
 * @param <U>
 */

public class Pair<T,U> {

    private T first;
    private U second;

    public Pair (T first, U second) {
	this.first = first;
	this.second = second;
    }

    public T getFirst () {
	return first;
    }

    public U getSecond () {
	return second;
    }

}

// arch-tag: 765d5a6d-a6b0-44a5-8501-802af728ed3f
