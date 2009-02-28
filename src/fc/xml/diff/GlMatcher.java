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

// $Id: GlMatcher.java,v 1.20.2.1 2006/06/30 12:48:03 ctl Exp $
package fc.xml.diff;

import static fc.xml.diff.Segment.Operation.COPY;
import static fc.xml.diff.Segment.Operation.INSERT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import fc.util.log.Log;

/** Rolling window matcher. Should be wickedly fast for small changes.
 * Inspired by rsync algorithms.
 */

public class GlMatcher<E> {

  /* Experimental support for aligning on boundaries. Does not work well
  * in all cases. Consder the following (bounds on .)
  * base="bar. There is nothing like foobar."
  * matched=ins<"bar. There">, scanning for < is nothing... >
  * We match the scan to <There is nothing ...> -> "There" occurs twice in
  * the match.
  */
  public int[] tokenBoundaries;
  public int[] docTokenBounds;

  int falseHashMatches = 0;

  private HashAlgorithm<E> ha;

  public GlMatcher(HashAlgorithm<E> ha) {
    this.ha = ha;
  }

  public List<Segment<E>> match(List<E> base,
                             List<E> doc, int[] sizes ) {
    long timeB = System.currentTimeMillis();
    List<Segment<E>> baseList = new ArrayList<Segment<E>>();
    baseList.add(Segment.createIns(0, base, 0) );
    List<Segment<E>> matchList = new LinkedList<Segment<E>>();
    matchList.add(Segment.createIns(0, doc, 0) );
    int minSize = sizes[sizes.length-1];
    //Log.log("Base  at "+sizes[sizes.length-1]+": "+baseList.size(),Log.INFO);
    for(int b : sizes ) {
      findChunks(matchList, baseList, b, minSize );
      //Log.log("Base  after "+b+": "+baseList.size(),Log.INFO);
      //Log.log("Match at "+b+": "+matchList.size(),Log.INFO);
      //Log.log("False matches "+falseHashMatches,Log.INFO);

    }
    long timeS = System.currentTimeMillis();
    simplify(matchList,doc);
    long timeU = System.currentTimeMillis();
    //updatify(matchList,baseList,base.size());
    long timeE = System.currentTimeMillis();
    //Log.log("Final list\n"+matchList,Log.INFO);
    /*Log.log("Matching took " + (timeS - timeB) + "msec, simplify " +
            (timeU - timeS) + "msec, updatify "+
            (timeE - timeU) + "msec.", Log.INFO);*/
    assert(_testPositionConsistency(matchList));
    return matchList;
  }

  // Test position() consistency
  private static final <E> boolean _testPositionConsistency(List<Segment<E>> ml) {
    Log.log("testing position list consistency",Log.INFO);
    int pos = 0;
    for( Segment<E> s : ml ) {
      if( pos != s.getPosition() )
        return false;
      pos += s.getInsertLen();
    }
    return true;
  }

  public void simplify(List<Segment<E>> ml, List<E> doc) {
    Segment<E> prev = null;
    for( ListIterator<Segment<E>> li = ml.listIterator();li.hasNext();) {
      Segment<E> current = li.next();
      if( prev != null && current.appendsTo(prev)) {
        // Ops concatenate
        li.remove();
        prev.append(current,doc);
      } else
        prev = current;
    }
  }

  // Note: baseLen = "right wall offset", ie the first ix outside base
  public void updatify(List<Segment<E>> ml, List<Segment<E>> deletia,
                         int baseLen) {
    // Scan for copy(m1)-ins(i1)-copy(m1) pattern so that there exists
    // a deletia d1 in deletia so that end_of_m1+1=d1_start and
    // start_of_me=d1_end+1. Then we infer the update d1->i1
    for( int i=0;i<ml.size();i++) {
      Segment<E> prev = i > 0 ? ml.get(i-1) : null;
      Segment<E> current = ml.get(i);
      Segment<E> next = i + 1 < ml.size() ? ml.get(i+1) : null;
      //if( prev == null || next ==null )
      //  continue; // If we Dont allow align on doc start/end
      if (current.getOp() == INSERT &&
          ( prev == null || prev.getOp() == COPY) &&
          ( next == null  || next.getOp() == COPY ) ) {
        int rqstart = prev != null ? prev.getOffset()+prev.getLength() : 0;
        int rqend = next != null ? next.getOffset() : baseLen;
        if( rqend <= rqstart )
          continue; // Copys out of order in src
        // Scan deletia; FIXME: use lookup(ix)->del yes/no for max efficiency
        for( ListIterator<Segment<E>> j = deletia.listIterator();j.hasNext();) {
          Segment<E> del = j.next();
          if( del.getOffset() > rqstart )
            break; // Done, if we assume order by start
          if( del.getOffset() < rqstart )
            continue;
          if( del.getOffset() + del.getLength() < rqend )
            continue; // Another move chunk m3 in between in src: m1.d.m3.m2
          //if( del.getOffset() + del.getLength() > rqend )
          //  continue; // Require lengths to match (optional)
          assert( del.getOffset() + del.getLength() == rqend );
            // Description of assert above:
            // This should not happen; e.g. mmmmmddnnnn (m,n matched tokens)
            // and deletia. length != 2
            // Log.log("Deletia overflows its space",Log.ASSERTFAILED);
          j.remove(); // No longer deleted
          ml.set(i, Segment.createUpdate(del.getOffset(),
                                         del.getLength(), current.getInsert(),
                                         current.getPosition()));
        }
      }
    }
  }


  // Matches all INSOPS in currentOps to the regions in base
  // min match length is minSize
  // base is list of insert chunks!

  // COLLATE note: The original algorithm splitted unmatched inserts
  // into sequential INS chunks of chunkSize. The collate functionality
  // introduced in 1.16 does not split insert chunks (unless when a match
  // is found in between). The general thinking is that this will lead to
  // better matches at small chunk sizes as boundary effects will be
  // (i.e. we don't get a lot of small ins chunks over whose boundaries
  // we cant match)

  protected void findChunks(List<Segment<E>> currentOps,
                                     List<Segment<E>> base,
                                     int chunkSize, int minSize ) {
    int firstRegion = 0;
    int scanpos = -1; // -1 indicates that we need a new chunk from doclist
    Segment<E> m=null;
    Segment<E> collatedIns=null; // Used to collect multiple emitted sequential
       // ins ops into one single ins op. See note about collate above
    Segment<E> match=null;
    int collatedLen=-1;
    if( base.size() ==0 )
      return; // Nothing left in base to match!
    for( ListIterator<Segment<E>> i= currentOps.listIterator();
                              scanpos != -1 || i.hasNext();) {
      if( scanpos == -1 ) {
        m = i.next();
        if( m.getOp() == COPY ) {
          firstRegion = -m.getOffset();
          continue;
        }
        if(  m.getOp() == INSERT && m.getLength() < chunkSize   )
          continue;
        // We have an insert chunk to match
        i.remove();
        scanpos = 0;
      }
      assert m.getOp() == INSERT;
      // offlen[0] = off in base, offlen[1]=length
      // offset of match in m.getInsert() is scanpos
      int[] offlen = findChunkInRegions(scanpos, m.getInsert(), base, firstRegion,
                                        chunkSize, minSize );
      //int zinsSize = m.getInsert().size();
      if (offlen == null) {
        // not found
        int splitSize = chunkSize;
        if( docTokenBounds != null ) {
          int off = m.getOffset() + splitSize;
          int low = Arrays.binarySearch(docTokenBounds,off);
          if( low < 0 )
            low = (-low-1);
          splitSize = docTokenBounds[low]-m.getOffset();
          assert( splitSize > 0 );
        }
        boolean willSplit = m.getInsert().size()-scanpos > splitSize;
        int inssize =  willSplit ? splitSize :  m.getInsert().size() - scanpos;
        /*List<E>
          realins = m.getInsert().subList(scanpos, scanpos + inssize);*/
        if( collatedIns == null ) {
          // NOTE: This copy op is just a placeholder for scanpos; we emit it
          // as insert when collation is flushed
          collatedIns = Segment.<E> createCopy(scanpos, -1, scanpos);
          collatedLen = inssize;
        } else {
          // Should currentOps <ins> should always append back-to-back; check this
          //Log.log("Pos="+(collatedIns.getOffset())+", clen="
          //  +collatedLen+", scanpos="+scanpos+", m="+m,Log.INFO);
          assert(collatedIns.getOffset()+collatedLen == scanpos );
          collatedLen+= inssize;
        }
        if( willSplit ) {
          scanpos += splitSize;
        } else {
          scanpos = -1;
        }
      }
      else {
        match =
          Segment.<E>createCopy(offlen[0], offlen[1], m.getPosition()+scanpos);
        if ((scanpos+offlen[1]) < m.getInsert().size() )
          scanpos += offlen[1];
        else
          scanpos = -1; // need next
        //Log.log("New firstreg is "+offlen[2],Log.INFO);
        firstRegion = offlen[2];
      }
      // Flush any collated inserts
      if( collatedIns != null &&
          (scanpos == -1 || !i.hasNext() || match != null )) {
        int origin=collatedIns.getOffset();
        collatedIns = collatedIns.createIns(origin+m.getOffset(),
                      m.getInsert().subList(origin, origin + collatedLen),
                      origin+m.getPosition());
        i.add(collatedIns);
        collatedIns = null;
      }
      if( match != null ) {
        i.add(match);
        match=null;
      }
    }
    assert( collatedIns == null );
  }

  protected int[] findChunkInRegions(int scanpos,
                                     List<E> chunkToMatch,
                            List<Segment<E>> baseRegions, int firstRegion,
                            int chunkSize, int minSize ) {
    if( firstRegion < 0 ) {
      // firsRegion < 0 -> find first base rgion with offset >= that
      int offset = -firstRegion;
      firstRegion=0;
      while( firstRegion < baseRegions.size() &&
        baseRegions.get(firstRegion).getOffset() < offset )
        firstRegion++;
    }
    int bfly=0,maxbfly = //Integer.MAX_VALUE-1;
        chunkSize >= 8  ? Integer.MAX_VALUE : 2*chunkSize+2;
        // IDEA: Dynamic maxbfly depending on chunk size
    //Log.log("Looking for " + chunkToMatch.subList(scanpos, chunkToMatch.size()),
    //        Log.INFO);
    loop:
    for( int ofi=firstRegion;ofi<firstRegion+baseRegions.size();ofi++) {
      int i = (baseRegions.size()+bfly+firstRegion)%baseRegions.size();
      if( bfly <= 0) // 0,1,-1,2,-2,3,-3,...
        bfly=-bfly+1;
      else
        bfly=-bfly;
      if( bfly > maxbfly || bfly <-maxbfly )
        return null;
      Segment<E> region = baseRegions.get(i);
      //Log.log("Looking into ix "+i+":"+region,Log.INFO);
      // assuming off 0 is start of region...; offlen coordinates in region,
      // corresponding offset in chunkToMatch is ***offlen[0]+scanpos***
      int[] offlen = findChunk(scanpos, chunkToMatch,
                               region.getInsert(),
                               chunkSize, minSize );
      if( offlen != null ) {
        //Log.log("Found match @ "+offlen[0]+", len "+offlen[1] ,Log.INFO);
        // Found match; remove matching chunkToMatch from baseRegions
        //Log.log("Split (off,len="+offlen[0]+","+offlen[1]+"): "+region,Log.INFO);
        if(  tokenBoundaries != null ) {
          int start = -1, end = -1,
              regionMax = region.getOffset()+region.getLength(),
              regionMin = region.getOffset();
          Log.log("Unaligned match is "+region.getInsert().subList(offlen[0],offlen[0]+offlen[1]),Log.INFO);
          // Try alignment on token boundaries
          // Determine low
          {
            int origStart = offlen[0]+region.getOffset();
            int low = Arrays.binarySearch(tokenBoundaries, origStart);
            if ( low < 0) {
              // in between, se up low so we're between low and low+1
              low = ( -low) - 2; // low points to ix < low boundary
              assert( tokenBoundaries[low + 1] >= regionMin );
              int mid = (tokenBoundaries[low] + tokenBoundaries[low + 1]) / 2;
              start = origStart <= mid && tokenBoundaries[low]>=regionMin ?
                      tokenBoundaries[low] : tokenBoundaries[low + 1];

            } else
              start = origStart;
          }
          // Now, do it fo hi
          {
            int origEnd = offlen[0] + offlen[1] + region.getOffset();
            int hi = Arrays.binarySearch(tokenBoundaries, origEnd );
            if (hi < 0) {
              // in between hi and hi +1
              hi = ( -hi) - 2;
              assert( tokenBoundaries[hi] <= regionMax );
              int mid = (tokenBoundaries[hi] + tokenBoundaries[hi + 1]) / 2;
              end = origEnd > mid && tokenBoundaries[hi+1] <= regionMax ?
                    tokenBoundaries[hi+1] : tokenBoundaries[hi];
            } else
              end = origEnd;
          }
          if(  end <= start)
            continue loop; // Aligned one turned out to beinvalid; case end < start
                           // Happens if hi+1 was rejected, and hi+0 is too low

          offlen[0]=start-region.getOffset();
          offlen[1]=end-start;
          assert( offlen[0] >= 0 );
          assert( offlen[1] >= 0 );
          assert( offlen[0] < region.getLength() );
          assert( offlen[0] + offlen[1] <= region.getLength() );
          assert( region.getLength()==region.getInsert().size() );
          //Log.log("=== off "+offlen[0]+", len="+offlen[1],Log.INFO);
          Log.log("Aligned match is "+region.getInsert().subList(offlen[0],offlen[0]+offlen[1]),Log.INFO);
        }
        baseRegions.remove(i); // NOTE: i.remove() is wrong; watch out for this
                               // (Note similar cases below)
        if( offlen[0] > 0 ) {
          Segment<E> pre = Segment.createIns(region.getOffset(),
                                             region.getInsert().subList(0,
            offlen[0]), region.getOffset());
          //Log.log("Pre :"+pre,Log.INFO);
          baseRegions.add(i,pre); // i.add(pre);
          i++;
        }
        if( offlen[0]+offlen[1] < region.getLength() ) {
          int start = offlen[0] + offlen[1];
          Segment<E> post = Segment.createIns(start + region.getOffset(),
                                              region.getInsert().subList(
                                                start, region.getInsert().size()),
                                              start + region.getOffset());

          //Log.log("Post:"+post,Log.INFO);
          baseRegions.add(i,post); //i.add(post);
        }
        offlen[0]+=region.getOffset();
        return new int[] {offlen[0],offlen[1],i+1};
      }
    }
    return null; // no match
  }

  // finds the chunk chunk.sublist(scanpos,chunklen) in baseRegion
  // chunk is initially between minSize and chunkSize, depending on what
  // can fit inside the region. Upon match, the chunk is expanded maximally
  // returns offlen[0]=offset into baseRegion
  //         offlen[1]=length of match

  protected int[] findChunk(int scanpos, List<E> chunk,
                            List<E> baseRegion, int chunkSize, int minSize ) {
    //Log.log("findChunk: ",Log.INFO, new int[] {scanpos,chunk.length,baseRegion.length,maxsize});
    int len = Math.min(chunkSize, chunk.size() - scanpos);
    if (baseRegion.size() < len || len < minSize )
      return null; // Doesn't fit
    short[] froll = initroll(chunk, len, scanpos);
    short[] broll = initroll(baseRegion, len, 0);
    // Note i = match offset, i-1 is expunged
    for (int i = 0; i + len <= baseRegion.size(); i++) {
      if (froll[0] == broll[0] && froll[1] == broll[1]) {
        // Potential match @ i, length len
        int j=0;
        for(;j<len && chunk.get(scanpos+j).equals(baseRegion.get(i+j));j++)
          ; // Deliberately empty stmnt here!
        /*        Log.log("Potential match (chunk,base): \n"+new String(chunk,scanpos,len)+"\n"+
                        new String(baseRegion,i,len),Log.INFO);*/
        if ( j==len ) {
          // try expanding left
          int maxmore = Math.min(chunk.size() - len - scanpos,
                                 baseRegion.size() - i - len);
          int extra = 0;
          while (extra < maxmore &&
                 baseRegion.get(i + len + extra).equals(
                 chunk.get(scanpos + len + extra) ) )
            extra++;
          /*if( extra < maxmore ) {
            Log.log("Stopped due to 1:"+baseRegion.get(i + len + extra),Log.INFO);
            Log.log("Stopped due to 2:"+chunk.get(scanpos + len + extra),Log.INFO);
          }*/
          return new int[] {i, len + extra};
        } else
          /*Log.log("False matched j="+j+",len="+len+" event@chunk="+chunk.get(scanpos+j)+
      " event@base="+baseRegion.get(i+j)+" equals="+chunk.get(scanpos+j).equals(baseRegion.get(i+j))+
                  "\n"+chunk.subList(scanpos,scanpos+len)+" to\n"+baseRegion.subList(i,i+len),Log.INFO);*/
          falseHashMatches++;
      }
      // Expunge this byte, and chew next, unless on last (test-only) lap
      if( i + len < baseRegion.size() )
        updateroll(broll, baseRegion.get(i), baseRegion.get(i + len), i,
                   i + len );
    }
    return null; // no chunk
  }

  protected final short[] initroll(List<E> baseRegion, int len, int off ) {
    short a=0,b=0;
    // Make s(0,len-1)
    for( int i=0;i<len;i++) {
      short hash = ha.quickHash(baseRegion.get(i+off));
      a += hash;
      b += (len-i)*hash;
    }
    return new short[] {a,b};
  }

  protected final void updateroll(short[] state, E outT,
                            E inT, int k, int l) {
    short in = ha.quickHash( inT );
    short out = ha.quickHash( outT );
    /*state[0]+=in-out;
    short tmp =(short) (((short) (l-k))*out);
    short tmp2 = (short) (state[1]-tmp);
    state[1]=(short) (tmp2+state[0]);*/
    state[0]+=in-out;
    state[1]=(short) ((state[1]-((l-k)*out))+state[0]);
  }

}
