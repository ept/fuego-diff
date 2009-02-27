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

import static fc.xml.diff.Segment.Operation.INSERT;
import static fc.xml.diff.Segment.Operation.UPDATE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import fc.util.StringUtil;
import fc.util.Util;
import fc.xml.diff.Segment;
import fc.xml.xas.Item;

public class AlignEncoder implements DiffEncoder {

  public void encodeDiff(List<Item> base, List<Item> doc,
      List<Segment<Item>> matches, List<Item> preamble, OutputStream out)
      throws IOException {
    encodeDiff(base,doc,matches,preamble,out,DEFAULT_PT,DEFAULT_PT);
  }

  public static <E> void encodeDiff(List<E> base,
                                    List<E> doc,
                                    List<Segment<E>> matches,
                                    List<E> preamble,
                                    OutputStream out,
                                    PosTransformer lpt, PosTransformer rpt ) {
    PrintWriter pw = new PrintWriter(out);
    for( int pos =0;pos<base.size(); ) {
      // find corresponding match (+trailing ins)
      Segment<E> match=null;
      boolean found = false;
      for( Segment<E> s: matches ) {
        if( s.getOp() == INSERT && match != null) {
          // dump ins (ins dumped after copies)
          for( int i=0;i<s.getInsert().size(); i++ )
            emitLine(pw,-1,"-",s.getInsert().get(i),s.getOp()==UPDATE,
                    s.getPosition()+i,lpt,rpt);
        } else if( s.getOffset() == pos && s.getOp() != INSERT ) {
          found = true;
          match = s;
          int slen = s.getLength();
          for(int i=0;i<slen;i++) {
            Object updated = null;
            if( s.getOp() == UPDATE )
              updated = i<s.getInsert().size()  ?
                 s.getInsert().get(i) : "-";
            else
              updated = base.get(pos);
            if( i>2 && slen > 5 && i < slen - 2 &&  s.getOp() != UPDATE ) {
              if( i<5 )
                pw.println(StringUtil.format(".",-(EVENT_COLWIDTH+POS_COLWIDTH+2)));
            } else
              emitLine(pw,pos, base.get(pos), updated,
                      s.getOp() == UPDATE, s.getPosition()+i,lpt,rpt);
            pos++;
          }
          // update && ins > basematch
          if( s.getOp() == UPDATE ) {
            for( int i=s.getLength();i<s.getInsert().size(); i++ )
              emitLine(pw,-1,"-",s.getInsert().get(i),s.getOp()==UPDATE,
                      s.getPosition()+i,lpt,rpt);
          }
        } else
          match = null;
      }
      if( !found ) {
        // Dump del
        emitLine(pw,pos,base.get(pos), "-",false,-1,lpt,rpt);
        pos++;
      }
    }
    pw.flush();
  }

  public static final int EVENT_COLWIDTH=40;
  public static final int POS_COLWIDTH=6;

  private static void emitLine( PrintWriter out, int pos, Object base,
                                Object mod, boolean update, int rpos,
         PosTransformer lpt, PosTransformer rpt ) {
    String baseS = Util.toPrintable(base.toString());
    String brS = Util.toPrintable(mod.toString());
    if( baseS.length() > EVENT_COLWIDTH )
      baseS = baseS.substring(0,EVENT_COLWIDTH/2)
              +"..."+baseS.substring(baseS.length()-(EVENT_COLWIDTH/2-3));
    if( brS.length() > EVENT_COLWIDTH )
      brS = brS.substring(0,EVENT_COLWIDTH/2)+"..."+
            brS.substring(brS.length()-(EVENT_COLWIDTH/2-3));
    out.println(StringUtil.format(lpt.transform(pos), -POS_COLWIDTH, ' ') +
                   " " +
                   StringUtil.format(baseS,-EVENT_COLWIDTH,' ') +
                   " " +
                   StringUtil.format(brS,EVENT_COLWIDTH,' ') + (update ? " *" : "  ") +
                   StringUtil.format(rpt.transform(rpos), POS_COLWIDTH, ' '));
  }

  public static final PosTransformer DEFAULT_PT = new DefaultPosTranformer();


}
// arch-tag: 7d4784b9-0276-42e4-8cd4-c40d38c18c43
//
