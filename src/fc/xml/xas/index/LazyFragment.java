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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import fc.util.IOUtil;
import fc.util.Util;
import fc.util.log.Log;
import fc.util.log.LogLevels;
import fc.xml.xas.FragmentItem;
import fc.xml.xas.FragmentPointer;
import fc.xml.xas.Item;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.ParserSource;
import fc.xml.xas.Queryable;
import fc.xml.xas.SerializerTarget;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;
import fc.xml.xas.XasFragment;

/**
 * A fragment that can be created lazily from an external source. A lazy fragment is
 * <em>unforced</em> right after creation, i.e., it does not actually contain any items. There are
 * two different ways to <em>force</em> a fragment, i.e., cause it to parse itself into a sequence
 * of items from the external source. A lazy fragment can be forced <em>partially</em>, in which
 * case some of the items it contains are unforced lazy fragments, or <em>completely</em>, in which
 * case any contained lazy fragments are also completely forced.
 */
public class LazyFragment extends FragmentItem implements Queryable {

    public static final int LAZY_FRAGMENT = 0x2400;

    private Index index;
    private DeweyKey key;


    /**
     * Construct a new lazy fragment.
     * @param index
     *            the index storing the entries corresponding to this fragment's document
     * @param key
     *            the key corresponding to this fragment, absolute from the document's root
     * @param firstItem
     *            the first item in the created fragment
     */
    public LazyFragment(Index index, DeweyKey key, Item firstItem) {
        super(LAZY_FRAGMENT, 1);
        Verifier.checkNotNull(firstItem);
        Verifier.checkNotFragment(firstItem);
        if (Log.isEnabled(LogLevels.TRACE)) {
            Log.log("LazyFragment(" + key + ", " + firstItem + ")", LogLevels.TRACE);
            Log.log("entry=" + index.find(key), LogLevels.TRACE);
        }
        this.index = index;
        this.key = key;
        this.firstItem = firstItem;
    }


    private static void copyStream(InputStream in, String inEncoding, int length, OutputStream out,
                                   String outEncoding) throws IOException {
        if (Log.isEnabled(LogLevels.TRACE)) {
            Log.log("copyStream(" + in + ", " + inEncoding + ", " + length + ", " + out + ", " +
                    outEncoding + ")", LogLevels.TRACE);
        }
        if (inEncoding.equalsIgnoreCase(outEncoding)) {
            IOUtil.copyStream(in, out, length);
            out.flush();
        } else {
            throw new UnsupportedOperationException("Input charset=" + inEncoding +
                                                    ", output charset=" + outEncoding +
                                                    " differ, not implemented yet");
        }
    }


    boolean isEvaluated() {
        return items != null;
    }


    @Override
    public void appendTo(ItemTarget target) throws IOException {
        Verifier.checkNotNull(target);
        if (isEvaluated()) {
            super.appendTo(target);
        } else if (!(target instanceof SerializerTarget)) {
            forceAll();
            super.appendTo(target);
        } else {
            SeekableSource source = index.getSource();
            if (!(source instanceof ParserSource)) {
                forceAll();
                super.appendTo(target);
            } else {
                Index.Entry entry = index.find(key);
                ParserSource ps = (ParserSource) source;
                SerializerTarget st = (SerializerTarget) target;
                st.flush();
                source.setPosition(entry.getOffset(), entry.getContext());
                copyStream(ps.getInputStream(), ps.getEncoding(), entry.getLength(),
                           st.getOutputStream(), st.getEncoding());
                source.setPosition(entry.getEnd(), entry.getContext());
            }
        }
    }


    /**
     * Force this fragment completely.
     * @throws IOException
     *             if parsing the external source during forcing fails
     */
    public void forceAll() throws IOException {
        force(Integer.MAX_VALUE);
    }


    /**
     * For this fragment partially up to a given depth. Any contents below the given depth will be
     * left unforced.
     * @param depth
     *            the maximum depth to be forced, <code>0</code> is the root of this fragment (and
     *            hence <code>force(0)</code> has no effect)
     * @throws IOException
     *             if parsing the external source during forcing fails
     */
    public void force(int depth) throws IOException {
        if (Log.isEnabled(LogLevels.TRACE)) {
            Log.log("force(" + depth + ")", LogLevels.TRACE);
        }
        if (depth <= 0 || isEvaluated()) { return; }
        items = new ArrayList<Item>();
        int current = 0;
        DeweyKey k = key;
        Stack<StartTag> stack = new Stack<StartTag>();
        boolean isText = false;
        SeekableSource source = index.getSource();
        Index.Entry entry = index.find(key);
        if (Log.isEnabled(LogLevels.TRACE)) {
            Log.log("entry=" + entry, LogLevels.TRACE);
        }
        source.setPosition(entry.getOffset(), entry.getContext());
        int end = entry.getEnd();
        while (source.getCurrentPosition() < end) {
            if (Log.isEnabled(LogLevels.TRACE)) {
                Log.log("force, pos=" + source.getCurrentPosition() + ", end=" + end,
                        LogLevels.TRACE);
            }
            if (current < depth) {
                Item item = source.next();
                if (Log.isEnabled(LogLevels.TRACE)) {
                    Log.log("Item: " + item, LogLevels.TRACE);
                }
                if (Item.isStartTag(item)) {
                    stack.push((StartTag) item);
                    k = k.down();
                    current += 1;
                    isText = false;
                } else if (Item.isEndTag(item)) {
                    stack.pop();
                    k = k.up().next();
                    current -= 1;
                    isText = false;
                } else if (Item.isContent(item)) {
                    if (!isText) {
                        k = k.next();
                    }
                    isText = true;
                } else if (Item.isStartDocument(item)) {
                    k = k.down();
                } else if (Item.isEndDocument(item)) {
                    k = k.up();
                    if (!k.isRoot()) { throw new IOException("Reached EndDocument at key " + k); }
                } else {
                    k = k.next();
                    isText = false;
                }
                items.add(item);
            } else {
                passUntil(k, stack.empty() ? null : stack.peek(), end);
                source.setPosition(source.getCurrentPosition(), entry.getContext());
                current -= 1;
                k = k.up();
            }
        }
    }


    private void passLevel(DeweyKey k, StartTag context, int end) throws IOException {
        if (Log.isEnabled(LogLevels.TRACE)) {
            Log.log("passLevel(" + k + ", " + end + ")", LogLevels.TRACE);
        }
        SeekableSource source = index.getSource();
        boolean isText = false;
        int depth = 1;
        while (depth > 0 && source.getCurrentPosition() < end) {
            Item item = source.next();
            if (isText && !Item.isContent(item)) {
                k = k.next();
                isText = false;
            }
            if (Item.isStartTag(item)) {
                isText = false;
                Index.Entry entry = index.find(k);
                if (entry != null) {
                    items.add(new LazyFragment(index, k, item));
                    while (source.getCurrentPosition() < entry.getEnd()) {
                        source.next();
                    }
                    k = k.next();
                    continue;
                }
                k = k.down();
                depth += 1;
            } else if (Item.isEndTag(item)) {
                k = k.up().next();
                isText = false;
                depth -= 1;
            } else if (Item.isContent(item)) {
                if (!isText) {
                    Index.Entry entry = index.find(k);
                    if (entry != null) {
                        items.add(new LazyFragment(index, k, item));
                        while (source.getCurrentPosition() < entry.getEnd()) {
                            source.next();
                        }
                        k = k.next();
                        isText = false;
                        continue;
                    }
                }
                isText = true;
            } else {
                k = k.next();
                isText = false;
            }
            items.add(item);
        }
    }


    private void passUntil(DeweyKey k, StartTag context, int end) throws IOException {
        if (Log.isEnabled(LogLevels.TRACE)) {
            Log.log("passUntil(" + k + ", " + context + ", " + end + ")", LogLevels.TRACE);
        }
        SeekableSource source = index.getSource();
        boolean isText = false;
        while (source.getCurrentPosition() < end) {
            Item item = source.next();
            if (Log.isEnabled(LogLevels.TRACE)) {
                Log.log("passUntil, item=" + item, LogLevels.TRACE);
            }
            if (isText && !Item.isContent(item)) {
                k = k.next();
                isText = false;
            }
            if (Item.isStartTag(item)) {
                Index.Entry entry = index.find(k);
                if (entry != null) {
                    if (Log.isEnabled(LogLevels.TRACE)) {
                        Log.log("context=" + entry.getContext(), LogLevels.TRACE);
                    }
                    items.add(new LazyFragment(index, k, item));
                    source.setPosition(entry.getEnd(), context);
                } else {
                    items.add(item);
                    passLevel(k.down(), (StartTag) item, end);
                }
                k = k.next();
                isText = false;
                continue;
            } else if (Item.isEndTag(item)) {
                items.add(item);
                break;
            } else if (Item.isContent(item)) {
                if (!isText) {
                    Index.Entry entry = index.find(k);
                    if (entry != null) {
                        items.add(new LazyFragment(index, k, item));
                        source.setPosition(entry.getEnd(), context);
                        isText = false;
                        k = k.next();
                        continue;
                    }
                }
                isText = true;
            } else {
                k = k.next();
                isText = false;
            }
            items.add(item);
        }
    }


    private FragmentItem forceLevel(List<DeweyKey> l, int i) throws IOException {
        if (Log.isEnabled(LogLevels.TRACE)) {
            Log.log("forceLevel(" + l + ", " + i + ")", LogLevels.TRACE);
        }
        if (i == 0) {
            DeweyKey k = l.get(0);
            Index.Entry e = index.find(k);
            SeekableSource source = index.getSource();
            source.setPosition(e.getOffset(), e.getContext());
            Item item = source.next();
            LazyFragment f = new LazyFragment(index, k, item);
            f.force(1);
            items.add(f);
            return f;
        } else {
            DeweyKey k = l.get(i);
            DeweyKey c = l.get(i - 1);
            Index.Entry e = index.find(k);
            Index.Entry ce = index.find(c);
            if (Log.isEnabled(LogLevels.TRACE)) {
                Log.log("Key=" + k + ",entry=" + e, LogLevels.TRACE);
                Log.log("Child=" + c + ",entry=" + ce, LogLevels.TRACE);
            }
            SeekableSource source = index.getSource();
            source.setPosition(e.getOffset(), e.getContext());
            items.add(source.next());
            passUntil(k.down(), ce.getContext(), ce.getOffset());
            FragmentItem result = forceLevel(l, i - 1);
            if (Log.isEnabled(LogLevels.TRACE)) {
                Log.log("Key=" + k + ",entry=" + e, LogLevels.TRACE);
                Log.log("Child=" + c + ",entry=" + ce, LogLevels.TRACE);
            }
            source.setPosition(ce.getEnd(), ce.getContext());
            passUntil(c.next(), ce.getContext(), e.getEnd());
            return result;
        }
    }


    /**
     * Force this fragment partially up to a certain key. This method ensures that the fragment
     * identified by the given key will be forced. Otherwise the forcing is minimal, i.e., any
     * indexed subtree of the document whose key is not a prefix of the given key will be left
     * unforced.
     * @param k
     *            the key of the node to guarantee is forced
     * @return the forced fragment, if this fragment was unforced previously, <code>null</code>
     *         otherwise
     * @throws IOException
     *             if parsing the external source during forcing fails
     */
    public FragmentItem force(DeweyKey k) throws IOException {
        if (Log.isEnabled(LogLevels.TRACE)) {
            Log.log("force(" + k + "), key=" + key, LogLevels.TRACE);
        }
        if (isEvaluated()) {
            return null;
        } else if (Util.equals(k, key)) {
            force(1);
            return this;
        } else if (k.isDescendant(key)) {
            items = new ArrayList<Item>();
            if (index.find(k) != null) {
                List<DeweyKey> keys = new ArrayList<DeweyKey>();
                keys.add(k);
                do {
                    k = k.up();
                    keys.add(k);
                } while (!Util.equals(k, key));
                return forceLevel(keys, keys.size() - 1);
            } else {
                return null;
            }
        } else {
            throw new IllegalArgumentException("Argument key " + k + " not in subtree of key " +
                                               key);
        }
    }


    public LazyPointer pointer() {
        return pointer(0);
    }


    LazyPointer pointer(int index) {
        checkIndex(index);
        return new LazyPointer(this, index);
    }


    public FragmentPointer query(int[] path) {
        return query(pointer(), path);
    }


    FragmentPointer query(FragmentPointer pointer, int[] path) {
        if (Log.isEnabled(LogLevels.TRACE)) {
            Log.log("query(" + Arrays.toString(path) + "),this=" + this, LogLevels.TRACE);
        }
        int offset = key.size();
        if (!isEvaluated()) {
            try {
                force(DeweyKey.construct(path));
            } catch (IOException ex) {
                Log.warning("Query failed", ex);
                return null;
            }
        }
        for (int i = offset; i < path.length; i++) {
            Item item = pointer.get();
            if (Item.isStartItem(item)) {
                pointer.advance();
            } else if (item instanceof LazyFragment) {
                LazyFragment lf = (LazyFragment) item;
                if (!lf.isEvaluated()) {
                    try {
                        if (lf.force(DeweyKey.construct(path)) == null) { return null; }
                    } catch (IOException ex) {
                        Log.warning("Query failed", ex);
                        return null;
                    }
                }
                pointer = new LazyPointer(lf, 1);
            } else if (item instanceof XasFragment) {
                pointer = ((XasFragment) item).pointer();
                pointer.advance();
            } else {
                return null;
            }
            for (int j = 0; j < path[i]; j++) {
                pointer.advanceLevel();
            }
            item = pointer.get();
            if (Log.isEnabled(LogLevels.TRACE)) {
                Log.log("Item(" + i + "," + path[i] + ")=" + item, LogLevels.TRACE);
            }
            if (Item.isEndItem(item)) { return null; }
        }
        pointer.canonicalize();
        return pointer;
    }


    /**
     * Unforce this fragment.
     */
    public void unforce() {
        items = null;
    }


    public Index getIndex() {
        return index;
    }


    public DeweyKey getKey() {
        return key;
    }


    @Override
    public String toString() {
        return "LF(" + key + "," + firstItem + "," +
               (isEvaluated() ? "size=" + items.size() : " (unforced)") + ")";
    }

}

// arch-tag: 9aff16e9-ac4a-4682-a309-3bd12d590e52
