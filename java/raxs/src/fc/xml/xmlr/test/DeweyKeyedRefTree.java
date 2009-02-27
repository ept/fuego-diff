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

import java.util.Iterator;

import fc.util.log.Log;
import fc.xml.xmlr.IdAddressableRefTree;
import fc.xml.xmlr.Key;
import fc.xml.xmlr.KeyMap;
import fc.xml.xmlr.RefTree;
import fc.xml.xmlr.RefTreeNode;
import fc.xml.xmlr.Reference;
import fc.xml.xmlr.XmlrDebug;
import fc.xml.xmlr.xas.DeweyKey;

/** Present the underlying reftree using Dewey keys.
 * 
 * @author ctl
 *
 */
public class DeweyKeyedRefTree implements RefTree, KeyMap {

  RefTree t;
  DeweyKey root;
  IdAddressableRefTree it;
  
  public DeweyKeyedRefTree( RefTree t, DeweyKey root ) {
    this.t = t;
    this.root = root;
    if( t instanceof IdAddressableRefTree )
      it = (IdAddressableRefTree) t;
  }

  public DeweyKeyedRefTree( RefTree t) {
    this(t,DeweyKey.ROOT_KEY);
  }


  public RefTreeNode getRoot() {
    return new Node(null,(RefTreeNode) t.getRoot(),0);
  }
  
  private class Node implements RefTreeNode {
    
    RefTreeNode n;
    int pos;
    Node parent;
    DeweyKey k=null;
    
    public Node(Node parent, RefTreeNode n, int pos) {
      this.n = n;
      this.parent = parent;
      this.pos = pos;
      if( parent != null )
        k=parent.k.child(pos);
      else
        k= root; // DeweyKey.ROOT_KEY; 
         // new DeweyKey( fc.xml.xas.index.DeweyKey.initial()  );
    }

    public Key getId() {
      return k;
    }

    public RefTreeNode getParent() {
      return parent;
    }

    public Object getContent() {
      return n.getContent();
    }

    public Iterator getChildIterator() {
      final Iterator c = n.getChildIterator();
      final Node foo=this;
      return new Iterator() {
        int pos=0;
        public boolean hasNext() {
          return c.hasNext();
        }
        
        public Object next() {
          return new Node(foo,(RefTreeNode) c.next(),pos++);
        }
        
        public void remove() {
          c.remove();
        }
      };
    }
    
    public boolean isReference() {
      return n.isReference();
    }

    public boolean isTreeRef() {
      return n.isTreeRef();
    }

    public boolean isNodeRef() {
      return n.isNodeRef();
    }

    public Reference getReference() {
      return n.getReference();
    }    
  }

  public Key getBackKey(Key frontKey) {
    DeweyKey dk = (DeweyKey) frontKey;
    dk = dk.replaceAncestorSelf(root, DeweyKey.ROOT_KEY);
    int[] indexes=dk.deconstruct();
    RefTreeNode r = getRoot();
    for( int i = 0; i<indexes.length;i++) {
      for( Iterator ni = r.getChildIterator();--indexes[i]>0;)
        r=(RefTreeNode) ni.next();
    }
    return r.getId();
  }

  public Key getFrontKey(Key backKey) {
    Log.info("Getting front key for "+backKey);
    RefTreeNode n = it.getNode(backKey);
    DeweyKey dk = DeweyKey.ROOT_KEY;
    for(RefTreeNode p=n.getParent();p!=null;p=n.getParent()) {
      int pos = 0;
      for( Iterator i=p.getChildIterator();i.hasNext();) {
        if( n.equals( i.next() ) ) {
          //Log.debug("Replacing root of "+dk+" with initial "+pos+" to get",dk.replaceAncestorSelf(DeweyKey.ROOT_KEY, DeweyKey.topLevel(pos)));
          dk = dk.replaceAncestorSelf(DeweyKey.ROOT_KEY, DeweyKey.topLevel(pos));
          pos = -1;
          break;
        }
        pos++;
      }
      if( pos != -1 ) {
        Log.fatal("Not found");
        return null;
      }
      n=p;
    }
    dk = dk.replaceAncestorSelf(DeweyKey.ROOT_KEY, root);
    //Log.info("==The key is "+dk+" and the node there is "+
    //    RefTrees.getAddressableTree(this).getNode(dk).getContent() );
    //XmlrDebug.dumpTree(this);
    Log.info("The key is "+dk);
    return dk;  
  }
}

// arch-tag: 0394f321-dde0-4f21-8888-979ea81e9333
