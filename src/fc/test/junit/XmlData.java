/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.test.junit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.kxml2.io.KXmlParser;

import fc.xml.xas.EndDocument;
import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.ItemList;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.Qname;
import fc.xml.xas.Queryable;
import fc.xml.xas.StartDocument;
import fc.xml.xas.StartTag;
import fc.xml.xas.XasFragment;
import fc.xml.xas.XasUtil;
import fc.xml.xas.XmlPullSource;
import fc.xml.xas.index.VersionedDocument;
import fc.xml.xas.typing.ParsedPrimitive;
import fc.xml.xas.typing.TypedItem;
import fc.xml.xas.typing.ValueCodec;

public class XmlData {

    static final String TEST_NS = "http://www.hiit.fi/fuego/fc/test";
    private static final Map<XasFragment, Integer> sigCounts = new IdentityHashMap<XasFragment, Integer>();

    static StartDocument sd;
    static StartTag s0;
    static StartTag s01;
    static StartTag s011;


    private XmlData() {
    }


    private static XasFragment convert(ItemSource source) throws IOException {
        List<Item> items = new ArrayList<Item>();
        Item item = source.next();
        Item firstItem = item;
        while (item != null) {
            items.add(item);
            item = source.next();
        }
        return new XasFragment(items, firstItem);
    }


    public static List<XasFragment> getData() throws IOException {
        List<XasFragment> result = new ArrayList<XasFragment>();
        // BUG-20070204-1: This does not work in the Xebu project due
        // to the URL being a jar: URL instead of a file: URL. Need
        // to find a way to get resources from either kind of URL.
        URL url = XmlData.class.getResource("/test/xml/");
        try {
            File dir = new File(new URI(url.toString()));
            File[] files = dir.listFiles(); // NOTE: also lists dirs,despite
            // its name
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) continue;
                KXmlParser parser = new KXmlParser();
                result.add(convert(new XmlPullSource(parser, new FileInputStream(files[i]))));
            }
        } catch (Exception ex) {
            // Util.throwWrapped(new IOException(ex.getMessage()), ex);
        }
        return result;
    }


    public static List<ItemList> getTypedData() {
        List<ItemList> result = new ArrayList<ItemList>();
        ItemList list = new ItemList();
        list.append(StartDocument.instance());
        StartTag st = new StartTag(new Qname(TEST_NS, "foo"));
        st.addPrefix(XasUtil.XSI_NS, "xsi");
        st.addPrefix(XasUtil.XSD_NS, "xsd");
        st.addPrefix(TEST_NS, "test");
        st.addAttribute(XasUtil.XSI_TYPE, new ParsedPrimitive(new Qname(XasUtil.XSD_NS, "QName"),
                                                              new Qname(XasUtil.XSD_NS, "int")));
        list.append(st);
        list.append(new ParsedPrimitive(new Qname(XasUtil.XSD_NS, "int"), Integer.valueOf(188)));
        list.append(new EndTag(new Qname(TEST_NS, "foo")));
        list.append(EndDocument.instance());
        result.add(list);
        list = new ItemList();
        list.append(StartDocument.instance());
        st = new StartTag(new Qname(TEST_NS, "bar"));
        st.addPrefix(XasUtil.XSI_NS, "xsi");
        st.addPrefix(XasUtil.XSD_NS, "xsd");
        st.addPrefix(TEST_NS, "test");
        st.addAttribute(XasUtil.XSI_TYPE, new ParsedPrimitive(new Qname(XasUtil.XSD_NS, "QName"),
                                                              new Qname(TEST_NS, "person")));
        list.append(st);
        Person person = new Person("Jaakko", 30, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        list.append(new TypedItem(new Qname(TEST_NS, "person"), person));
        list.append(new EndTag(new Qname(TEST_NS, "bar")));
        list.append(EndDocument.instance());
        result.add(list);
        return result;
    }


    public static int getSignatureCount(XasFragment f) {
        Integer count = sigCounts.get(f);
        if (count != null) {
            return count.intValue();
        } else {
            return 0;
        }
    }


    public static XasFragment getFragment() {
        Qname n = new Qname(XmlData.TEST_NS, "n");
        StartTag s = new StartTag(n);
        EndTag e = new EndTag(n);
        ArrayList<Item> result = new ArrayList<Item>();
        result.add(s);
        result.add(e);
        return new XasFragment(result, s);
    }


    public static List<Queryable> getTrees() {
        ArrayList<Queryable> result = new ArrayList<Queryable>();
        sd = StartDocument.instance();
        EndDocument ed = EndDocument.instance();
        Qname n0 = new Qname(XmlData.TEST_NS, "n0");
        Qname n00 = new Qname(XmlData.TEST_NS, "n00");
        Qname n01 = new Qname(XmlData.TEST_NS, "n01");
        Qname n000 = new Qname(XmlData.TEST_NS, "n000");
        Qname n001 = new Qname(XmlData.TEST_NS, "n001");
        Qname n010 = new Qname(XmlData.TEST_NS, "n010");
        Qname n011 = new Qname(XmlData.TEST_NS, "n011");
        Qname n012 = new Qname(XmlData.TEST_NS, "n012");
        Qname n0110 = new Qname(XmlData.TEST_NS, "n0110");
        Qname n0111 = new Qname(XmlData.TEST_NS, "n0111");
        s0 = new StartTag(n0);
        StartTag s00 = new StartTag(n00, s0);
        s01 = new StartTag(n01, s0);
        StartTag s000 = new StartTag(n000, s00);
        StartTag s001 = new StartTag(n001, s00);
        StartTag s010 = new StartTag(n010, s01);
        s011 = new StartTag(n011, s01);
        StartTag s012 = new StartTag(n012, s01);
        StartTag s0110 = new StartTag(n0110, s011);
        StartTag s0111 = new StartTag(n0111, s011);
        EndTag e0 = new EndTag(n0);
        EndTag e00 = new EndTag(n00);
        EndTag e01 = new EndTag(n01);
        EndTag e000 = new EndTag(n000);
        EndTag e001 = new EndTag(n001);
        EndTag e010 = new EndTag(n010);
        EndTag e011 = new EndTag(n011);
        EndTag e012 = new EndTag(n012);
        EndTag e0110 = new EndTag(n0110);
        EndTag e0111 = new EndTag(n0111);
        ItemList list = new ItemList();
        list.append(sd);
        list.append(s0);
        list.append(s00);
        list.append(s000);
        list.append(e000);
        list.append(s001);
        list.append(e001);
        list.append(e00);
        list.append(s01);
        list.append(s010);
        list.append(e010);
        list.append(s011);
        list.append(s0110);
        list.append(e0110);
        list.append(s0111);
        list.append(e0111);
        list.append(e011);
        list.append(s012);
        list.append(e012);
        list.append(e01);
        list.append(e0);
        list.append(ed);
        // It's not possible to use non-versioned fragment for the things
        // XasTest wants to do
        // result.add(list.fragment());
        result.add(new VersionedDocument(list.fragment()));
        return result;
    }

    public static class Person {

        public String name;
        public int age;
        public Calendar birthday;


        public Person(String name, int age, Calendar birthday) {
            this.name = name;
            this.age = age;
            this.birthday = birthday;
        }


        @Override
        public int hashCode() {
            return name.hashCode() ^ age ^ birthday.hashCode();
        }


        @Override
        public boolean equals(Object o) {
            if (o instanceof Person) {
                Person p = (Person) o;
                return name.equals(p.name) && age == p.age && birthday.equals(p.birthday);
            } else {
                return false;
            }
        }


        @Override
        public String toString() {
            return "Person(name=" + name + ",age=" + age + ",birthday=" + birthday + ")";
        }

    }

    public static class PersonCodec implements ValueCodec {

        private static final Qname KNOWN_TYPE = new Qname(TEST_NS, "person");
        private static final Qname NAME_NAME = new Qname(TEST_NS, "name");
        private static final Qname AGE_NAME = new Qname(TEST_NS, "age");
        private static final Qname BIRTHDAY_NAME = new Qname(TEST_NS, "birthday");
        private static final Qname NAME_TYPE = new Qname(XasUtil.XSD_NS, "string");
        private static final Qname AGE_TYPE = new Qname(XasUtil.XSD_NS, "int");
        private static final Qname BIRTHDAY_TYPE = new Qname(XasUtil.XSD_NS, "dateTime");
        private static final EndTag NAME_END = new EndTag(new Qname(TEST_NS, "name"));
        private static final EndTag AGE_END = new EndTag(new Qname(TEST_NS, "age"));
        private static final EndTag BIRTHDAY_END = new EndTag(new Qname(TEST_NS, "birthday"));


        private static StartTag tag(Qname name, Qname type, StartTag parent) {
            StartTag result = new StartTag(name, parent);
            result.addAttribute(XasUtil.XSI_TYPE, new ParsedPrimitive(new Qname(XasUtil.XSD_NS,
                                                                                "QName"), type));
            return result;
        }


        public boolean isKnown(Qname typeName) {
            return KNOWN_TYPE.equals(typeName);
        }


        public void encode(Qname typeName, Object value, ItemTarget target, StartTag parent)
                throws IOException {
            Person person = (Person) value;
            target.append(tag(NAME_NAME, NAME_TYPE, parent));
            target.append(new ParsedPrimitive(NAME_TYPE, person.name));
            target.append(NAME_END);
            target.append(tag(AGE_NAME, AGE_TYPE, parent));
            target.append(new ParsedPrimitive(AGE_TYPE, person.age));
            target.append(AGE_END);
            target.append(tag(BIRTHDAY_NAME, BIRTHDAY_TYPE, parent));
            target.append(new ParsedPrimitive(BIRTHDAY_TYPE, person.birthday));
            target.append(BIRTHDAY_END);
        }


        private Object expect(Qname name, Qname type, ItemSource source) throws IOException {
            Object result = null;
            Item item = source.next();
            if (Item.isStartTag(item)) {
                StartTag st = (StartTag) item;
                if (st.getName().equals(name)) {
                    item = source.next();
                    if (ParsedPrimitive.isParsedPrimitive(item)) {
                        ParsedPrimitive pp = (ParsedPrimitive) item;
                        if (pp.getTypeName().equals(type)) {
                            result = pp.getValue();
                            source.next();
                        }
                    }
                }
            }
            return result;
        }


        public Object decode(Qname typeName, ItemSource source) throws IOException {
            String name = (String) expect(NAME_NAME, NAME_TYPE, source);
            Integer age = (Integer) expect(AGE_NAME, AGE_TYPE, source);
            Calendar birthday = (Calendar) expect(BIRTHDAY_NAME, BIRTHDAY_TYPE, source);
            if (name != null && age != null && birthday != null) {
                return new Person(name, age.intValue(), birthday);
            } else {
                return null;
            }
        }

    }

}

// arch-tag: b8d903bf-2dea-42c6-a696-0f4d30c7b8f6
