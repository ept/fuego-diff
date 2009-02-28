/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.index;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import fc.xml.xas.Item;
import fc.xml.xas.StartTag;

/**
 * An index mapping Dewey keys to index entries. This class is essentially a map from
 * {@link DeweyKey} to {@link Index.Entry}. Each index is specific to a certain document. Useful
 * indexes are a <em>complete</em> index, which indexes every node in the XML document, and an
 * <em>element</em> index, which indexes only element nodes. Currently in XAS there is no versioned
 * index, even though both {@link DeweyKey} and {@link Index.Entry} have versioned counterparts.
 */
public class Index {

    private static final long serialVersionUID = 0xDEADL;

    private transient SeekableSource source;
    private Map<DeweyKey, Entry> index;


    private Index() {
    }


    /**
     * Create an empty index based on a source. The new index can be filled using the various
     * <code>insert</code> methods in this class.
     * @param source
     */
    public Index(SeekableSource source) {
        this.source = source;
        this.index = new HashMap<DeweyKey, Entry>();
    }


    public SeekableSource getSource() {
        return source;
    }


    public void setSource(SeekableSource source) {
        this.source = source;
    }


    public Entry find(DeweyKey key) {
        return index.get(key);
    }


    /**
     * Insert an entry with specified start and end offsets.
     * @param key
     *            the {@link DeweyKey} of the new entry
     * @param start
     *            the starting offset of the new entry
     * @param end
     *            the ending offset of the new entry
     * @param context
     *            the processing context of the new entry
     */
    public void insert(DeweyKey key, int start, int end, StartTag context) {
        assert start <= end : String.valueOf(start) + ", " + end + ", " + key + ", " + context;
        index.put(key, new Entry(start, end - start, context));
    }


    /**
     * Insert an entry that was just read. The ending offset of the new entry is the current
     * position of this index's source.
     * @param key
     *            the {@link DeweyKey} of the new entry
     * @param start
     *            the starting offset of the new entry
     * @param context
     *            the processing context of the new entry
     */
    public void insertRead(DeweyKey key, int start, StartTag context) {
        insert(key, start, source.getCurrentPosition(), context);
    }


    /**
     * Insert an entry that is about to be read. The starting offset of the new entry is the current
     * position of this index's source.
     * @param key
     *            the {@link DeweyKey} of the new entry
     * @param end
     *            the ending offset of the new entry
     * @param context
     *            the processing context of the new entry
     */
    public void insertCurrent(DeweyKey key, int end, StartTag context) {
        insert(key, source.getCurrentPosition(), end, context);
    }


    public int size() {
        return index.size();
    }


    @Override
    public String toString() {
        return "In(" + index + ")";
    }


    private static Index build(SeekableSource source, boolean indexAll, int depth)
            throws IOException {
        Index index = new Index(source);
        DeweyKey k = DeweyKey.initial();
        StartTag context = null;
        Stack<StartTag> sts = new Stack<StartTag>();
        sts.push(null);
        Stack<Integer> ps = new Stack<Integer>();
        boolean isText = false;
        Item item;
        while ((item = source.next()) != null) {
            if (isText && !Item.isContent(item)) {
                Integer pos = ps.pop();
                if (indexAll) {
                    index.insert(k, pos, source.getPreviousPosition(), context);
                }
                k = k.next();
                isText = false;
            }
            if (Item.isStartTag(item)) {
                context = (StartTag) item;
                sts.push(context);
                ps.push(source.getPreviousPosition());
                k = k.down();
            } else if (Item.isEndTag(item)) {
                sts.pop();
                context = sts.peek();
                k = k.up();
                Integer pos = ps.pop();
                if (depth >= sts.size()) {
                    index.insert(k, pos, source.getCurrentPosition(), context);
                }
                k = k.next();
            } else if (Item.isContent(item)) {
                if (!isText) {
                    ps.push(source.getPreviousPosition());
                }
                isText = true;
            } else if (Item.isDocumentDelimiter(item)) {
                isText = false;
                continue;
            } else {
                if (indexAll) {
                    index.insert(k, source.getPreviousPosition(), source.getCurrentPosition(),
                                 context);
                }
                k = k.next();
            }
        }
        index.insert(DeweyKey.root(), -1, source.getCurrentPosition() - 1, null);
        return index;
    }


    /**
     * Build a complete index from a source. This method indexes all nodes that can be read from the
     * given source. The source is assumed to be at the start of the document, i.e., the current
     * {@link DeweyKey} is the root key.
     * @param source
     *            the source to index
     * @return a complete index for the source
     * @throws IOException
     *             if reading the source fails for some reason
     */
    public static Index buildFull(SeekableSource source) throws IOException {
        return build(source, true, Integer.MAX_VALUE);
    }


    /**
     * Build a complete index from a source up to a certain depth. This method indexes all nodes
     * that are below the given depth from the given source. The source is assumed to be at the
     * start of the document, i.e., the current {@link DeweyKey} is the root key.
     * @param source
     *            the source to index
     * @param depth
     *            the depth up to which to index
     * @return a depth-limited complete index for the source
     * @throws IOException
     *             if reading the source fails for some reason
     */
    public static Index buildFull(SeekableSource source, int depth) throws IOException {
        return build(source, true, depth);
    }


    /**
     * Build an element index from a source. This method indexes all element nodes that can be read
     * from the given source. The source is assumed to be at the start of the document, i.e., the
     * current {@link DeweyKey} is the root key.
     * @param source
     *            the source to index
     * @return an element index for the source
     * @throws IOException
     *             if reading the source fails for some reason
     */
    public static Index buildElement(SeekableSource source) throws IOException {
        return build(source, false, Integer.MAX_VALUE);
    }


    /**
     * Build an element index from a source up to a certain depth. This method indexes all element
     * nodes that are below the given depth from the given source. The source is assumed to be at
     * the start of the document, i.e., the current {@link DeweyKey} is the root key.
     * @param source
     *            the source to index
     * @param depth
     *            the depth up to which to index
     * @return a depth-limited element index for the source
     * @throws IOException
     *             if reading the source fails for some reason
     */
    public static Index buildElement(SeekableSource source, int depth) throws IOException {
        return build(source, false, depth);
    }

    /**
     * The class representing an index entry. The offsets are otherwise byte-oriented except that
     * the starting offset for an entry representing the complete document is <code>-1</code>
     * instead of <code>0</code> to make it possible to distinguish it from the root element's
     * starting offset.
     */
    public static class Entry {

        private int offset;
        private int length;
        private StartTag context;


        public Entry(int offset, int length, StartTag context) {
            this.offset = offset;
            this.length = length;
            this.context = context;
        }


        /**
         * Get the starting offset of the entry.
         */
        public int getOffset() {
            return offset;
        }


        /**
         * Get the length of the entry.
         */
        public int getLength() {
            return length;
        }


        /**
         * Get the ending offset of the entry.
         */
        public int getEnd() {
            return offset + length;
        }


        public StartTag getContext() {
            return context;
        }


        @Override
        public String toString() {
            return "IE{offset=" + offset + ",length=" + length + ",context=" +
                   (context == null ? "/" : context.getName()) + "}";
        }

    }

}

// arch-tag: c1b72dfd-c02b-4089-8f1b-8c99c5f5b517
