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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fc.util.Stack;
import fc.xml.xas.index.LazyFragment;

public class XasUtil {

    private static Map<String, FormatFactory> factories = new HashMap<String, FormatFactory>();

    public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";
    public static final String XSDT_NS = "http://www.w3.org/2001/XMLSchema-datatypes";
    public static final String XML_NS = "http://www.w3.org/XML/1998/namespace";
    public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";

    public static final Qname XSI_TYPE = new Qname(XSI_NS, "type");
    public static final Qname QNAME_TYPE = new Qname(XSD_NS, "QName");
    public static final Qname INT_TYPE = new Qname(XSD_NS, "int");
    public static final Qname LONG_TYPE = new Qname(XSD_NS, "long");
    public static final Qname STRING_TYPE = new Qname(XSD_NS, "string");
    public static final Qname HEX_BINARY_TYPE = new Qname(XSD_NS, "hexBinary");
    public static final Qname BASE64_BINARY_TYPE = new Qname(XSD_NS, "base64Binary");
    public static final Qname DATETIME_TYPE = new Qname(XSD_NS, "dateTime");
    public static final Qname DOUBLE_TYPE = new Qname(XSD_NS, "double");

    public static final String XML_MIME_TYPE = "text/xml";

    static {
        registerFactory(XML_MIME_TYPE, new XmlFormatFactory());
    }


    private XasUtil() {
    }


    public static void registerFactory(String type, FormatFactory factory) {
        Verifier.checkNotNull(type);
        Verifier.checkNotNull(factory);
        FormatFactory existing = factories.get(type);
        if (existing == null) {
            factories.put(type, factory);
        } else if (!existing.getClass().equals(factory.getClass())) { throw new IllegalArgumentException(
                                                                                                         "Type " +
                                                                                                                 type +
                                                                                                                 " already reserved for factory class " +
                                                                                                                 existing.getClass()); }
    }


    public static FormatFactory getFactory(String type) {
        return factories.get(type);
    }


    public static Collection<String> factoryTypes() {
        return factories.keySet();
    }


    public static void copy(ItemSource source, ItemTarget target) throws IOException {
        copy(source, target, Long.MAX_VALUE);
    }


    public static void copy(ItemSource source, ItemTarget target, long max) throws IOException {
        Item item;
        for (; max > 0 && (item = source.next()) != null; max--) {
            target.append(item);
        }
    }


    public static void copy(ItemSource source, List<Item> target) throws IOException {
        Item item;
        while ((item = source.next()) != null) {
            target.add(item);
        }
    }


    public static void copyFragment(ItemSource source, ItemTarget target) throws IOException {
        Item item;
        while ((item = source.next()) != null) {
            if (!Item.isDocumentDelimiter(item)) {
                target.append(item);
            }
        }
    }


    public static void copyFragment(ItemSource source, List<Item> target) throws IOException {
        Item item;
        while ((item = source.next()) != null) {
            if (!Item.isDocumentDelimiter(item)) {
                target.add(item);
            }
        }
    }


    /**
     * Return non-fragment item for this item. The method returns the first item that is not a
     * fragment by removing any fragment item encapsulations. I.e.
     * <code>skipFragment( XF(XF(ST({}tag))) ) = ST({}tag)</code>.
     * @param i
     *            item to strip fragments from
     * @return first non-fragment item
     */
    public static final Item skipFragment(Item i) {
        if (FragmentItem.isFragment(i)) {
            return skipFragment(((FragmentItem) i).get(0));
        } else return i;
    }

    public static final EmptyItemSource EMPTY_SOURCE = new EmptyItemSource();

    public static final ItemTarget SINK_TARGET = new ItemTarget() {

        public void append(Item item) {

        }

    };


    public static ItemSource itemSource(final ItemList il) {
        return new ItemSource() {

            int pos = 0;


            public Item next() {
                return pos < il.size() ? il.get(pos++) : null;
            }
        };

    }


    public static ItemSource itemSource(final Item... items) {
        return new ItemSource() {

            int pos = 0;


            public Item next() {
                return items.length > pos ? items[pos++] : null;
            }
        };
    }

    /**
     * An empty item source.
     */
    // Implementation note: ctl thinks that an explicit class for the empty
    // source is better than an anonymous one, as this will show up in the
    // class hierarchy which means its easier to find.
    public static class EmptyItemSource implements ItemSource {

        public Item next() {
            return null;
        }
    }

    /**
     * Item source holding exactly one item. The source is refillable.
     */

    public static class OneItemSource implements ItemSource {

        private int left = 0;
        private Item i;
        private boolean checkReads = false;


        public OneItemSource(Item i) {
            refill(i);
        }


        public Item next() throws IOException {
            if (checkReads && left <= 0) throw new IOException("Read past end of source");
            return left-- > 0 ? i : null;
        }


        public void refill(Item i) {
            left = 1;
            this.i = i;
        }
    }

    /**
     * Item source that returns only non-fragment items. Auto-forces any lazy fragments.
     */

    public static class FlattenItemSource implements ItemSource {

        Stack<FragmentItem> fiStack = new Stack<FragmentItem>();
        Stack<Integer> posStack = new Stack<Integer>();
        FragmentItem fi;
        int pos;


        public FlattenItemSource() {
        }


        public FlattenItemSource(FragmentItem fi) {
            this.fi = fi;
        }


        public void reset(FragmentItem fi) {
            fiStack.clear();
            posStack.clear();
            this.fi = fi;
            pos = 0;
        }


        public Item next() throws IOException {
            Item i = fi.get(pos++);
            if (FragmentItem.isFragment(i)) {
                // jkangash: If this is the only one, I won't change this yet
                if (i instanceof LazyFragment) ((LazyFragment) i).force(1);
                fiStack.push(fi);
                posStack.push(pos);
                pos = 0;
                fi = (FragmentItem) i;
                return next();
            } else if (i == null) {
                if (fiStack.isEmpty()) return null;
                fi = fiStack.pop();
                pos = posStack.pop();
                return next();
            }
            return i;
        }
    }

    /**
     * Item source on top of an array of items. Supports starting from a specific offset and ending
     * at a specific offset.
     */
    public static class ArraySource implements ItemSource {

        private ArrayList<Item> list;
        private int index;
        private int end;


        public ArraySource(ArrayList<Item> list) {
            this(list, 0, list.size());
        }


        public ArraySource(ArrayList<Item> list, int offset) {
            this(list, offset, list.size());
        }


        public ArraySource(ArrayList<Item> list, int offset, int end) {
            Verifier.checkNotNull(list);
            Verifier.checkOffsetLength(0, offset, end - offset, list.size());
            this.list = list;
            this.index = offset;
            this.end = end;
        }


        public Item next() throws IOException {
            if (index < end) {
                return list.get(index++);
            } else {
                return null;
            }
        }

    }

}

// arch-tag: 6a19edf3-d27a-4353-87d9-b81d4085d677
