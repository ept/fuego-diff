/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util;

import java.util.Map;
import java.util.HashMap;

/**
 * A class for measuring times taken in various operations. The idea is the standard one of taking
 * timestamps both before and after an operation, the duration of which is being measured. What this
 * class does is to make it possible to run several different measurements, of same or different
 * operations, concurrently, collecting total times taken for each type of operation.
 */
public class TimeMeasurer {

    private static final TimeMeasurer defaultInstance = new TimeMeasurer();

    private Map pendingWallTimes;
    private Map totalTimes;
    private Map numberMeasurements;


    /**
     * Get the default measurer instance. This method always returns the same measurer, different
     * from any measurer created by users of this class. Note that this is not a singleton;
     * applications may create other <code>TimeMeasurer</code> instances if they want to. The
     * measurer returned by this method must still be initialized.
     * @return the default measurer instance
     * @see #init
     */
    public static TimeMeasurer getInstance() {
        return defaultInstance;
    }


    /**
     * Initialize a measurer. In production code timing measurements typically only slow down an
     * application. A created measurer, even the default one, remains inactive, accepting no
     * measurements, until this method is called.
     */
    public void init() {
        pendingWallTimes = new HashMap();
        totalTimes = new HashMap();
        numberMeasurements = new HashMap();
    }


    /**
     * Begin a wall clock timing measurement. This method records the current time, and returns a
     * token uniquely identifying this measurement.
     * @return a token identifying this measurement, <code>null</code> if measurer is not active
     * @see #end
     */
    public Object beginWall() {
        Object token = null;
        if (pendingWallTimes != null) {
            token = new Object();
            pendingWallTimes.put(token, new Long(System.currentTimeMillis()));
        }
        return token;
    }


    /**
     * End a timing measurement. This method takes a token returned when a measurement began, and
     * records the time taken in the measured operation under a specified name. The timing records
     * are cumulative, meaning that measured times are added to the existing measurement.
     * @param token
     *            the token identifying the concluded measurement; if <code>null</code> or not
     *            returned by a previous <code>begin</code> call, no record is made
     * @param measurement
     *            the name under which to record this measurement
     * @see #beginWall
     */
    public void end(Object token, String measurement) {
        if (token != null) {
            long end = -1;
            long begin = -1;
            Long beginObj = (Long) pendingWallTimes.get(token);
            if (beginObj != null) {
                end = System.currentTimeMillis();
                begin = beginObj.longValue();
            }
            if (beginObj != null) { // A sanity check
                Long current = (Long) totalTimes.get(measurement);
                if (current == null) {
                    current = new Long(end - begin);
                    numberMeasurements.put(measurement, new Integer(1));
                } else {
                    current = new Long(end - begin + current.longValue());
                    Integer times = (Integer) numberMeasurements.get(measurement);
                    numberMeasurements.put(measurement, new Integer(times.intValue() + 1));
                }
                totalTimes.put(measurement, current);
            }
        }
    }


    /**
     * Get the total time taken by the specified operations. This method returns the cumulative
     * time, in milliseconds, taken by all operations measured with the specified identifier.
     * @param measurement
     *            the name identifying the operation
     * @return the total time taken in the operation
     */
    public long getTime(String measurement) {
        long ret = 0;
        if (totalTimes != null) {
            Long value = (Long) totalTimes.get(measurement);
            if (value != null) {
                ret = value.longValue();
            }
        }
        return ret;
    }


    /**
     * Get the total number of times an operation was measured. This method returns the number of
     * times an operation with the specified identifier was measured.
     * @param measurement
     *            the name identifying the operation
     * @return the total number of times the operation was measured
     */
    public int getNumber(String measurement) {
        int ret = 0;
        if (numberMeasurements != null) {
            Integer value = (Integer) numberMeasurements.get(measurement);
            if (value != null) {
                ret = value.intValue();
            }
        }
        return ret;
    }


    /**
     * Get times for all measurements made. The returned <code>Map</code> has a mapping for each
     * measurement identifier that was measured in a <code>begin()</code>-<code>end()</code> pair
     * with the value a <code>Long</code> signifying the cumulative time in milliseconds taken by
     * the operation.
     */
    public Map getTimes() {
        return totalTimes;
    }


    /**
     * Get the numbers of times for all operations measured. The returned <code>Map</code> has a
     * mapping for each measurement identifier that was measured in a <code>begin()</code>-
     * <code>end()</code> pair with the value an <code>Integer</code> signifying the number of times
     * the operation was measured.
     */
    public Map getNumbers() {
        return numberMeasurements;
    }

}
