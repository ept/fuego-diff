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

// $Id: DirTreeGenerator.java,v 1.3 2006/02/27 09:12:54 ctl Exp $
package fc.xml.diff.test;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.AttributeNode;
import fc.xml.xas.Item;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.typing.TypedItem;
import fc.xml.xmlr.AbstractMutableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.NodeNotFoundException;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.RefTreeNodeImpl;
import fc.xml.xmlr.RefTrees;
import fc.xml.xmlr.model.KeyIdentificationModel;
import fc.xml.xmlr.model.KeyModel;
import fc.xml.xmlr.model.StringKey;
import fc.xml.xmlr.test.XasTests.AbstractItemTransform;
import fc.xml.xmlr.xas.PeekableItemSource;
import fc.xml.xmlr.xas.UniformXasCodec;

/* Generator for directory trees. This code is a light refactoring of the
 dirtree testing generator found in Syxaw 
 NOTE: The code is ugly in its mixed use of strings and StringKeys. There's
 no reason t use both.
 */

public class DirTreeGenerator {

  public static final int NO_VERSION = -1;

  private static final long RND_SEED = 42L;
  private static Random rndz =new Random(RND_SEED);

  public static long idGen = 0L;
  public static final Key ROOT_ID = StringKey.createKey(idGen);

  public DirTreeGenerator() {
  }

  public static void permutateTree(fc.xml.xmlr.MutableRefTree
                                   t, long ops, String pdf, double
                                   deleteTreeProb, final Random rnd) {
    SortedSet treeNodes = new TreeSet<String>(new Comparator() {
      final int seed = rnd.nextInt(); // Gives a quite random order -- hopefully...
      public boolean equals(Object o1,Object o2) {
        return compare(o1,o2)==0;
      }
      public int compare(Object o1, Object o2) {
        return (o1.hashCode()^seed)-(o2.hashCode()^seed);
      }
    });
    treeSet(t,t.getRoot(),treeNodes,'+');
    Object pos = treeNodes.last();
    int redos =0;
    boolean redo = false;
    char op='Q';
    for(;ops>0;ops--) {
      op = redo ? op :
          pdf.charAt(rnd.nextInt(pdf.length()));
      redo = false;
      // The selection proc below may choose a file only once, but a dir as
      // many times as it has files. Also, the probability of a dir is
      // weighted by the numbers of files in it
      String rndId = (String) pos;
      RefTreeNode rndNode =  t.getNode(StringKey.createKey( rndId ));
      RefTreeNode rndDirNode = rndNode;
      int  dscan = treeNodes.size();
      while (
          ( (DirectoryEntry) rndDirNode.getContent()).getType() ==
          DirectoryEntry.FILE) {
        pos = nextPos(pos, treeNodes);
        rndDirNode = (RefTreeNode) t.getNode( StringKey.createKey( (String) pos ) );
        dscan--;
        if(dscan<0)
          Log.log("No dir found",Log.ASSERTFAILED);
      }
//      treeNodes.remove(rndId);
//      Log.log("Picked "+rndId,Log.INFO);
      // Get next pos...what a kludge
      Key nId = nextId();
      try {
        redo:
        switch (op) {
          case 'i': // File ins
            t.insert(rndDirNode.getId(), nId,
                     new DirectoryEntry(nId, fileName(nId.toString(),rnd),
                                        DirectoryEntry.FILE));
//            Log.log("ins-f @ "+rndDirNode.getId()+" ",Log.INFO, t.getNode(nId).getContent());
            treeNodes.add(nId.toString());
            break;
          case 'I': // Dir ins
            t.insert(rndDirNode.getId(), nId,
                     new DirectoryEntry(nId, dirName(nId.toString(),rnd),
                                        DirectoryEntry.DIR));
//            Log.log("ins-d @ "+rndDirNode.getId()+" ",Log.INFO,t.getNode(nId).getContent());
            treeNodes.add(nId.toString());
            break;
          case 'd': // delete
            // Travel to bottom
            for( Iterator i = rndNode.getChildIterator();i.hasNext() &&
                 (rndNode = (RefTreeNode) i.next()) == null;);
///            while (rndNode.firstChild() != null)
//              rndNode = rndNode.firstChild();
              // Travel upwards with delTree probability
            while (rnd.nextDouble() < deleteTreeProb)
              rndNode = rndNode.getParent();
//            Log.log("Del ",Log.INFO,rndNode.getContent());
            /*{
              RefTreeNode _n = rndNode;
              while (_n != null && !ROOT_ID.equals(_n.getId())) {
                _n = _n.getParent();
              }
              if (_n == null) {
                Log.log("Working around ChangeTree delete bug",Log.WARNING);
                break;
              }
            } */
            if( rndNode == null || ROOT_ID.equals(rndNode.getId()) )
              break; // Never delete root
            {
              Set deletia = new HashSet();
              treeSet(t, rndNode, deletia, '+');
              try {
                t.delete(rndNode.getId());
              } catch ( IllegalArgumentException ex ) {
                Log.log("Ignoring ChangeTree delete bug",Log.WARNING);
                break;
              }
              treeSet(t, rndNode, treeNodes, '-');
              /*Sweep delete
               for( Iterator i = deletia.iterator(); i.hasNext(); ) {
                t.delete((String) i.next());
              }*/
            }
            break;
          case 'u':
            DirectoryEntry c = (DirectoryEntry) rndNode.getContent();
            String newName = c.getType() == DirectoryEntry.FILE ?
                fileName(nextId().toString(),rnd) : 
                  dirName(nextId().toString(),rnd);
            //Log.log("Upd "+newName+", old=",Log.INFO,rndNode.getContent());
            ///c.setName(newName);
            DirectoryEntry nc = new DirectoryEntry(c);
            nc.setName(newName);
            t.update(rndNode.getId(),nc);
            break;

          case 'm':
            Key moveNode = StringKey.createKey((String) treeNodes.first());
            if( ROOT_ID.equals(moveNode) ) {
              redo = true;
              break; // Never move root
            }
            RefTreeNode  newParent = rndDirNode;
            // Check for cyclic move
            while( newParent != null ) {
              if( newParent.getId().equals(moveNode) ) {
                redo = true;
                break redo;
              }
              newParent = newParent.getParent();
            }
//            Log.log("mov "+moveNode+" below "+rndDirNode.getId(),Log.INFO);
            t.move(moveNode,rndDirNode.getId());
            break;
          default:
            Log.log("Invalid op "+op,Log.ASSERTFAILED);
        }
        if( redo ) {
          //Log.log("Redoing failed "+op,Log.INFO);
          ops++;
          redos++;
          if( redos > 10 ) {
            redo = false;
            redos = 0;
            Log.log("10 redos failed, giving up", Log.INFO);
          }
        }
        pos = nextPos(pos,treeNodes).toString();
      } catch (NodeNotFoundException ex) {
        Log.log("Selected nonexisting node",Log.FATALERROR);
      }
    } // End for
  }

  private static void treeSet(fc.xml.xmlr.MutableRefTree
                              t, RefTreeNode root, Set s, char op) {
    switch (op) {
      case '+':
        s.add(root.getId().toString());
        break;
      case '-':
        s.remove(root.getId().toString());
        break;
      default:
        Log.log("Invalid op " + op, Log.ASSERTFAILED);
    }
    for (Iterator i = root.getChildIterator(); i.hasNext(); ) {
      treeSet(t, (RefTreeNode) i.next(), s, op);
    }
  }

  public static MutableRefTree randomDirTree(long nodes, long nodesPerDir, double dirProb,
                                             double variance, double dirVariance,
                                             Random rnd) {
    Key id = nextId();
    MutableRefTree t = new DirTreeGenerator.MutableRefTree(id,
                                          new DirectoryEntry(id, null,
        DirectoryEntry.TREE));
    try {
      LinkedList roots = new LinkedList();
      roots.add(t.getRoot().getId());
      while (nodes > 0 && roots.size() > 0) {
        Key parentId =  (StringKey) roots.removeFirst();
        long dents = (long) (rnd.nextGaussian() * variance) + nodesPerDir;
        dents=Math.max(1,dents);
        long dirs = (long) (rnd.nextGaussian() * dirVariance + dents * dirProb);
        dirs=Math.max(1,dirs);
        long files = dents - dirs;
        if( files + dirs > nodes )
          dirs = 0;
        if( files > nodes )
          files = nodes;
        // Add dirs
        nodes -= (dirs+files);
        for (; dirs > 0; dirs--) {
          Key nId = nextId();
          t.insert(parentId, nId , new DirectoryEntry(nId, dirName(nId.toString(),rnd),
              DirectoryEntry.DIR));
          roots.addLast(nId);
        }
        // Add files
        for (; files > 0; files--) {
          Key nId = nextId();
          t.insert(parentId, nId , new DirectoryEntry(nId, fileName( nId.toString(),rnd ),
              DirectoryEntry.FILE));
        }
      }
    } catch (NodeNotFoundException ex) {
      Log.log("Tree construction error",Log.ASSERTFAILED,ex);
    }
    return t;
  }

  public static String fileName(String id, Random rnd) {
    final String[] fnames = {"foo","bar","baz","quup","ding","dong",
        "jabber","wocky","armadillo","gnu","gnat"};
    final String[] exts = {"c","java","txt","h","doc","xml","gif","jpg",
        "tmp","class","ps","tex"};
    return fnames[rnd.nextInt(fnames.length)]+"-"+id+"."+
        exts[rnd.nextInt(exts.length)];
  }

  public static String dirName(String id, Random rnd) {
    final String[] dnames = {"bin","share","doc","linux","src","cache",
    "sbin","home","texmf"};
    return dnames[rnd.nextInt(dnames.length)]+"-"+id;
  }


  public static Key nextId() {
    return StringKey.createKey(idGen++);
  }

  protected static Object nextPos(Object pos, SortedSet s) {
    try {
      SortedSet tmp = s.subSet(s.first(), pos);
      //Log.log("tmp="+tmp,Log.INFO);
      return tmp.size() == 0 ? s.last() : tmp.last();
    } catch( IllegalArgumentException ex ) {
      // Happens when old s.first() was removed so pos becomes first
    }
    return s.first();
  }

  /*
  public static class DirNodeContent implements DirectoryEntry {

    private Key id;
    private String name;
    private int type;

    public DirNodeContent() {
      this(null,null,-1);
    }

    public DirNodeContent(DirNodeContent src) {
      id = src.id;
      name = src.name;
      type = src.type;
    }


    public DirNodeContent(Key aId, String aName, int aType) {
      id = aId;
      name = aName;
      type = aType;
    }

    public void setId(Key id) {
      this.id = id;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setType(int type) {
      this.type = type;
    }

    public Key getId() {
      return id;
    }

    public String getName() {
      if( type == DirectoryEntry.TREE ) // The tree has no name attr!
        return null;
      return name;
    }

    public int getType() {
      return type;
    }

    public boolean equals(Object o) {
      return o instanceof DirectoryEntry && (
          Util.equals(((DirectoryEntry) o).getId(),getId()) &&
          Util.equals(((DirectoryEntry) o).getName(),getName()) &&
          ((DirectoryEntry) o).getType()==getType()
          );
    }

     // dummies
     public String getLocationId() {
       return "#lid#";
     }

     public String getNextId() {
       return "#nextid#";
     }

     public String getUid() {
       return "#uid#-"+getId();
     }

     public int getVersion() {
       return 0;
     }

    public String getLinkNextId() {
      return "#lnextid#";
    }

    public String getLinkUid() {
      return "#luid#";
    }

    public int getLinkVersion() {
      return 1;
    }

    public Key getLinkId() {
      return id;
    }

//    public boolean getIsFile() {
//      return TYPE_FILE.equals(getType());
//    }
  }*/

  public static class MutableRefTree extends AbstractMutableRefTree {

  private Map index =new HashMap();

  private RefTreeNodeImpl root = null;

  public MutableRefTree(Key rootId, Object content) {
    root = new RefTreeNodeImpl(null,rootId,content);
    index.put(rootId,root);
  }


  public MutableRefTree(RefTree initTree) {
    root = (RefTreeNodeImpl) initTree.getRoot();
    init(root);
  }

  // The MutableRefTree iface ------
  public RefTreeNode getNode(Key id) {
    return (RefTreeNode) index.get(id);
  }

  public void delete(Key id) throws NodeNotFoundException {
    RefTreeNodeImpl n = (RefTreeNodeImpl) index.get(id);
    if( n == null )
      throw new NodeNotFoundException(id);
    RefTreeNodeImpl p = (RefTreeNodeImpl) n.getParent();
    if( index.get(p.getId()) == null )
      throw new NodeNotFoundException(id); // Parent was previously deleted!

    if( p == null )
      root = null;
    else {
      p.removeChild(n);
      index.remove(id);
    }

  }

  public Key insert(Key parentId, long pos, Key newId, Object content)
      throws NodeNotFoundException {
    if( content == null || pos != MutableRefTree.DEFAULT_POSITION )
      Log.log("Invalid op",Log.ASSERTFAILED);
    RefTreeNodeImpl n = (RefTreeNodeImpl) index.get(parentId);
    if( n == null )
      throw new NodeNotFoundException(parentId);
    RefTreeNodeImpl newNode = new RefTreeNodeImpl(n,newId,content);
    if( index.put(newId,newNode) != null )
      Log.log("Duplicate id",Log.ASSERTFAILED);
    n.addChild(newNode);
    return newId;
  }

  public Key move(Key nodeId, Key parentId, long pos) throws NodeNotFoundException {
    if( pos != MutableRefTree.DEFAULT_POSITION )
      Log.log("Invalid op",Log.ASSERTFAILED);
    RefTreeNodeImpl n = (RefTreeNodeImpl) index.get(nodeId);
    if( n == null )
      throw new NodeNotFoundException(nodeId);
    RefTreeNodeImpl pNew = (RefTreeNodeImpl) index.get(parentId);
    if( pNew == null )
      throw new NodeNotFoundException(parentId);
    RefTreeNodeImpl p = (RefTreeNodeImpl) n.getParent();
    if( p == null )
      Log.log("Tried to move root",Log.ASSERTFAILED);
    p.removeChild(n);
    pNew.addChild(n);
    //n.setParent(pNew);
    return n.getId();
  }

  public boolean update(Key nodeId, Object content) throws NodeNotFoundException {
    if( content == null )
      Log.log("Invalid op",Log.ASSERTFAILED);
    RefTreeNodeImpl n = (RefTreeNodeImpl) index.get(nodeId);
    if( n == null )
      throw new NodeNotFoundException(nodeId);
    if( !content.equals(n.getContent()) ) {
      n.setContent(content);
      return true;
    }
    return false;
  }

  public RefTreeNode getRoot() {
    return root;
  }

  private void init(RefTreeNode root) {
    if (index.put(root.getId(), root) != null)
      Log.log("Duplicate ids", Log.ASSERTFAILED);
    for (Iterator i = root.getChildIterator(); i.hasNext(); )
      init( (RefTreeNode) i.next());
  }

  }

  public static class EntryDecoder extends AbstractItemTransform {

    public static final Qname DUMMY_TYPE=new Qname("","");
    public static final Qname ID_ATTR=new Qname("","id");
    public static final Qname NAME_ATTR=new Qname("","name");
    
    public void append(Item i) throws IOException {
      if( i.getType() == Item.START_TAG ) {
        StartTag t = (StartTag) i;
        String ts = t.getName().getName();
        String id = t.getAttribute(ID_ATTR).getValue().toString();
        AttributeNode name = t.getAttribute(NAME_ATTR); 
        DirectoryEntry de = new
          DirectoryEntry( StringKey.createKey(id),
              name == null ? null : name.getValue().toString(),
              "file".equals(ts) ? DirectoryEntry.FILE : ("tree".equals(ts) ? 
                  DirectoryEntry.TREE : DirectoryEntry.DIR ));
        queue.offer(i);
        queue.offer(new TypedItem(DUMMY_TYPE,de));
      } else {
        queue.offer(i);
        //Log.info("Skipping item ",i);
      }
    }

  }
  
  public static class DirTreeModel implements UniformXasCodec {
    
    KeyModel okim = null; // Overriding kim to get proper class for
      // de.getId(), when the node key is not correct
    EntryDecoder ed = new EntryDecoder();
    
    public DirTreeModel(KeyModel okim) {
      this.okim = okim;
    }

    public DirTreeModel() {
    }
    
    public Object decode(PeekableItemSource is, KeyIdentificationModel kim)
      throws IOException {
      //assert kim != null;
      // A bit kludge-ish .. we way we re-use the sequence encoder...
      Item i = is.next();
      ed.append(i);
      ed.next(); // The untouched ST
      // FIXME: A dummy wrapper node for the content is really a bit ugly
      // OTOH, we quite nicely got key decode here!
      DirectoryEntry de = (DirectoryEntry) ((TypedItem) ed.next()).getValue();
      //de.setId((okim == null ? kim : okim) .identify(i));
      de.setId((okim == null ? kim : okim).makeKey(de.getId().toString()));
      return de;
    }
    
    public void encode(ItemTarget t, RefTreeNode n, StartTag ctx) throws IOException {
      DirectoryEntry e = (DirectoryEntry) n.getContent();
      StartTag st = new StartTag(new Qname("",
          e.getType() == DirectoryEntry.FILE ? "file" : 
            (e.getType() == DirectoryEntry.DIR ? "directory" : "tree")),ctx);
      if(e.getType() != DirectoryEntry.TREE )
        st.addAttribute( EntryDecoder.NAME_ATTR, e.getName());
      st.addAttribute( EntryDecoder.ID_ATTR, e.getId().toString());
      t.append(st);
    }

    public int size() {
      return 1;
    }

  }  
  public static class DirectoryEntry implements RefTrees.IdentifiableContent {

    /** Constant indicating missing type. */
    public static final int NONE = -1;

    /** Constant indicating directory tree root entry. */
    public static final int TREE = 1;

    /** Constant indicating directory type entry. */
    public static final int DIR= 2;

    /** Constant indicating file type entry. */
    public static final int FILE= 3;

    private Key id;
    private String name;
    private int type;

    public DirectoryEntry() {
      this(null,null,-1);
    }

    public DirectoryEntry(DirectoryEntry src) {
      id = src.id;
      name = src.name;
      type = src.type;
    }


    public DirectoryEntry(Key aId, String aName, int aType) {
      id = aId;
      name = aName;
      type = aType;
    }

    public void setId(Key id) {
      this.id = id;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setType(int type) {
      this.type = type;
    }

    public Key getId() {
      return id;
    }

    public String getName() {
      if( type == DirectoryEntry.TREE ) // The tree has no name attr!
        return null;
      return name;
    }

    public int getType() {
      return type;
    }

    public boolean equals(Object o) {
      return o instanceof DirectoryEntry && (
          Util.equals(((DirectoryEntry) o).getId(),getId()) &&
          Util.equals(((DirectoryEntry) o).getName(),getName()) &&
          ((DirectoryEntry) o).getType()==getType()
          );
    }

    public int hashCode() {
      return type^(name==null ? 0 : name.hashCode())^id.hashCode();
    }
    
     // dummies
     public String getLocationId() {
       return "#lid#";
     }

     public String getNextId() {
       return "#nextid#";
     }

     public String getUid() {
       return "#uid#-"+getId();
     }

     public int getVersion() {
       return 0;
     }

    public String getLinkNextId() {
      return "#lnextid#";
    }

    public String getLinkUid() {
      return "#luid#";
    }

    public int getLinkVersion() {
      return 1;
    }

    public String getLinkId() {
      return id.toString();
    }

    public String toString()  {
      return (type==FILE ? "file" :
        (type==DIR ? "dir" : "tree "))+
          "{name="+name+",...}";
    }
  }


}
