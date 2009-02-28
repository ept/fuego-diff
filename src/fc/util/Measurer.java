/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util;

import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;

/**
 * @author Jaakko Kangasharju
 */
public final class Measurer {

    public static final int TIMING = 0;
    public static final int MEMORY = 1;
    private static final int LAST = 2;

    private static final Measurer[] measurers = new Measurer[LAST];
    private static final Enumeration emptyKeys = new EmptyEnumeration();

    static {
        for (int i = 0; i < measurers.length; i++) {
            measurers[i] = new Measurer(i);
        }
    }

    private final int type;
    private Hashtable pending = null;
    private Hashtable finished = null;
    private Object lastToken = null;


    private Measurer(int type) {
        this.type = type;
    }


    private void init() {
        pending = new Hashtable();
        finished = new Hashtable();
    }


    public static void initAll() {
        for (int i = 0; i < measurers.length; i++) {
            init(i);
        }
    }


    public static void init(int type) {
        measurers[type].init();
    }


    public static Measurer get(int type) {
        return measurers[type];
    }


    public String getName() {
        switch (type) {
            case TIMING:
                return "Timer";
            case MEMORY:
                return "Memory";
            default:
                return "";
        }
    }


    private long measure() {
        switch (type) {
            case TIMING:
                return System.currentTimeMillis();
            case MEMORY:
                return Util.usedMemory();
            default:
                return 0;
        }
    }


    public Object start() {
        if (pending != null) {
            Object token = new Object();
            lastToken = token;
            LongHolder holder = new LongHolder();
            pending.put(token, holder);
            holder.set(measure());
            return token;
        } else {
            return null;
        }
    }


    public void finishCurrent(Object key) {
        finish(lastToken, key);
    }


    public void finish(Object token, Object key) {
        if (token != null) {
            long current = measure();
            if (pending != null && token != null) {
                LongHolder holder = (LongHolder) pending.remove(token);
                if (holder != null) {
                    long diff = current - holder.get();
                    if (diff >= 0) {
                        LongArray la = (LongArray) finished.get(key);
                        if (la == null) {
                            la = new LongArray();
                            finished.put(key, la);
                        }
                        la.add(diff);
                    }
                }
            }
            if (token == lastToken) {
                lastToken = null;
            }
        }
    }


    public void reset(Object key) {
        LongArray la = (LongArray) finished.get(key);
        if (la != null) {
            la.reset();
        }
    }


    public Enumeration keys() {
        if (finished != null) {
            return finished.keys();
        } else {
            return emptyKeys;
        }
    }


    /**
     * @deprecated A drop-in replacement is {@link #outputSummary(PrintStream)} but
     *             {@link #outputFull(PrintStream)} is preferable
     */
    public void output(PrintStream out) {
        outputSummary(out);
    }


    public void outputSummary(PrintStream out) {
        if (finished != null) {
            for (Enumeration e = keys(); e.hasMoreElements();) {
                Object k = e.nextElement();
                LongArray la = (LongArray) finished.get(k);
                int n = la.getSize();
                if (n > 0) {
                    long sum = 0;
                    long sumSq = 0;
                    for (int i = 0; i < n; i++) {
                        long v = la.getValue(i);
                        sum += v;
                        sumSq += v * v;
                    }
                    out.println(String.valueOf(k) + ": " + sum + " " + sumSq + " (" + n + ")");
                }
            }
        }
    }


    public void outputFull(PrintStream out) {
        if (finished != null) {
            for (Enumeration e = keys(); e.hasMoreElements();) {
                Object k = e.nextElement();
                LongArray la = (LongArray) finished.get(k);
                int n = la.getSize();
                if (n > 0) {
                    out.print(String.valueOf(k) + ":");
                    for (int i = 0; i < n; i++) {
                        out.print(' ');
                        out.print(la.getValue(i));
                    }
                    out.println();
                }
            }
        }
    }

    private static final class LongHolder {

        private long value;


        public LongHolder() {
            this(0);
        }


        public LongHolder(long value) {
            this.value = value;
        }


        public long get() {
            return value;
        }


        public void set(long value) {
            this.value = value;
        }

    }

    private static final class LongArray {

        private static final int[] SIZES = { 20, 50, 100, 250 };

        private long[] values;
        private int size;
        private int sizeIndex;


        public LongArray() {
            sizeIndex = 0;
            values = new long[SIZES[sizeIndex]];
            size = 0;
        }


        private void expand(int newSize) {
            long[] newValues = new long[newSize];
            System.arraycopy(values, 0, newValues, 0, size);
            values = newValues;
        }


        private void ensureFit() {
            if (size >= values.length) {
                if (sizeIndex < SIZES.length - 1) {
                    sizeIndex += 1;
                    expand(SIZES[sizeIndex]);
                } else {
                    expand(2 * values.length);
                }
            }
        }


        public void add(long value) {
            ensureFit();
            values[size++] = value;
        }


        public int getSize() {
            return size;
        }


        public long getValue(int index) {
            return values[index];
        }


        public void reset() {
            size = 0;
        }

    }

    private static final class EmptyEnumeration implements Enumeration {

        public boolean hasMoreElements() {
            return false;
        }


        public Object nextElement() {
            throw new NoSuchElementException("Enumeration is empty");
        }

    }

}

// arch-tag: edb0832b-14c1-46be-9260-0a6fecae3494
