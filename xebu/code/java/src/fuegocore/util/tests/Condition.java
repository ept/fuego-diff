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

package fuegocore.util.tests;

/**
 * A simple utility for use in regression tests. One uses this typically
 * by extending an inner class and defining the method
 * <code>checkCondition</code>. The method <code>waitFor</code> is then
 * called with the minimum time (in milliseconds) the condition should be
 * given to become true.
 * 
 * @author msaarest
 * @version $Id: Condition.java,v 1.2 2006/02/24 15:36:17 jkangash Exp $
 */
public abstract class Condition {

    protected Object optHandle;
    protected Object exitValue = null;
    
    /**
     * A typical constructor for the Condition
     * @param handle An optional object reference that may be helpful in
     *               implementing the checkCondition method.
     */
    public Condition(Object handle) { this.optHandle = handle; }
    
    /**
     * Implement to test whatever condition you like. This method must not
     * block, ever!
     * @return boolean True if the condition check is positive, false if not.
     */
    public abstract boolean checkCondition();
    
    /**
     * Start waiting for the condition to become true. Will call
     * <code> checkCondition </code> repeatedly with exponentialy incresing
     * time intervals. This method will exit when the condition check first
     * becomes true, or when the wait time has been exhausted.
     * 
     * @param minimum The minimum time the checked condition is given time
     *                to become true. The maximum wait time is appr.
     *                <code> minimum*2-1 </code>
     * @return long   The actual period of time if milliseconds before the 
     *                condition became true. If the condition was not true
     *                at all, returns the value of -1.
     */
    public long waitFor(long minimum) {
        long timeOfInvocation = System.currentTimeMillis();
        long interval = 1;
        long now = timeOfInvocation;

        this.exitValue = null;

        do {
            try { Thread.sleep(interval); }
            catch (InterruptedException ignored) { }
            interval*=2;
            
            if (checkCondition()) {
                return now-timeOfInvocation;
            } 
        } while ((now=System.currentTimeMillis())<=timeOfInvocation+minimum);
        
        return -1;
    }

    /**
     * The checkCondition method may optionally set an exit value for
     * further inspection. The exit value is reset to null at each 
     * consecutive call to method waitFor.
     * @return Object
     */    
    public Object getExitValue() { return this.exitValue; }
}
