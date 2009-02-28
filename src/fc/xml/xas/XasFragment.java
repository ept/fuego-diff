/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * An in-memory fragment representation.
 */
public class XasFragment extends FragmentItem implements Iterable<Item>, Queryable {

    public static final int LIST_FRAGMENT = 0x4200;
    private boolean detached = false;


    public XasFragment(Collection<Item> items, Item firstItem) {
        this(LIST_FRAGMENT, items, firstItem);
    }


    protected XasFragment(int type, Collection<Item> items, Item firstItem) {
        super(type, 1);
        Verifier.checkFragment(items);
        Verifier.checkNotNull(firstItem);
        Verifier.checkNotFragment(firstItem);
        this.items = new ArrayList<Item>(items);
        this.firstItem = firstItem;
    }


    XasFragment(XasFragment fragment, int offset, int length) {
        super(LIST_FRAGMENT, length);
        fragment.checkSublist(offset, length);
        firstItem = fragment.get(offset);
        this.items = fragment.items.subList(offset, offset + length);
    }


    private void checkSublist(int offset, int length) {
        checkIndex(offset);
        Verifier.checkPositive(length);
        checkIndex(offset + length - 1);
    }


    private void checkStart(int offset) {
        Item item = get(offset);
        if (!Item.isStartTag(item)) { throw new IllegalArgumentException(String.valueOf(item) +
                                                                         " at " + offset +
                                                                         " not a start tag"); }
    }


    /*
     * private void checkEnd (int offset) { Item item = get(offset); if (! (item instanceof EndTag))
     * { throw new IllegalArgumentException(String.valueOf(item) + " at " + offset + " not an end
     * tag"); } }
     */
    void setDirect(int index, Item item) {
        checkIndex(index);
        items.set(index, item);
    }


    void set(int index, Item item) {
        checkIndex(index);
        Item i = items.get(index);
        if (Item.isStartTag(i)) {
            convert(index);
            i = items.get(index);
        }
        if (FragmentItem.isFragment(i)) {
            if (FragmentItem.isFragment(item)) {
                ((FragmentItem) item).setSize(((FragmentItem) i).getSize());
            } else {
                WrapperItem w = new WrapperItem(item);
                w.setSize(((FragmentItem) i).getSize());
                item = w;
            }
        } else if (FragmentItem.isFragment(item)) {
            ((FragmentItem) item).setSize(1);
        }
        items.set(index, item);
    }


    void insert(int index, Item item) {
        Verifier.checkBetween(0, index, items.size());
        if (!detached) {
            items = new ArrayList<Item>(items);
            detached = true;
        }
        items.add(index, item);
    }


    Item delete(int index) {
        checkIndex(index);
        // System.out.println("delete(" + index + "),this=" + this +
        // ",detached=" + detached);
        if (!detached) {
            items = new ArrayList<Item>(items);
            detached = true;
        }
        return items.remove(index);
    }


    void move(int source, int target) {
        if (source < target) {
            Item item = get(source);
            insert(target + 1, item);
            delete(source);
        } else if (source > target) {
            insert(target + 1, delete(source));
        }
    }


    public int length() {
        return items.size();
    }


    boolean isValid(int index) {
        return index >= 0 && index < items.size();
    }


    public MutableFragmentPointer pointer() {
        return new MutableFragmentPointer(this, 0);
    }


    Pointer pointer(int index) {
        checkIndex(index);
        return new MutableFragmentPointer(this, index);
    }


    public Iterator<Item> iterator() {
        return new FragmentIterator(this, 0);
    }


    private int elementSize(int start) {
        checkStart(start);
        int depth = 1;
        int i = start + 1;
        while (depth > 0) {
            Item item = get(i);
            if (FragmentItem.isFragment(item)) {
                FragmentItem fi = (FragmentItem) item;
                i += fi.getSize();
                continue;
            } else if (Item.isStartTag(item)) {
                depth += 1;
            } else if (Item.isEndTag(item)) {
                depth -= 1;
            }
            i += 1;
        }
        return i - start;
    }


    boolean convert(int index, int length) {
        checkSublist(index, length);
        Item item = get(index);
        if (Item.isStartTag(item)) {
            Item end = get(index + length - 1);
            if (Item.isEndTag(end) && ((EndTag) end).getName().equals(((StartTag) item).getName())) {
                setDirect(index, new XasFragment(this, index, length));
                return true;
            } else {
                return false;
            }
        } else if (FragmentItem.isFragment(item)) {
            return ((FragmentItem) item).getSize() == length;
        } else {
            return false;
        }
    }


    boolean convert(int index) {
        Item item = get(index);
        if (Item.isStartTag(item)) {
            int subSize = elementSize(index);
            setDirect(index, new XasFragment(this, index, subSize));
            return true;
        } else if (FragmentItem.isFragment(item)) {
            return true;
        } else {
            return false;
        }
    }


    public XasFragment subFragment(int offset, int length) {
        return new XasFragment(this, offset, length);
    }


    public MutableFragmentPointer query(int[] path) {
        return query(pointer(), path);
    }


    MutableFragmentPointer query(MutableFragmentPointer pointer, int[] path) {
        // System.out.println("query(" + pointer + "," + Arrays.toString(path)+
        // ")");
        for (int i = 0; i < path.length; i++) {
            Item item = pointer.get();
            // System.out.println("i=" + i + ",item=" + item);
            if (Item.isStartTag(item) || Item.isStartDocument(item)) {
                pointer.advance();
            } else if (item instanceof XasFragment) {
                pointer = new MutableFragmentPointer((XasFragment) item, 1);
            } else {
                return null;
            }
            for (int j = 0; j < path[i]; j++) {
                pointer.advanceLevel();
            }
            item = pointer.get();
            if (Item.isEndTag(item) || Item.isEndDocument(item)) { return null; }
        }
        // pointer.canonicalize();
        return pointer;
    }


    private int addItem(Item item, ItemTarget target) throws IOException {
        int result = 1;
        if (FragmentItem.isFragment(item)) {
            FragmentItem fi = (FragmentItem) item;
            result = fi.getSize();
        }
        target.append(item);
        return result;
    }


    // private void feed (List<Item> source, int offset, int length,
    // List<Item> target) {
    // for (int i = offset; i < offset + length; ) {
    // Item item = source.get(i);
    // i += addItem(item, target);
    // }
    // }

    public void appendTo(ItemTarget target) throws IOException {
        int n = items.size();
        int i = 0;
        while (i < n) {
            i += addItem(get(i), target);
        }
    }


    public void treeify() {
        ArrayList<Integer> stack = new ArrayList<Integer>();
        int n = items.size();
        for (int i = 0; i < n; i++) {
            Item item = items.get(i);
            if (Item.isStartTag(item)) {
                stack.add(i);
            } else if (Item.isEndTag(item)) {
                Integer si = stack.remove(stack.size() - 1);
                setDirect(si, new XasFragment(this, si, i - si + 1));
            }
        }
    }


    // public XasFragment flattenPure () {
    // List<Item> newItems = new ArrayList<Item>(length);
    // feed(items, offset, length, newItems);
    // return new XasFragment(newItems, 0, newItems.size());
    // }

    // public XasFragment treeifyPure () {
    // ArrayList<Integer> stack = new ArrayList<Integer>();
    // List<Item> newItems = new ArrayList<Item>(length);
    // int j = 0;
    // for (int i = 0; i < length; i++, j++) {
    // Item item = get(i);
    // if (newItems.size() > j) {
    // newItems.set(j, item);
    // } else if (newItems.size() == j) {
    // newItems.add(item);
    // } else {
    // throw new RuntimeException("size=" + newItems.size() + ",j="
    // + j);
    // }
    // if (item instanceof StartTag) {
    // stack.add(j);
    // } else if (item instanceof FragmentItem) {
    // if (((FragmentItem) item).getSize() > 1) {
    // stack.add(-j);
    // }
    // } else if (item instanceof EndTag) {
    // int k = stack.remove(stack.size() - 1);
    // if (k >= 0) {
    // StartTag st = (StartTag) newItems.get(k);
    // newItems.set(k, new FragmentItem
    // (new XasFragment(newItems.subList(k, j + 1)),
    // st, 1));
    // j = k;
    // } else {
    // FragmentItem fi = (FragmentItem) newItems.get(-k);
    // fi.setFragment(new XasFragment(newItems.subList(-k,
    // j + 1)));
    // j = -k;
    // }
    // }
    // }
    // return new XasFragment(newItems, 0, j);
    // }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof XasFragment)) {
            return false;
        } else {
            XasFragment xf = (XasFragment) o;
            if (!firstItem.equals(xf.firstItem)) {
                return false;
            } else if (items.size() != xf.items.size()) {
                return false;
            } else {
                int n = items.size();
                for (int i = 1; i < n; i++) {
                    if (!get(i).equals(xf.get(i))) { return false; }
                }
                return true;
            }
        }
    }


    // FIXME: This is broken, items[0] == this
    public int hashCode() {
        return 37 * firstItem.hashCode() + items.hashCode();
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("XF[" + firstItem);
        for (int i = 1; i < items.size(); i++) {
            sb.append(", ");
            sb.append(get(i));
        }
        sb.append("]");
        return sb.toString();
    }

}

// arch-tag: 92b00c96-b594-47ef-b632-02211c7bd1b9
