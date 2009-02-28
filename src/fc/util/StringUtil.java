/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util;

import java.util.Arrays;
import java.util.LinkedList;

/**
 * @author Tancred Lindholm
 * @author Eemil Lagerspetz
 */
public class StringUtil {

    private StringUtil() {
    }


    /**
     * Format string to given width using spaces.
     * @param s
     *            string to format
     * @param width
     *            magnitude of width of output string, if &lt; 0 <code>s</code> is adjusted right.
     */

    public static String format(String s, int width) {
        return format(s, width, ' ');
    }


    /**
     * Format string to given width.
     * @param s
     *            string to format
     * @param width
     *            magnitude of width of output string, if &lt; 0 <code>s</code> is adjusted right.
     * @param filler
     *            character to use as filler.
     */

    public static String format(String s, int width, char filler) {
        StringBuffer buf = new StringBuffer();
        if (s == null) s = "(null)";
        int strlen = s.length();
        int fill = Math.abs(width) - strlen;
        if (fill > 0 && width < 0) for (; fill-- > 0; buf.append(filler))
            ;
        buf.append(s);
        if (fill > 0 && width > 0) for (; fill-- > 0; buf.append(filler))
            ;
        return buf.toString();
    }


    public static String toString(Object[] array, String delim) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(delim);
            sb.append(array[i]);
        }
        return sb.toString();
    }


    public static String[] split(String s, char ch) {
        if (s == null) return null;
        int count = 1;
        for (int i = -1; (i = s.indexOf(ch, i + 1)) != -1;)
            count++;
        String[] split = new String[count];
        int item = 0;
        int start = 0;
        int end = 0;
        do {
            end = s.indexOf(ch, start);
            if (end == -1) {
                split[item] = s.substring(start);
            } else {
                split[item++] = s.substring(start, end);
                start = end + 1;
            }
        } while (end != -1);
        return split;
    }


    /**
     * Split into words on whitespace.
     * @param s
     *            String to split
     * @return array of split words
     */
    // emulate by regexp [[:space:]]+
    public static final String[] splitWords(String s) {
        return splitWords(s, true, null);
    }


    /**
     * Split into words.
     * @param s
     *            String to split
     * @param splitOnWs
     *            whitespace causes split
     * @param delimiters
     *            array of other chars causing a split, must be sorted in ascending order, e.g.
     *            <code>{',','.'}</code>
     * @return array of words
     */
    public static final String[] splitWords(String s, boolean splitOnWs, char[] delimiters) {
        if (s == null) return null;
        int cstart = -1, // -1 = scanning space, other = startpos of content
        len = s.length();
        assert delimiters == null || _splitWords_assert(delimiters);
        LinkedList l = new LinkedList();
        for (int pos = 0; pos < len; pos++) {
            char ch = s.charAt(pos);
            if (cstart < 0) {
                if (splitOnWs && (ch == 0x0a || Character.isSpaceChar(ch))) continue;
                if (delimiters != null && Arrays.binarySearch(delimiters, ch) > -1) continue;
                cstart = pos;
            } else {
                boolean endOfWord = false;
                if (splitOnWs && ((ch == 0x0a || Character.isSpaceChar(ch)))) {
                    endOfWord = true;
                }
                if (delimiters != null && Arrays.binarySearch(delimiters, ch) > -1) {
                    endOfWord = true;
                }
                if (!endOfWord) {
                    continue;
                }
                l.add(s.substring(cstart, pos));
                cstart = -1;
            }
        }
        if (cstart >= 0) l.add(s.substring(cstart)); // Emit final token
        return (String[]) l.toArray(new String[l.size()]);
    }


    /**
     * Split into words.
     * @param s
     *            String to split
     * @param splitOnWs
     *            whitespace causes split
     * @param delimiters
     *            array of other chars causing a split, must be sorted in ascending order, e.g.
     *            <code>{',','.'}</code>
     * @return A LinkedList of Strings, the words <code>s</code> was split to
     */
    public static final LinkedList splitWordsL(String s, boolean splitOnWs, char[] delimiters) {
        if (s == null) return null;
        int cstart = -1, // -1 = scanning space, other = startpos of content
        len = s.length();
        // A! assert _splitWords_assert(delimiters);
        LinkedList l = new LinkedList();
        for (int pos = 0; pos < len; pos++) {
            char ch = s.charAt(pos);
            if (cstart < 0) {
                if (splitOnWs && (ch == 0x0a || Character.isSpaceChar(ch))) continue;
                if (delimiters != null && Arrays.binarySearch(delimiters, ch) > -1) continue;
                cstart = pos;
            } else {
                boolean endOfWord = false;
                if (splitOnWs && ((ch == 0x0a || Character.isSpaceChar(ch)))) {
                    endOfWord = true;
                }
                if (delimiters != null && Arrays.binarySearch(delimiters, ch) > -1) {
                    endOfWord = true;
                }
                if (!endOfWord) {
                    continue;
                }
                l.add(s.substring(cstart, pos));
                cstart = -1;
            }
        }
        if (cstart >= 0) l.add(s.substring(cstart)); // Emit final token
        return l;
    }


    private static boolean _splitWords_assert(char[] ch) {
        for (int i = 1; i < ch.length; i++)
            assert ch[i - 1] <= ch[i] : "Array should be sorted";
        return true;
    }


    public static Options getOptions(String[] args) {
        return new Options(args);
    }

    public static class Options {

        private int errors = 0;
        private String[] args;


        public Options(String[] opts) {
            args = new String[opts.length];
            System.arraycopy(opts, 0, args, 0, opts.length);
        }


        public boolean getOpt(String name, boolean def) {
            String s = findOpt(name, 1, true);
            if (s == null) return def;
            return s.startsWith("--no") ? false : true;
        }


        public long getOpt(String name, long def) {
            String s = findOpt(name, 2, false);
            if (s == null) return def;
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                errors++;
            }
            return -1;
        }


        public String getOpt(String name, String def) {
            String s = findOpt(name, 2, false);
            if (s == null) return def;
            return s;
        }


        public String[] getUnused() {
            int len = 0;
            for (int i = 0; i < args.length; i++)
                len += args[i] != null ? 1 : 0;
            String[] unused = new String[len];
            len = 0;
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) unused[len++] = args[i];
            }
            return unused;
        }


        private String findOpt(String opt, int len, boolean noOpt) {
            String nopt = opt.length() == 1 || !noOpt ? null : "--no" + opt;
            opt = (opt.length() == 1 ? "-" : "--") + opt;
            int findix = -1;
            int argc = 0;
            boolean isOpt;
            for (int i = 0; i < args.length && findix == -1; i++) {
                if (args[i] != null &&
                    ((isOpt = args[i].equals(opt)) || (args[i].equals(nopt) && !(isOpt = false)))) {
                    findix = i;
                    if (args.length < i + len) {
                        errors++;
                        return null;
                        // throw new IOException("Mssing value for "+opt);
                    }
                    String s = args[i + len - 1];
                    for (int j = 0; j < len; j++)
                        args[i + j] = null;
                    return s;
                }
            }
            return null;
        }


        public int getErrors() {
            return errors;
        }
    }

}

// arch-tag: 7c03cee0-4b6c-4c81-8044-c81184db76ef
