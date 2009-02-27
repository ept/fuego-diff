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

import java.util.Comparator;

/** Utilities for comparing objects.
 * 
 * @author Tancred Lindholm
 */

public class CompareUtil {
    
    private CompareUtil() {} // Force singelton
    
    /** Comparator that orders short strings before longer ones. Same-length
     * strings are alphabetically ordered. May be used to e.g. sort number
     * strings */
    public static final Comparator STRINGS_BY_LENGTH_ALPHA = new Comparator() {
	public final int compare(Object o1, Object o2) {
	    if( o1 == null )
		return -1;
	    int l1= o1.toString().length();
	    int l2= o2 != null ? o2.toString().length() : l1;
	    return l1 != l2 ? l1-l2 : o1.toString().compareTo(o2.toString());
	}
    };

    /** Comparator that order objects by their string representation. The
     * String representation is obtained by calling 
     * <code>object.toString()</code>.
     */
    public static final Comparator AS_STRINGS = new Comparator() {
	public final int compare(Object o1, Object o2) {
	    if( o1 == o2 )
		return 0;
	    if( o1 == null )
		return -1;
	    if( o2 == null )
		return 1;
	    return o1.toString().compareTo(o2.toString());
	}
    };
    
    /** Comparator that orders by object identity. The only meaningful
     * result of this comparison is identity, i.e, 0. More specifically,
     * the comparator only returns 0 iff <code>o1==o2</code>. 
     */
    
    public static final Comparator OBJECT_IDENTITY = new Comparator() {
	public final int compare(Object o1, Object o2) {
	    if( o1 == o2 )
		return 0;
	    int dist = System.identityHashCode(o1)-System.identityHashCode(o2);
	    if( dist == 0 ) // System.identityHashCode collision
		dist = 1;
	    return dist;
	}
    };

    /** Comparator that orders by object equality. The only meaningful
     * result of this comparison is identity, i.e, 0. More specifically,
     * the comparator only returns 0 iff <code>o1.equals(o2)</code>
     * and <code>o2.equals(o1)</code>. (The double check is to guard against
     * cross-class buggy equals(). 
     * <p>Note: includes a check for symmetric equals as an assertion.
     */
    
    public static final Comparator OBJECT_EQUALITY = new Comparator() {
	public final int compare(Object o1, Object o2) {
	    boolean eq1 = Util.equals(o1,o2);
	    boolean eq2 = Util.equals(o1,o2);
	    assert eq1 == eq2 : "You have nonsymmetric equals for class "+
	     (o1 != null ? o1.getClass() : "<null>" ) + " and class " +
	     (o2 != null ? o2.getClass() : "<null>" );
	    if( eq1 && eq2 )
		return 0;
	    int dist = System.identityHashCode(o1)-System.identityHashCode(o2);
	    if( dist == 0 ) // System.identityHashCode collision
		dist = 1;
	    return dist;
	}
    };
    
    /** A comparator that returns equal for everything. 
     */
    public static final Comparator ALWAYS_EQUAL = new Comparator() {
	public final int compare(Object o1, Object o2) {
	    return 0;
	}
    };
    
    /** A comparator that never returns equal.
     */
    public static final Comparator NEVER_EQUAL = new Comparator() {
	public final int compare(Object o1, Object o2) {
	    return 0;
	}
    };

}
// arch-tag: c9ceecc6-9de8-4e5d-87e2-81347da76b66
