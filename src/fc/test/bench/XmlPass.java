/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.test.bench;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.kxml2.io.KXmlParser;
import org.w3c.dom.Document;
import org.xml.sax.XMLReader;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import fc.util.Measurer;
import fc.util.Util;
import fc.xml.xas.Item;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.XmlPullSource;

public class XmlPass {

    private static void saxPass(String inFile, String outFile) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        SAXParser parser = factory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        OutputFormat format = new OutputFormat();
        XMLSerializer serializer = new XMLSerializer(new FileOutputStream(outFile), format);
        reader.setContentHandler(serializer);
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", serializer);
        reader.parse(inFile);
    }


    private static void domPass(String inFile, String outFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inFile);
        OutputFormat format = new OutputFormat();
        XMLSerializer serializer = new XMLSerializer(new FileOutputStream(outFile), format);
        serializer.serialize(document);
    }


    private static void xasPass(String inFile, String outFile) throws Exception {
        KXmlParser parser = new KXmlParser();
        FileInputStream in = new FileInputStream(inFile);
        XmlPullSource source = new XmlPullSource(parser, in);
        XmlOutput serializer = null;
        FileOutputStream out = new FileOutputStream(outFile);
        serializer = new XmlOutput(out, source.getEncoding());
        Item item;
        while ((item = source.next()) != null) {
            serializer.append(item);
        }
    }


    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                System.err.println("Usage: XmlPass (sax|dom|xas) <file>");
                System.exit(1);
            }
            Measurer.init(Measurer.TIMING);
            Measurer timer = Measurer.get(Measurer.TIMING);
            String outFile = args[1].concat(".pass");
            int end = 10;
            for (int i = 0; i < end; i++) {
                Util.runGc();
                long beginMemory = Util.usedMemory();
                Object token = timer.start();
                if (args[0].equals("sax")) {
                    saxPass(args[1], outFile);
                } else if (args[0].equals("dom")) {
                    domPass(args[1], outFile);
                } else if (args[0].equals("xas")) {
                    xasPass(args[1], outFile);
                } else {
                    System.err.println("Usage: XmlPass (sax|dom|xas) <file>");
                    System.exit(1);
                }
                if (i >= end - 2) {
                    timer.finish(token, "XML passthrough");
                }
                long spentMemory = Util.usedMemory();
                Util.runGc();
                long endMemory = Util.usedMemory();
                System.out.println("Total memory spent: " + (spentMemory - beginMemory));
                System.out.println("Object size: " + (endMemory - beginMemory));
                if (i >= end - 2) {
                    timer.output(System.out);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}

// arch-tag: 90503871-2ba5-4f9d-9e34-2513001e6238
