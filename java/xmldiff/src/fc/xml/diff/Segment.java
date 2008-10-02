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

// $Id: Segment.java,v 1.4 2006/01/20 13:29:34 ctl Exp $
package fc.xml.diff;

import java.util.List;

import static fc.xml.diff.Segment.Operation.*;

public class Segment<E> {

  public enum Operation {INSERT,COPY,UPDATE};

  private List<E> ins;
  private int off; // Offset refers to position in (copy) source list
  private int len; // Length of chunk
  private Operation op;
  private int pos; // position in destination list of segment
                   // (redundant, as it is computable by summing lengths for
                   // previous segments)

  protected Segment(int off, int len, Operation op, List<E> ins, int pos) {
    this.off =off;
    this.len = len;
    this.op = op;
    this.ins = ins;
    this.pos = pos;
  }

  public static final <F> Segment<F> createIns(int off, List<F> data, int pos) {
    return new Segment<F>(off,data.size(),INSERT,data,pos);
  }

  public static final <F> Segment<F> createCopy(int off, int len, int pos) {
    return new Segment<F>(off,len,COPY,null,pos);
  }

  public static final<F> Segment<F> createUpdate(int off, int len,
    List<F> data, int pos) {
    return new Segment<F>(off, len, UPDATE, data,pos);
  }

  public Operation getOp() {
    return op;
  }

  public int getOffset() {
    return off;
  }

  public int getLength() {
    return len;
  }

  public int getPosition() {
    return pos;
  }

  public List<E> getInsert() {
    return ins;
  }

  // Length of segment
  public int getInsertLen() {
    return op == COPY ? len : ins.size();
  }

  public String toString() {
    String insstr = ins != null ? ins.toString() : null;
    if( insstr != null && insstr.length() > 48 )
      insstr = insstr.substring(0,24)+"..."+insstr.substring(insstr.length()-24);
    return "<"+pos+": " + op + "," + off + "," + len +
      (op != COPY ? "," + insstr.replace('\n', '\u0000') + ">" : ">");
  }

  public boolean appendsTo(Segment<E> head) {
    return head.op == op && (head.off + head.len ) == off;
  }

  public <F extends E> void append(Segment<F> s, List<E> inserts) {
    len+=s.len;
    if( ins != null ) {
       ins = inserts.subList(off,off+len);
    }

  }
}
