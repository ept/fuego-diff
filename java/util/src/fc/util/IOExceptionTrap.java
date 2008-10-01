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

import java.io.IOException;

/** Interface for handling "silent" I/O errors. In some cases, the 
 * implementation of some algorithm may encounter I/O errors, which
 * are not originally a natural part of the algorithm. For instance,
 * a method call shouldn't normally have to be checked for I/O errors. However,
 * if the called implementation has been implemented as a distributed service,
 * this may happen.   
 * <p>In the case of an I/O error, when no one is really anticipated, we need
 * to have a way to still report it. One solution is make an implementation
 * that takes one of these objects, and agrees (by the interface contract) to
 * call the {@link #trap(IOException)} method whenever an I/O error occurs.
 * 
 * @author Tancred Lindholm
 */

public interface IOExceptionTrap {
    
    /** IOException callback. Called with an exception when an I/O error 
     * occurs.
     * @param ex the causing Exception
     */
    public void trap(IOException ex);
    
    /** Trap that throws a {@link IOExceptionTrap.RuntimeIOException}. That
     * exception encapsulates the <code>IOException</code> as the cause.
     * 
     */
    public static final IOExceptionTrap DEFAULT_TRAP = new IOExceptionTrap() {

	public void trap(IOException ex) {
	    throw new RuntimeIOException(ex);
	}
	
    };
    
    public static class RuntimeIOException extends RuntimeException {

	public RuntimeIOException(IOException cause) {
	    super("Unhandled trapped IOException "+cause.getMessage(),cause);
	}
	
    }
}
// arch-tag: d57087e5-f4f5-4cee-8b54-065428d11f26

