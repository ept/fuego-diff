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

package fc.xml.diff.encode;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.StartTag;
import fc.xml.xas.TransformSource;
import fc.xml.xas.transform.NsPrefixFixer;
import fc.xml.xmlr.NodeReference;
import fc.xml.xmlr.model.StringKey;
import fc.xml.xmlr.xas.RefItem;
import fc.xml.xmlr.xas.RefNodeItem;
import fc.xml.xmlr.xas.RefTreeItem;

public class XmlDiffEncoder extends RefTreeEncoder {

  protected ItemSource getOuputTransform(ItemSource is) {
    return new TransformSource(
        new TransformSource( is, new RefEStoDiffES() ), new NsPrefixFixer() );
  }

  /** ReferenceEs to Diff es. The input es ids are assumed to be absolute
   * XPaths of the form /n1/n2.../nm. The XPaths in the output are either
   * full paths, or relative to the parent.
   */

  public static class RefEStoDiffES implements ItemTransform {
    
    protected StartTag rootTag = null; 
    protected Queue<Item> queue = new LinkedList<Item>();
    private int state = 0; // 0 = normal, 1 = collecting treerefs
    private int run = 1;
    private String copySrc = null;
    private RefItem prevRef = null;
  
    private Stack<String> parentPaths = new Stack<String>();
  
    public RefEStoDiffES() {
    }
    
    
    public boolean hasItems () {
      return !queue.isEmpty();
    }
  
    public Item next ()  {
      return queue.poll();
    }
  
    public void append (Item ev) throws IOException {
      if( state == 1) {
        if( ev.getType() == RefTreeItem.TREE_REFERENCE &&
            appends(prevRef.getTarget().toString(),
                ((RefItem) ev).getTarget().toString() ) ) {
          run++;
          // BUGFIX070619-10: Fix broken path tracking
          prevRef=((RefItem) ev);
          return;
        } else {
          // Did not append/was not ref, emit seq
          StartTag st = new StartTag(fc.xml.xmlr.tdm.Diff.DIFF_COPY_TAG,rootTag);
          st.addAttribute(fc.xml.xmlr.tdm.Diff.DIFF_CPYSRC_ATTR, copySrc);
          st.addAttribute(fc.xml.xmlr.tdm.Diff.DIFF_CPYRUN_ATTR, 
              String.valueOf(run));
          EndTag et = new EndTag(st.getName());
          queue.add(st);
          queue.add(et);
          state = 0;
          prevRef = null; 
          run = -1;
        } 
      }
      assert state == 0;
      switch( ev.getType() ) {
        case Item.START_DOCUMENT:
          queue.add(ev);
          // FIXME-20061113-2: Namespace prefixes
          rootTag = new StartTag(fc.xml.xmlr.tdm.Diff.DIFF_ROOT_TAG);
          rootTag.ensurePrefix(fc.xml.xmlr.tdm.Diff.DIFF_NS,
            fc.xml.xmlr.tdm.Diff.DIFF_NS_PREFIX);
          //
          rootTag.addAttribute(fc.xml.xmlr.tdm.Diff.DIFF_ROOTOP_ATTR, 
              fc.xml.xmlr.tdm.Diff.DIFF_ROOTOP_INS);
          queue.add(rootTag);
          parentPaths.push(null); // For convenience
          break;
        case RefTreeItem.TREE_REFERENCE: 
          run = 1;
          copySrc = getRelativePath(parentPaths.peek(),
              ((RefItem) ev).getTarget().toString());
          prevRef = (RefItem) ev;
          state = 1;
          break;
        case RefNodeItem.NODE_REFERENCE: 
          RefNodeItem ni = (RefNodeItem) ev;
          if( !ni.isEndTag() )  {
            // FIXME-20061212-2
            queue.add(RefItem.makeStartItem(
                new NodeReference( StringKey.createKey( 
                    getRelativePath(parentPaths.
                    peek(), ni.getTarget().toString()))),null /*ni.getContext()*/));
            String longPath = ni.getTarget().toString();
            parentPaths.push(longPath);
          } else {  
            parentPaths.pop();
            queue.add(ev);
          }
          break;
        case Item.END_DOCUMENT:
          EndTag et = new EndTag(fc.xml.xmlr.tdm.Diff.DIFF_ROOT_TAG);
          queue.add(et);
          parentPaths.pop();
          break;
        default:
          queue.add(ev);
      }
      
    }
        
    
    // Generates ./n1/[..]/n2 or full path
    private String getRelativePath(String parent, String path) {
      if( parent != null && path.startsWith(parent) )
        return "."+path.substring(parent.length());
      return path;
    }
  
    /*
    private boolean appends(String baseId, String nextId) {
      Log.debug("appends("+baseId+","+nextId+"):"+_appends(baseId, nextId));
      return _appends(baseId, nextId);
    }*/
    
    private boolean appends(String baseId, String nextId) {
      int sep = baseId.lastIndexOf('/');
      if( sep == -1 || nextId.length() < sep ||
          sep != nextId.lastIndexOf('/') ||
          !nextId.startsWith(baseId.substring(0,sep)) )
        return false;
      if( Integer.parseInt(baseId.substring(sep+1))+1 !=
          Integer.parseInt(nextId.substring(sep+1)) )
        return false;
      return true;
    }
  }
 
}
// arch-tag: fee7d40c-388c-487f-9cd3-fcd8bce2ffe9
//
