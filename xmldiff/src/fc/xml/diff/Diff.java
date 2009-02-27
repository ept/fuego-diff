/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xmldiff-users@hoslab.cs.helsinki.fi.
 */

// $Id: Diff.java,v 1.19.2.1 2006/06/30 12:48:03 ctl Exp $

package fc.xml.diff;

import static fc.xml.diff.Segment.Operation.COPY;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;

import fc.util.log.Log;
import fc.util.log.StreamLogger;
import fc.xml.diff.encode.AlignEncoder;
import fc.xml.diff.encode.DiffEncoder;
import fc.xml.diff.encode.RefTreeByIdEncoder;
import fc.xml.diff.encode.RefTreeEncoder;
import fc.xml.diff.encode.XmlDiffEncoder;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.transform.DataItems;

public class Diff {

  static final int[] CHUNK_SIZES = {32,16,8,4,2,1};

  public static Map<String,String> ENCODER_ALIASES = new HashMap<String,String>();
  public static Map<String,String> FILTER_ALIASES = new HashMap<String,String>();
  static {
    ENCODER_ALIASES.put("xml",XmlDiffEncoder.class.getName());
    ENCODER_ALIASES.put("ref",RefTreeEncoder.class.getName());
    ENCODER_ALIASES.put("ref:id",RefTreeByIdEncoder.class.getName());
    ENCODER_ALIASES.put("align",AlignEncoder.class.getName());
    FILTER_ALIASES.put("full",null);
    FILTER_ALIASES.put("none",null);
  }

  public static void main(String[] args) throws IOException {
    Log.setLogger(new StreamLogger(System.err));
    Class encoder = fc.xml.diff.encode.XmlDiffEncoder.class;
    Class filter = DataItems.class;
    String encoderName = System.getProperty("encoder");
    String filterName = System.getProperty("filter");
    if( encoderName != null ) {
      if( ENCODER_ALIASES.containsKey(encoderName) )
        encoderName = ENCODER_ALIASES.get(encoderName);
      try {
        encoder = encoderName != null ? Class.forName(encoderName) : null;
      } catch ( ClassNotFoundException ex ) {
        Log.log("Cannot locate encoder "+encoderName,Log.FATALERROR);
      }
    }
    if( filterName != null ) {
      if( FILTER_ALIASES.containsKey(filterName) )
        filterName =  FILTER_ALIASES.get(filterName);
      try {
        filter = filterName != null ? Class.forName(filterName ) : null;
      } catch ( ClassNotFoundException ex ) {
        Log.log("Cannot locate filter "+filterName,Log.FATALERROR);
      }
    }
    if( args.length < 2 ) {
      Log.log("Usage [-Dencoder={xml,ref,align,<class>}] [-Dfilter={simple,<class>}] "+
             "base.xml new.xml [out.xml]", Log.ERROR);
      System.exit(1);
    }
    OutputStream dout = System.out;
    try {
      FileInputStream base = new FileInputStream(args[0]);
      FileInputStream updated = new FileInputStream(args[1]);
      if( args.length > 2 && !"-".equals(args[2]))
        dout = new FileOutputStream(args[2]);
      diff(base, updated, dout, filter, encoder, null /*options*/, true );
    } catch ( IOException ex ) {
      Log.log("I/O error while diffing",Log.ERROR,ex);
    } finally {
      if( dout != System.out )
        dout.close();
    }
  }

  public static boolean diff(InputStream bases, InputStream docs,
                          OutputStream dout) throws
          IOException {
    return diff(bases, docs, dout, DataItems.class,
         XmlDiffEncoder.class, null, true );
  }

  // Returns true if bases and docs differ
  public static boolean diff(ItemSource baseEs,
                             XmlPullParser baseParser,
                             ItemSource docEs,
                             XmlPullParser docParser,
                             OutputStream dout,
                             Class<DiffEncoder> outputEncoding,
                             Map<String,String> encoderOptions,
                             boolean emitEmpty ) throws
          IOException {
    long _start=System.currentTimeMillis();
    ArrayList<Integer> posListBase = baseParser == null ? null:
                                     new ArrayList<Integer>();
    ArrayList<Integer> posListNew = docParser == null ? null :
                                    new ArrayList<Integer>();
    List<Item> preamble = new ArrayList<Item>();
    List<Item> base =
      IoUtil.makeEventList(baseEs,preamble, posListBase, baseParser);
    List<Item> doc =
      IoUtil.makeEventList(docEs, null,posListNew, docParser);
    GlMatcher<Item> m = new GlMatcher<Item>(IoUtil.getEventHashAlgorithm());
    List<Segment<Item>> ml = m.match(base,doc,CHUNK_SIZES);
    long _stop=System.currentTimeMillis();
    boolean isEmpty = ml.size() == 1 &&
                      ml.get(0).getLength() == base.size() &&
                      ml.get(0).getOp() == COPY;
    try {
      DiffEncoder enc = outputEncoding.newInstance();
      if( dout != null && !isEmpty || emitEmpty ) {
        enc.encodeDiff(base, doc, ml, preamble, dout);
      }
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException("Cannot access encoder class "+outputEncoding,e);
    } catch (InstantiationException e) {
      throw new IllegalArgumentException("Unknown encoder "+outputEncoding,e);
    }
    if( isEmpty ) {
      Log.log("Documents identical " +
              " (" + base.size() + " XAS events in " +
              (_stop - _start) + "ms).", Log.INFO);
    } else {
      Log.log("Documents differ.",Log.INFO);
      //Log.log("Match list is "+ml,Log.DEBUG);
    }

    return !isEmpty;
  }

  // Returns true if bases and docs differ
  public static boolean diff(InputStream bases, InputStream docs,
                             OutputStream dout,
                             Class<? extends ItemTransform> filter,
                             Class outputEncoding,
                             Map<String, String> encoderOptions,
                             boolean emitEmpty) throws
      IOException {
    ItemSource docpa = IoUtil.getXmlParser(docs);
    ItemSource basepa = IoUtil.getXmlParser(bases);
    Log.log("Comparing by filter " +
            (filter == null ? "<none>" : filter.getName()),Log.INFO);

    return diff(IoUtil.getEventSequence(basepa,filter),
        null, // FIXME-20061113-3: Passing of XmlPull parser basepa,
        IoUtil.getEventSequence(docpa,filter),
        null, // FIXME-20061113-3: Passing of XmlPull parser docpa,
        dout,outputEncoding,encoderOptions,emitEmpty);
  }

}
