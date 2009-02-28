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

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import fc.util.log.Log;

/** Utilities for debugging, such as for dumping objects to printable formats.
 * @author Tancred Lindholm
 * @author Jaakko Kangasharju
 */

// ToPrintable ripped from old fc.util.Util
public class Debug {

    /**  String used for null data.
     * 
     */
    public static final String NULL_AS_STRING = "<null>";
    
    private final static char hexDigits[] = { '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private Debug() {}
    
    /** Get a string describing the object contents suitable for debug output.
     * This method should produce useful debug printouts for almost anything
     * you throw at it. 
     * @param o
     * @return content string
     */
    public static String toString(Object o ) {
	return toString(o,Integer.MAX_VALUE);
    }

    public static String toString(Object o, int level ) {
	if( level == 0 )
	    return "...";
	Class c = o==null ? null : o.getClass(); 
	if ( c != null && c.isArray() ) {
	    if( o instanceof byte[] )
		return Util.toPrintable((byte[]) o);
	    Object [] objs2 = box( o );
	    return toString(objs2,0,objs2.length,level -1);
	}
	return o == null ? NULL_AS_STRING :  o.toString();
    }
    
    public static String toString(Object[] objs, int lo, int hi, int levels ) {
	StringBuilder sb = new StringBuilder();
	String delim = "";
	sb.append("[");
	for(int i=lo;i<hi;i++) {
	    sb.append(delim);
	    Object o = objs[i];
	    sb.append(toString(o,levels-1));
	    delim=", ";
	}
	sb.append("]");
	return sb.toString();
    }

    public static Object[] box(Object o ){
	if( o == null )
	    return null;
	if( o instanceof byte[] ) {
	    byte[] ar= (byte []) o;
	    Byte[] val = new Byte[ar.length];
	    for( int i=0;i<ar.length;i++)
		val[i]=new Byte(ar[i]);
	    return val;
	} else if( o instanceof int[] ) {
	    int[] ar= (int []) o;
	    Integer[] val = new Integer[ar.length];
	    for( int i=0;i<ar.length;i++)
		val[i]=new Integer(ar[i]);
	    return val;
	}  else if( o instanceof long[] ) {
	    long[] ar = (long[]) o;
	    Long[] val = new Long[ar.length];
	    for (int i = 0; i < ar.length; i++)
		val[i] = new Long(ar[i]);
	    return val;
	} else if( o instanceof float[] ) {
	    float[] ar= (float []) o;
	    Float[] val = new Float[ar.length];
	    for( int i=0;i<ar.length;i++)
		val[i]=new Float(ar[i]);
	    return val;
	} else if( o instanceof double[] ) {
	    double[] ar= (double []) o;
	    Double[] val = new Double[ar.length];
	    for( int i=0;i<ar.length;i++)
		val[i]=new Double(ar[i]);
	    return val;
	} else if( o instanceof char[] ) {
	    char[] ar= (char []) o;
	    Character[] val = new Character[ar.length];
	    for( int i=0;i<ar.length;i++)
		val[i]=new Character(ar[i]);
	    return val;
	} else if( o instanceof boolean[] ) {
	    boolean[] ar= (boolean []) o;
	    Boolean[] val = new Boolean[ar.length];
	    for( int i=0;i<ar.length;i++)
		val[i]=new Boolean(ar[i]);
	    return val;
	} else if( o instanceof Object[] )
	    return (Object[]) o;
	return null;
    }
    
    /**
     * Convert an array of bytes to a printable string.  All
     * non-printable byte values are converted to their hexadecimal
     * representations (preceded by <code>%</code> like in URLs).
     *
     * @param buffer the array containing the bytes to convert
     * @param offset the starting index of the bytes to convert
     * @param length the number of the bytes to convert
     * @return a printable string representing the arguments
     */
    public static String toPrintable (byte[] buffer, int offset, int length) {
        StringBuffer value = new StringBuffer();
        for (int i = offset; i < offset + length; i++) {
            if (isPrintable(buffer[i])) {
                value.append((char) buffer[i]);
            } else {
                value.append("%" + toHex(buffer[i]));
            }
        }
        return value.toString();
    }

    /**
     * Convert an array of bytes to a printable string.  All
     * non-printable byte values are converted to their hexadecimal
     * representations (preceded by <code>%</code> like in URLs).
     *
     * @param buffer the array containing the bytes to convert
     * @return a printable string representing the arguments
     */
    public static String toPrintable (byte[] buffer) {
        return toPrintable(buffer, 0, buffer.length);
    }

    /**
     * Convert a string to a printable string.
     *
     * @param string the string to convert
     * @return a printable string representing the arguments
     *
     * @see #toPrintable(byte[],int,int)
     */
    public static String toPrintable (String string) {
        return toPrintable(string.getBytes());
    }  
    	
    /**
     * Check whether a byte value is a printable character.  Currently
     * returns <code>true</code> only for printable ASCII characters.
     *
     * @param b the byte value to check
     * @return whether it is safe to print the character
     */  
    
    public static boolean isPrintable (byte b) {
        return b >= 0x20 && b < 0x7F;
    }
    
    /**
     * Convert a byte to its hexadecimal representation.
     *
     * @param b the byte value to convert
     * @return the representation of <code>b</code> as a pair of
     * hexadecimal digits.
     */

    public static String toHex (byte b) {
        return (new Character(hexDigits[(b & 0xF0) >> 4])).toString()
            + hexDigits[b & 0x0F];
    }

    public static void sleep(long millis) {
    	try { 
    		Thread.sleep(millis);
    	} catch( InterruptedException ex ) {
    		; // Deliberately empty
    	}
    }
    
    public static long usedMemory() {
	Object tester = new Object();
	WeakReference<Object> wr = new WeakReference<Object>(tester);
	tester = null;
	gcLoop(wr);
	return Runtime.getRuntime().totalMemory() - 
		Runtime.getRuntime().freeMemory();
    }
    
    private static void gcLoop(WeakReference<Object> wr) {
	for( int i = 0;i<256;i++) {
	    Util.runGc();
	    if( wr.get() == null ) {
		//Log.debug("Got freed weak-ref at lap "+i);
		return;
	    }
	    sleep(i);
	}
	Log.warning("Test weak reference was not garbage-collected."+
		"Seems we couldn't trigger the gc");
    }

    /** A simple timing system. The point of this class is to easily mark
     * some point in time, and then recall or get the elapsed time since that
     * mark. In particular, your code won't need to keep track of any handles,
     * because you can use existing objects. Also, any timing information is 
     * automatically discarded with its object. 
     * 
     * <p>For something more advanced in terms of control and book-keeping,
     * see the  {@link Measurer} class.
     * 
     * @author ctl
     *
     */
    public static class Time {
		
	private static Stack<Object> anonymous = new Stack<Object>();

	private static Map<Object,Long> timeStamps= 
	    new WeakHashMap<Object,Long>();
	
	public static final long NO_TIME = -1l;
	private static long zero = 0;
	
	public static void zeroIsNow() {
	    zero=System.currentTimeMillis();
	}
	
	/** Record the current time, and associate it with the given handle.
	 * Typically this handle can be <code>this</code> of the calling object.
	 * @param handle
	 * @return last timestamp
	 */ 
	public static long stamp(Object handle) {
	    Long l = timeStamps.put(handle, System.currentTimeMillis());
	    return l == null ? NO_TIME : l;
	}

	/** Stamp and return handle.  The handle will be removed when
	 * calling since() without a handle. These handles work like a stack,
	 * i.e., the latest stamp() handle is destroyed by since() 
	 */

	public static Object stamp() {
	    Object o = new Object();
	    anonymous.push(o);
	    stamp(o);
	    return o;
	}

	
	/** Read timestamp using handle.
	 * 
	 * @param handle
	 * @return timestamp
	 */
	public static long get(Object handle) {
	    Long l = timeStamps.get(handle);
	    return l == null ? NO_TIME : l-zero;
	}
	
	/** Read relative time since stamp. 
	 * 
	 * @param handle
	 * @return relative time
	 */
	
	public static long since(Object handle) {
	    Long l = timeStamps.get(handle);
	    return l == null ? NO_TIME : System.currentTimeMillis()-l;
	}

	/** Read relative time since stamp and remove handle. 
	 * 
	 * @return relative time 
	 */

	public static long since() {
	    return since(anonymous.pop());
	}
	
	/** Get object containing elapsed time. When toString() is called,
	 * the object will create a suitably formatted String.
	 * @param handle
	 * @return object containing elapsed time
	 */ 
	public static Object sinceFmt(Object handle) {
	    final long l = since(handle);
	    return new Object() {
		public String toString()  {
		    return l == -1 ? "n/a" : String.valueOf(l)+" msec";
		}
	    };
	}

	public static Object sinceFmt() {
	    return sinceFmt(anonymous.pop());
	}

    }
    
    /** A simple measurement system.
     * @author ctl
     *
     */
    public static class Measure {

	private static Map<Object,Long> values= 
	    new WeakHashMap<Object,Long>();
	
	public static final long NO_VALUE = -1l;
	private static long bias = 0;
	
	public static void setBias(long zero) {
	    Measure.bias = zero;
	}
	
	/** Record the current time, and associate it with the given handle.
	 * Typically this handle can be <code>this</code> of the calling object.
	 * @param handle
	 * @return last timestamp
	 */ 
	public static long set(Object handle, long val) {
	    Long l = values.put(handle, val);
	    return val;
	}
	
	/** Read value using handle. A timing handle will return a timing
	 * value, as by <code>Time.get</code>.
	 * 
	 * @param handle
	 * @return timestamp
	 */
	public static long get(Object handle) {
	    Long l = values.get(handle);
	    if( l == null ) {
		return Time.get(handle);
	    }
	    return l-bias;
	}
		
	/** Get object containing elapsed time. When toString() is called,
	 * the object will create a suitably formatted String.
	 * @param handle
	 * @return object containing elapsed time
	 */ 
	public static Object getFmt(Object handle) {
	    final long l = get(handle);
	    return new Object() {
		public String toString()  {
		    return l == NO_VALUE ? "n/a" : String.valueOf(l);
		}
	    };
	}

    }
    
}
// arch-tag: c2117146-54e6-4ff3-8935-4f4047d64701
