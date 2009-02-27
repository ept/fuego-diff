/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-raxs-users@hoslab.cs.helsinki.fi.
 */

package fc.raxs;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.SortedMap;
import java.util.TreeMap;

import fc.util.AbstractDictionary;
import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.Comment;
import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.TransformSource;
import fc.xml.xas.transform.DataItems;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.model.IdentificationModel;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.KeyModel;
import fc.xml.xmlr.model.NodeModel;
import fc.xml.xmlr.model.TreeModel;
import fc.xml.xmlr.model.TreeModels;
import fc.xml.xmlr.model.XasCodec;
import fc.xml.xmlr.xas.PeekableItemSource;
import fc.xml.xmlr.xas.XasSerialization;

/** Configuration of RAXS store. The object encapsulates the configuration
 * parameters for a RAXS store. These include XML file location, history
 * location, XMLR Tree Model, etc.
 * <p>Configuration have an XML serialization, whose schema is illustrated
 * by the following 
 * <pre>
 * &lt;raxs-c:config xmls:raxs-c="http://www.hiit.fi/fc/xmlr/raxs-config" &gt;
 *  &lt;raxs-c:entry key="raxs.contentpath" value="content.xml" /&gt; 
 *  &lt;raxs-c:entry key="raxs.datamodel" value="fc.raxs.XasStore.XAS_ITEM_TREE" /&gt;
 *   .
 *   .
 *   .
 * &lt;/raxs-c:config&gt; 
 * </pre>
 * The serialization is defined by this class.  
 */
public abstract class RaxsConfiguration extends AbstractDictionary implements
  StoreConfiguration {

  public static final String CONFIG_NS = "http://www.hiit.fi/fc/xmlr/raxs-config";
  public static final Qname CONFIG_ROOT_TAG = new Qname(CONFIG_NS,"config");
  public static final Qname CONFIG_ENTRY_TAG = new Qname(CONFIG_NS,"entry");
  public static final Qname CONFIG_KEY_ATTR = new Qname("","key");
  public static final Qname CONFIG_VALUE_ATTR = new Qname("","value");
  
  // NOTE: If you add a field here, add it to KEYS below!
  public static final String STORE_ROOT="raxs.root";
  public static final String STORE_FILE="raxs.contentpath";
  public static final String HISTORY_FILE="raxs.historypath";
  public static final String STORE_CLASS="raxs.store";
  public static final String STORE_CLASS_VERSION="raxs.storever";
  public static final String STORE_MODEL="raxs.datamodel";
  public static final String STORE_MODEL_KEYS="raxs.datamodel.keys";
  public static final String STORE_MODEL_IDENT="raxs.datamodel.id";
  public static final String STORE_MODEL_CODEC="raxs.datamodel.codec";
  public static final String STORE_MODEL_NODES="raxs.datamodel.nodes";
  
  // Ordered list of keys to serialize
  private static final String[] KEYS = { STORE_ROOT,  STORE_FILE, STORE_CLASS,
      STORE_CLASS_VERSION, STORE_MODEL, STORE_MODEL_KEYS, STORE_MODEL_IDENT,
      STORE_MODEL_CODEC, STORE_MODEL_NODES, HISTORY_FILE };

  /** XAS Codec version 1 for configurations.
   */
  
  public static final XasCodec XAS_CODEC_V1 = new StoreCodec();
  
  /** Current XAS Codec for configurations. Currently set to 
   * {@link #XAS_CODEC_V1}.
   */
  
  public static final XasCodec XAS_CODEC = XAS_CODEC_V1;
  
  /** Get default configuration.
   * 
   * @param s store class
   * @param f XML file, or directory containing <code>content.xml</code>
   * @return configuration
   * @throws IOException
   */
  public static RaxsConfiguration getConfig(Class<? extends Store> s, File f) 
  throws IOException {
    if( f.isDirectory() ) {
      return RaxsConfiguration.getConfig(s,f,new File("content.xml"));
    } else
      return RaxsConfiguration.getConfig(s,f.getParentFile(),new File(f.getName()));
  }  

  /** Get default configuration.
   * 
   * @param st store class
   */
  
  public static RaxsConfiguration getConfig(Class<? extends Store> st) {
    return new DefaultConfig(st,null,null,null);
  }
  /** Get default configuration.
   * 
   * @param st store class
   * @param root root directory of store
   * @param storeFile XML file, or directory containing <code>content.xml</code>.
   * relative to <i>root</i>
   * @return configuration
   */
  public static RaxsConfiguration getConfig(Class<? extends Store> st, 
      File root, File storeFile) {
    return new DefaultConfig(st,root,storeFile,null);
  }
  
  public static RaxsConfiguration getConfig(Class<? extends Store> st, 
      File root, File storeFile, TreeModel tm) {
    return new DefaultConfig(st,root,storeFile,tm);
  }

  /** Read configuration from XML.
   * 
   * @param is Item source containing an XML-encoded configuration.
   * @return configuration
   * @throws IOException
   */
  public static RaxsConfiguration read(ItemSource is) throws IOException {
    RefTree t = XasSerialization.readTree( new TransformSource(
        is, new DataItems() ), 
        TreeModels.xasItemTree().swapCodec(XAS_CODEC));
    return (RaxsConfiguration) t.getRoot().getContent();
  }
  
  /** Write this configuration to XML.
   * 
   * @param t Item target to output XML to
   * @throws IOException
   */
  public void write(ItemTarget t) throws IOException {
    RefTree rt = RefTrees.getRefTree(this);
    XasSerialization.writeTree(rt, t, XAS_CODEC);
  }
  
  /** Get store root. 
   * 
   * @return store root
   */
  public File getRoot() {
    String n = lookup(STORE_ROOT);
    return n != null ? new File(n) : null;
  }
  
  /** Get store XML file.
   * @return store XML file
   */
  public File getStoreFile() {
    String storeFile = lookup(STORE_FILE);
    File root = getRoot();
    if( root == null )
      return null;
    return storeFile == null ? root:  new File(root,storeFile);
  }

  /** Get store history location.
   * 
   * @return location of history data.
   */
  public File getHistoryFile() {
    String historyFile = lookup(HISTORY_FILE);
    File root = getRoot();
    if( root == null )
      return null;
    return historyFile == null ? root:  new File(root,historyFile);
  }
  
  /** Get store tree model.
   * @return store tree model
   */
  public TreeModel getModel() throws ParseException {
    String model = lookup(STORE_MODEL);
    String keys = lookup(STORE_MODEL_KEYS);
    String ids = lookup(STORE_MODEL_IDENT);
    String nodes = lookup(STORE_MODEL_NODES);
    String codec = lookup(STORE_MODEL_CODEC);
    TreeModel m = null; //BUGFIX-20061221-2: null is right default
    if( model != null ) {
      return m = (TreeModel) readField(model,TreeModel.class);
    }
    if( keys != null ) {
      KeyModel km = (KeyModel) readField(keys, KeyModel.class);
      m.swapKeyModel(km);
    }
    if( ids != null ) {
      IdentificationModel im = (IdentificationModel) readField(keys, 
          IdentificationModel.class);
      m.swapIdentificationModel(im);
    }
    if( codec != null ) {
      XasCodec c = (XasCodec) readField(keys, XasCodec.class);
      m.swapCodec(c);
    }
    if( nodes != null ) {
      NodeModel nm = (NodeModel) readField(keys, NodeModel.class);
      m.swapNodeModel(nm);
    }
    return m;
  }

  /** Create store instance. Creates a store instance by dynamically
   * creating an instance of the store class specified by the configuration.
   * The configuration is passed to the class constructor, i.e. the
   * Store class must have a constructor 
   * <code>public <i>ClassName</i>(StoreConfiguration _)</code>. 
   * 
   * @return store instance
   * @throws ParseException
   */
  @SuppressWarnings("unchecked")
  public Store createStore() throws ParseException {
    String sclassS = lookup(STORE_CLASS);
    if(Util.isEmpty(sclassS)) {
      throw new ParseException("No store class given");
    }
    try {
      Class<Store> sclass = (Class<Store>) Class.forName(sclassS);
      Constructor<Store> c = sclass.getConstructor(
          new Class[] {StoreConfiguration.class});
      return c.newInstance(this);
    } catch (ClassCastException e) {
      Log.error(e);
      throw new ParseException("Value "+sclassS+" is not a store");
    } catch (SecurityException e) {
      Log.error(e);
      throw new ParseException("Cannot create store "+sclassS);
    } catch (InvocationTargetException e) {
      Log.error(e);
      throw new ParseException("Cannot create store "+sclassS);
    } catch (ClassNotFoundException e) {
      Log.error(e);
      throw new ParseException("Unknown value (no class): "+sclassS);
    } catch (IllegalAccessException e) {
      Log.error(e);
      throw new ParseException("Cannot create store "+sclassS);
    } catch ( NoSuchMethodException e) {
      Log.error(e);
      throw new ParseException("Cannot create store "+sclassS);
    } catch (InstantiationException e) {
      Log.error(e);
      throw new ParseException("Cannot create store "+sclassS);
    } 
    
  }
  
  private Object readField(String fieldName, Class c) throws ParseException {
    int fstart = fieldName.lastIndexOf('.');
    if( fstart == -1 || (fstart+1) >= fieldName.length())
      throw new ParseException("Bad value (missing '.') "+fieldName);
    String mclassS = fieldName.substring(0, fstart);
    String field = fieldName.substring(fstart+1);
    try {
      Class mclass = Class.forName(mclassS);
      Field f = mclass.getField(field);
      Object o = f.get(null);
      c.cast(o); // Test casting
      return o;
    } catch (ClassCastException e) {
      Log.error(e);
      throw new ParseException("Value of "+fieldName+
          " is not of type "+c.getName());
    } catch (SecurityException e) {
      Log.error(e);
      throw new ParseException("Cannot evaluate "+fieldName);
    } catch (IllegalArgumentException e) {
      Log.error(e);
      throw new ParseException("Cannot evaluate "+fieldName);
    } catch (ClassNotFoundException e) {
      Log.error(e);
      throw new ParseException("Unknown value (no class): "+fieldName);
    } catch (NoSuchFieldException e) {
      Log.error(e);
      throw new ParseException("Unknown value (no field): "+fieldName);
    } catch (IllegalAccessException e) {
      Log.error(e);
      throw new ParseException("Cannot evaluate "+fieldName);
    }
  }

  private static class StoreCodec implements XasCodec {

    private static int CODEC_VER = 1;
    
    public Object decode(PeekableItemSource is, 
        KeyIdentificationModel kim) throws IOException {
      StartTag entryTag = new StartTag(CONFIG_ENTRY_TAG);
      EndTag entryETag = new EndTag(CONFIG_ENTRY_TAG);
      DefaultConfig c = new DefaultConfig();
      Item ri = is.peek();
      if( !(new StartTag(CONFIG_ROOT_TAG)).equals(ri) )
        throw new ParseException("Expected ",new StartTag(CONFIG_ROOT_TAG),is);
      for( Item i = is.next(); (i=is.next()).equals(entryTag); ) {
        StartTag st = (StartTag) i;
        String key = st.getAttributeValue(CONFIG_KEY_ATTR).toString();
        String value = st.getAttributeValue(CONFIG_VALUE_ATTR).toString();
        if( key == null || value == null)
          throw new ParseException("Missing key or value attribute for ",st,is);
        c.set(key, value);
        if( !entryETag.equals( is.next() ) )
          throw new ParseException("Expected end tag ",entryETag);
      }
      if( !(new StartTag(CONFIG_ROOT_TAG)).equals(is.peek()) )
        throw new ParseException("Expected ",new EndTag(CONFIG_ROOT_TAG),is);
      return c;
    }

    public void encode(ItemTarget t, RefTreeNode n, StartTag context) throws IOException {
      
      assert n.getContent() instanceof RaxsConfiguration;
      RaxsConfiguration sc = (RaxsConfiguration) n.getContent();
      t.append(new Comment("RAXS Store config file version "+CODEC_VER));
      t.append(new StartTag(CONFIG_ENTRY_TAG,context));
      for( String key : KEYS ) {
        String val = sc.lookup(key);
        if( val != null ) {
          StartTag st = new StartTag(CONFIG_ENTRY_TAG,context);
          st.addAttribute(CONFIG_KEY_ATTR, key);
          st.addAttribute(CONFIG_VALUE_ATTR, val);
          EndTag et = new EndTag(st.getName());
          t.append(st);
          t.append(et);
        }
      }
      t.append(new EndTag(CONFIG_ENTRY_TAG)); // Not really needed
    }
  }
  
  private static class DefaultConfig extends RaxsConfiguration {

    private SortedMap<String,String> vals = new TreeMap<String, String>();
    private TreeModel tm = null;
    public DefaultConfig() {
      this(DeweyStore.class,null,null,null);
    }
    
    public DefaultConfig(Class<? extends Store> st, File root, 
        File sf, TreeModel tm) {
      super();
      this.tm = tm;
      vals.put( STORE_CLASS, st.getName() );
      vals.put( STORE_CLASS_VERSION, "1" );
      vals.put( STORE_MODEL, null );
      vals.put( STORE_MODEL, tm == null ? null : "<UNKNOWN>" );
      vals.put( STORE_MODEL_KEYS, null);
      vals.put( STORE_MODEL_IDENT, null);
      vals.put( STORE_MODEL_CODEC, null);
      vals.put( STORE_MODEL_NODES, null);
      vals.put( STORE_FILE, root == null ? null : sf.getPath() );
      vals.put( HISTORY_FILE, "history");
      vals.put( STORE_ROOT, root == null ? null : root.getPath());
    }

    @Override
    protected String lookup(String key) {
      return vals.get(key);
    }
 
    @Override
    public TreeModel getModel() throws ParseException {
      if( tm != null )
        return tm;
      return super.getModel();
    }

    void set(String key, String val) {
      vals.put(key, val);
    }
  }
}
// arch-tag: 64227f7e-68c7-48d3-bb92-7f5a24ff6e5e

