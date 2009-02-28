/*
 * Copyright 2006 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.message.encoding;

/**
 * A cache mapping objects to small integers.  This class provides a
 * mapping of objects to integers in the way that an insert of a new
 * element into a full mapping removes an existing association and
 * returns the integer connected to that.
 */
public interface OutTokenCache {

    /**
     * Insert an object into the cache.  The returned value is the
     * integer this object is mapped into after the insertion.  It is
     * guaranteed that the returned values will be a consecutive
     * sequence of integers starting from <code>0</code> until the
     * cache is full, after which no guarantees are given.
     *
     * @param key the object to insert
     * @return the integer that <code>key</code> now maps to
     */
    int insert (Object key);

    /**
     * Fetch an object's corresponding integer from a cache.  The
     * returned value is also used for signaling errors; it will be -1
     * if <code>key</code> is not found in the cache.
     *
     * @param key the object to look for
     * @return the integer <code>key</code> maps to, or -1 if
     * <code>key</code> has no mapping
     */
    int fetch (Object key);

    /**
     * Fetch an object corresponding to a given value.  This is the
     * reverse of the ordinary mapping and is useful when needing to
     * access the non-cached form of objects.
     *
     * @param value the value to look for
     * @return the object that maps to <code>value</code>, or
     * <code>null</code> if no object maps to <code>value</code>.
     */
    Object fetchReverse (int value);

}
