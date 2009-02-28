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
import java.util.List;

/**
 * An item representing a sequence of items. A fragment has two size-like properties. Its length is
 * the number of items that it contains. If it is contained in another fragment, its size is the
 * number of items that it contains in its parent fragment. Therefore the expression
 * <code>i + f.getSize()</code> for a fragment <code>f</code> at index <code>i</code> gives the
 * index of the item following <code>f</code>.
 */
public abstract class FragmentItem extends Item implements AppendableItem {

    private static final int FRAGMENT_TYPE = 0x190000;

    protected List<Item> items;
    protected Item firstItem;
    private int size;


    protected FragmentItem(int type, int size) {
        super(FRAGMENT_TYPE | (type & ~CLASS_MASK));
        this.size = size;
    }


    public static boolean isFragment(Item i) {
        return hasClass(i, FRAGMENT_TYPE);
    }


    public int length() {
        return items.size();
    }


    public int getSize() {
        return size;
    }


    protected void setSize(int size) {
        this.size = size;
    }


    protected void checkIndex(int index) {
        if (index == 0) {
            if (firstItem == null) { throw new IllegalArgumentException("Fragment does not have "
                                                                        + "item 0"); }
        } else if (items == null) {
            throw new IllegalArgumentException("Fragment does not have items"
                                               + " other than the first");
        } else if (index < 0 || index >= items.size()) { throw new IllegalArgumentException(
                                                                                            String.valueOf(index) +
                                                                                                    " not in (0, " +
                                                                                                    items.size() +
                                                                                                    ")"); }
    }


    public Item get(int index) {
        checkIndex(index);
        if (index == 0) {
            return firstItem;
        } else {
            return items.get(index);
        }
    }


    public void appendTo(ItemTarget target) throws IOException {
        if (firstItem != null) {
            target.append(firstItem);
        }
        if (items != null) {
            int n = items.size();
            for (int i = 1; i < n; i++) {
                target.append(items.get(i));
            }
        }
    }


    /*
     * XXX - the following two methods will disappear, possibly in place of this class implementing
     * the List<Item> interface in some manner
     */
    public List<Item> getFragmentContent() {
        List<Item> result = null;
        if (Item.isStartItem(firstItem)) {
            result = new ArrayList<Item>(items.size());
            int n = items.size() - 1;
            for (int i = 1; i < n; i++) {
                Item item = items.get(i);
                if (Item.isStartTag(item)) {
                    List<Item> fragment = new ArrayList<Item>();
                    fragment.add(item);
                    int depth = 1;
                    while (depth > 0) {
                        if (i >= n) { throw new IllegalStateException("An unbalanced " +
                                                                      "fragment: " + items); }
                        Item it = items.get(++i);
                        if (Item.isStartTag(it)) {
                            depth += 1;
                        } else if (Item.isEndTag(it)) {
                            depth -= 1;
                        }
                        fragment.add(it);
                    }
                    result.add(new XasFragment(fragment, item));
                } else if (Item.isContent(item)) {
                    Item it = items.get(i + 1);
                    if (Item.isContent(it)) {
                        List<Item> fragment = new ArrayList<Item>();
                        fragment.add(item);
                        while (Item.isContent(it)) {
                            fragment.add(it);
                            it = items.get(++i + 1);
                        }
                        result.add(new XasFragment(fragment, item));
                    } else {
                        result.add(new WrapperItem(item));
                    }
                } else {
                    result.add(item);
                    if (FragmentItem.isFragment(item)) {
                        i += ((FragmentItem) item).getSize() - 1;
                    }
                }
            }
        }
        return result;
    }


    public void setFragmentContent(List<Item> items) {
        if (this.items != null && this.items.size() > 0) {
            Item lastItem = this.items.get(this.items.size() - 1);
            this.items.clear();
            this.items.add(firstItem);
            this.items.addAll(items);
            this.items.add(lastItem);
        }
    }

}

// arch-tag: 4b882ebc-0350-4f6c-b09a-8ffb6ecbcbee
