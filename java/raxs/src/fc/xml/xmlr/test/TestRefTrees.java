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

package fc.xml.xmlr.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.kxml2.io.KXmlParser;

import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.ItemSource;
import fc.xml.xas.StartTag;
import fc.xml.xas.TransformSource;
import fc.xml.xas.XmlPullSource;
import fc.xml.xas.transform.DataItems;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.KeyMap;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.StringKey;
import fc.xml.xmlr.model.TreeModels;
import fc.xml.xmlr.model.XasCodec;
import fc.xml.xmlr.xas.PeekableItemSource;
import fc.xml.xmlr.xas.RefItem;
import fc.xml.xmlr.xas.XasSerialization;

public class TestRefTrees extends TestCase {
  public TestRefTrees(String name) {
    super(name);
  }

  public static final String NT1_BASE =
      "<r xmlns:ref='"+RefItem.REF_NS+"' id='r'>"+
      "<a id='a'><b id='b'/></a><c id='c'><d id='d'><e id='e'/></d></c>"+
      "</r>";

  public static final String NT1_1 =
      "<r xmlns:ref='"+RefItem.REF_NS+"' id='r'>"+
      "<i id='i'><ref:tree id='a' /></i><ref:tree id='c' />"+
      "</r>";

  public static final String NT1_2 =
      "<r xmlns:ref='"+RefItem.REF_NS+"' id='r'>"+
      "<ref:tree id='a' /><ref:node id='d' />"+
      "</r>";
  public static final String[][] NT1_FACIT={{"a","e"},{"a"}};


  public static final String NT2_BASE = NT1_BASE;
  public static final String NT2_1 =
      "<ref:tree xmlns:ref='"+RefItem.REF_NS+"' id='r' />";

  public static final String NT2_2 =
      "<r xmlns:ref='"+RefItem.REF_NS+"' id='r'>"+
      "<ref:tree id='a' /><ref:tree id='d' />"+
      "</r>";
  public static final String[][] NT2_FACIT={{"a","d"},{"a","d"}};

  
  
  public static final String NT3_BASE = NT1_BASE;
  public static final String NT3_1 =
      "<r xmlns:ref='"+RefItem.REF_NS+"' id='r'>"+
      "<a id='a'><ref:tree id='b' /></a><ref:tree id='c' />"+
      "</r>";
  public static final String NT3_2 =
      "<r xmlns:ref='"+RefItem.REF_NS+"' id='r'>"+
      "<ref:tree id='a' /><c><ref:tree id='d' /></c>"+
      "</r>";
  public static final String[][] NT3_FACIT={{"b","d"},{"b","d"}};


  public static final String NT4_BASE =
      "<r xmlns:ref='"+RefItem.REF_NS+"' id='r'>"+
      "<a id='a'><b id='b'/></a><c id='c'><d id='d'><e id='e'/></d></c><f id='f'><g id='g' /></f>"+
      "</r>";

  public static final String NT4_1 =
      "<r xmlns:ref='"+RefItem.REF_NS+"' id='r'>"+
      "<ref:tree id='a' /><ref:node id='d'><ref:tree id='f' /></ref:node>"+
      "</r>";

  public static final String NT4_2 =
      "<r xmlns:ref='"+RefItem.REF_NS+"' id='r'>"+
      "<i id='i'><ref:tree id='a' /></i><ref:tree id='c' />"+
      "</r>";
  public static final String[][] NT4_FACIT={{"a","f"},{"a","e"}};

  // Downmove
  public static final String NT5_BASE =
      "<r xmlns:ref='" + RefItem.REF_NS + "' id='r'>" +
      "<a id='a' /><b id='b' />"+
      "</r>";

  public static final String NT5_1 =
      "<ref:tree xmlns:ref='"+RefItem.REF_NS+"' id='r' />";

  public static final String NT5_2 =
      "<r xmlns:ref='" + RefItem.REF_NS + "' id='r'>" +
      "<ref:node id='a'><ref:tree id='b' /><i id='i' /></ref:node>"+
      "</r>";
  public static final String[][] NT5_FACIT = {
      {"b"},
      {"b"}
  };

  public static final String NT6_BASE =
        "<tree id='0' xmlns:ref='http://www.hiit.fi/fc/xml/ref'>" +
/*        "<file name='gnu-10.txt' id='10' />" +
        "<file name='quup-11.class' id='11' />" +
        "<file name='quup-12.tmp' id='12' />" +
        "<file name='armadillo-17.txt' id='17' />" +*/
        "<file name='ding-22.gif' id='22' />" +
//        "<file name='ding-35.ps' id='35' />" +
//        "<file name='wocky-4.tex' id='4' />" +
//        "<file name='gnat-44.gif' id='44' />" +
        "<directory name='bin-46' id='46'>" +
//        "<file name='wocky-61.gif' id='61' />" +
        "</directory>" +
        "<file name='baz-5.txt' id='5' />" +
//        " <file name='dong-50.java' id='50' />" +
//        " <file name='gnu-63.doc' id='53' />" +
        "  <file name='gnu-56.tex' id='56' />" +
//        "  <file name='dong-6.tmp' id='6' />" +
//        "  <file name='baz-7.gif' id='7' />" +
        "  <file name='quup-58.java' id='8' />" +
//        "  <file name='ding-9.doc' id='9' />" +
        "</tree>";

  public static final String NT6_1 =
        "<ref:tree xmlns:ref='"+RefItem.REF_NS+"' id='0' />";

  public static final String NT6_2 =
        "<ref:node id='0' xmlns:ref='http://www.hiit.fi/fc/xml/ref'>" +
        "<file name='armadillo-73.java' id='5' />"+
///        "<ref:tree id='5' />" +
/*        "<ref:tree id='6' />" +
        "<ref:tree id='7' />" +
        "<ref:tree id='9' />" +
        "<ref:tree id='10' />" +
        "<ref:tree id='11' />" +
        "<ref:tree id='12' />" +
        "<ref:tree id='17' />" +
        "<ref:tree id='35' />" +
        "<ref:tree id='44' />" +*/
 //"<ref:tree id='22' />" + // NOTDELAFTERALL
//"<ref:tree id='56' />" + // NOTDELAFTERALL

        "<ref:node id='46'>" +
        "<ref:tree id='8' />" +
        "<file name='gnu-75.xml' id='75' />" +
        "</ref:node>" +
//        "<ref:tree id='50' />" +
//        "<ref:tree id='53' />" +
        "<file name='baz-74.tmp' id='74' />" +
        "</ref:node>";
  public static final String[][] NT6_FACIT = {
//  ORIG:        {"10","11","12","17","35","4","44","5","50","53","6","7","8","9","22","56",    "46"},
//  ORIG:        {"10","11","12","17","35","4","44","5","50","53","6","7","8","9",    "22","56",    "46"}
    { "5", //
      // The presence of "5" depends on the matching. If there is no matching
      // between expanded nodes (e.g. in the case where we assume expanded ->
      // updated or inserted), then "5" is an allowed ref in NT6_1 (because 
      // "5" in NT6_2 has nothing to do with it)
      // If, OTOH, the 5s are matched, a treeref to "5" in NT&_1 can't be 
      // allowed, as this would contradict the expansion state (non-ref) in
      // NT 6_1
      "8","22","56"  /*,"46" /****"61"*/},
      {"8"},
  };

  
  // Use case: finding set of deleted refs
  public static final String NT7_BASE =
      "<a xmlns:ref='" + RefItem.REF_NS + "' id='a'>" +
      "<b id='b' ><d id='d'/><e id='e'/></b><c id='c'><f id='f'/><g id='g'/></c>"+
      "</a>";

  public static final String NT7_1 =
    "<ref:node id='a' xmlns:ref='" + RefItem.REF_NS + "'>" +
    "<b id='b' ><ref:node id='e'/></b><ref:tree id='c' />"+
    "</ref:node>";

  public static final String NT7_2 =
    "<ref:tree id='a' xmlns:ref='" + RefItem.REF_NS + "' />";

  public static final String[][] NT7_FACIT = {
      {"c"},
      {"c","d"}
  };
  

  public void testNormalize() throws Exception  {
    Object[][] tests = {
        {NT1_BASE,NT1_1,NT1_2,NT1_FACIT},
        {NT2_BASE,NT2_1,NT2_2,NT2_FACIT},
        {NT3_BASE,NT3_1,NT3_2,NT3_FACIT},
        {NT4_BASE,NT4_1,NT4_2,NT4_FACIT},
        {NT5_BASE,NT5_1,NT5_2,NT5_FACIT},
        {NT6_BASE,NT6_1,NT6_2,NT6_FACIT},
        {NT7_BASE,NT7_1,NT7_2,NT7_FACIT}
        
    };
    for(int iTree=0;iTree<tests.length;iTree++) {
      Object[] test = tests[iTree];
      IdAddressableRefTree base =
        RefTrees.getAddressableTree(readTree(test[0].toString()));
      RefTree t1 = readTree(test[1].toString());
      RefTree t2 = readTree(test[2].toString());
      for( int i=0;i<2;i++) {
        RefTree t1p = i==0 ? t1 : t2;
        RefTree t2p = i==0 ? t2 : t1;
        Set[] crefs = new Set[2];
        Set[] refs = RefTrees.normalize(base, new RefTree[] {t1p, t2p}, 
            crefs, KeyMap.UNMAPPABLE );
        //System.out.println("Content is expanded for: "+crefs);
        String[] facitSet0 = (String[]) ( (Object[]) test[3])[i];
        String[] facitSet1 = (String[]) ( (Object[]) test[3])[(i+1)%2];
                
        Assert.assertEquals("Facit for 1st tree failed, tree "+iTree+",lap "+i,
            makeKeySet(facitSet0),refs[0]);
        Assert.assertEquals("Facit for 2nd tree failed, tree "+iTree+",lap "+i,
            makeKeySet(facitSet1),refs[1]);
        
        Log.log("Set[t1]=" + refs[0], Log.INFO);
        Log.log("Set[t2]=" + refs[1], Log.INFO);
      }
    }
  }

  public Set makeKeySet(String[] skeys ) {
    Set keys = new HashSet<String>();
    for( String s : skeys )
      keys.add(StringKey.createKey(s));
    return keys;
  }
    
  public static RefTree readTree(String s) throws IOException {
    RefTree tree = null;
    ItemSource is = new TransformSource( new XmlPullSource(new KXmlParser(),
        new ByteArrayInputStream(s.getBytes())),
        new DataItems() );
    tree = XasSerialization.readTree(is, 
        TreeModels.xmlr1Model().swapCodec(new TestCodec()) );
    Assert.assertNotNull("Read tree is null.",tree);
    //XmlrDebug.dumpTree(tree, System.out);
    return tree;
  }

  public static String ET1_BACKING=
      "<tree id='0'>"+
       "<dir id='1'>"+
        "<dir id='8'>"+
         "<dir id='9'>"+
          "<dir id='10' />"+
         "</dir>"+
        "</dir>"+
       "</dir>"+
      "<file id='2' />"+
      "<file id='3' />"+
      "<file id='4' />"+
      "<file id='5' />"+
      "<file id='6' />"+
      "<file id='7' />"+
     "</tree>";

  public static String ET1_FACIT=
    "<ref:node xmlns:ref='"+RefItem.REF_NS+"' id='0'>"+
     "<ref:node id='1'>"+
      "<ref:node id='8'>"+
       "<dir id='9'>"+
        "<ref:tree id='10' />"+
       "</dir>"+
      "</ref:node>"+
     "</ref:node>"+
    "<ref:tree id='2' />"+
    "<ref:tree id='3' />"+
    "<ref:tree id='4' />"+
    "<ref:tree id='5' />"+
    "<ref:tree id='6' />"+
    "<ref:tree id='7' />"+
   "</ref:node>";

  
  public static String[] ET1_TREEREFS = {"2", "4", "6", "3", "7", "10", "5"};
  public static String[] ET1_NODEREFS = {"0", "1", "2", "3", "4", "5", "6",
    "7", "8", "10" }; // 9 is the missing number

  
  public void testExpand() throws Exception  {
    Object[][] tests = {
        // BUGFIX-20061212-1: The ET1 set tests for this bug
        {ET1_BACKING,ET1_TREEREFS,ET1_NODEREFS,ET1_FACIT},  
    };
    
    for(int iTree=0;iTree<tests.length;iTree++) {
      Object[] test = tests[iTree];
      IdAddressableRefTree backing =
        RefTrees.getAddressableTree(readTree(test[0].toString()));
      IdAddressableRefTree facit =
        RefTrees.getAddressableTree(readTree(test[3].toString()));

      Set allowedTreeRefs = makeKeySet((String[]) test[1]);
      Set allowedNodeRefs = makeKeySet((String[]) test[2]);
      RefTree expTree = RefTrees.expandRefs(
          RefTrees.getRefTree(backing),
          allowedTreeRefs, allowedNodeRefs, backing);
      //Log.debug("Expanded tree is");
      //XmlrDebug.dumpTree(expTree);
      //Log.debug("Facit tree is");
      //XmlrDebug.dumpTree(facit);
      Assert.assertTrue("Facit and expanded trees differ (tree2=facit)",
          XmlrDebug.equalityTreeComp( expTree, facit ) );
    }
  }
  
  public static class TestCodec extends XasCodec.DecoderOnly {

    public Object decode(PeekableItemSource is, KeyIdentificationModel kim) 
      throws IOException {
      TestNode n=new TestNode();
      StartTag st = (StartTag) is.next();
      TestNode node = new TestNode();
      node.setId(kim.identify(st));
      return node;
    }
    
  }
  
  public static class TestNode implements RefTrees.IdentifiableContent {
    private Key id;
    public void setId(Key id) {
      this.id = id;
    }

    public Key getId() {
      return id;
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof TestNode) && 
        Util.equals(((TestNode) obj).id,id);
    }

    @Override
    public int hashCode() {
      return id == null ? 0 : id.hashCode();
    }
    
    
  }
  
  // XmlUtil stuff
  
  
}
// arch-tag: 68c998c99169e9e86042cbc676fc5c38 *-
