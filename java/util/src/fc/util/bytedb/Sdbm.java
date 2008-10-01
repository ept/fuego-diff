/*
 * License: Public Domain.
 */
package fc.util.bytedb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import fc.util.log.Log;

/**
 * Java rewrite of sdbm. sdbm - 
 * ndbm work-alike hashed database library based on 
 * Per-Aake Larson's Dynamic Hashing algorithms. BIT 18 (1978). 
 * original author: oz@nexus.yorku.ca 
 * status: Public Domain.
 * @author  Justin F. Chapweske <justin@chapweske.com>
 * @author Tancred Lindholm (adaption to Fuego Core project & Symbian)
 * @author Eemil Lagerspetz (adaption to Fuego Core project)
 * @version  .01 06/06/98  core routines
 */

public class Sdbm {

  public static final int DBLKSIZ = 4096;
  public static final int PBLKSIZ = 1024;
  public static final int PAIRMAX = 1008; //arbitrary on PBLKSIZ-N
  public static final int SPLTMAX = 10; //maximum allowed splits

  public static final int SHORTSIZ = 2;
  public static final int BITSINBYTE = 8;

  public static final String DIREXT = ".dir";
  public static final String PAGEXT = ".pag";

  RandomAccessFile dirRaf; // directory file descriptor
  RandomAccessFile pagRaf; // page file descriptor
  File dirFile;
  File pagFile;
  String mode;
  int maxbno; // size of dirfile in bits
  int curbit; // current bit number
  int hmask; // current hash mask
  Page page; // page file block buffer
  int dirbno; // current block in dirbuf
  byte[] dirbuf; // directory file block buffer
  Random rand = null;

  /**
   * @param name The name of the database, a name.pag and a name.dir file will be created.
   * @param mode The mode to open the database in, either "r" or "rw"
   */
  public Sdbm(File baseDir, String name, String mode) throws IOException {
    this.mode = mode;

    this.dirFile = new File(baseDir, name + DIREXT);
    this.pagFile = new File(baseDir, name + PAGEXT);

    dirRaf = new RandomAccessFile(dirFile, mode);
    pagRaf = new RandomAccessFile(pagFile, mode);
    dirbuf = new byte[DBLKSIZ];

    // need the dirfile size to establish max bit number.
    // zero size: either a fresh database, or one with a single,
    // unsplit data page: dirpage is all zeros.
    dirbno = dirRaf.length() == 0 ? 0 : -1;
    maxbno = (int) dirRaf.length() * BITSINBYTE;
    //System.out.println("MAXBNO:"+maxbno);
    //System.out.println("BITSINBYTE:"+BITSINBYTE);
    //System.out.println("size:"+dirRaf.length());
  }

  private static final void checkKey(byte[] key) {
    if (key == null) {
      throw new NullPointerException();
    } else if (key.length <= 0) {
      throw new IllegalArgumentException("key too small: " + key.length);
    }
  }

  private static final long OFF_PAG(int off) {
    return ((long) off) * PBLKSIZ;
  }

  private static final long OFF_DIR(int off) {
    return ((long) off) * DBLKSIZ;
  }

  private static final int masks[] = {
    000000000000, 000000000001, 000000000003, 000000000007,
    000000000017, 000000000037, 000000000077, 000000000177,
    000000000377, 000000000777, 000000001777, 000000003777,
    000000007777, 000000017777, 000000037777, 000000077777,
    000000177777, 000000377777, 000000777777, 000001777777,
    000003777777, 000007777777, 000017777777, 000037777777,
    000077777777, 000177777777, 000377777777, 000777777777,
    001777777777, 003777777777, 007777777777, 017777777777
  };

  /**
   * Close the database.
   */
  public synchronized void close() throws IOException {
    dirRaf.close();
    pagRaf.close();
  }

  /**
   * Get the value associated with the key, returns null if that
   * value doesn't exist.
   */
  public synchronized byte[] get(byte[] keyBytes) throws IOException {

    checkKey(keyBytes);

    //System.out.println(key);
    page = getPage(Hash.hash(keyBytes));
    //System.out.println(page.bno);
    //page.print();
    if (page == null) {
      return null;
    }
    return page.get(keyBytes);
  }

  /**
   * @param key the key to check.
   *
   * @return true if the dbm contains the key
   */
  public synchronized boolean containsKey(byte[] keyBytes) throws IOException {
    checkKey(keyBytes);

    page = getPage(Hash.hash(keyBytes));

    if (page == null) {
      return false;
    }
    return page.containsKey(keyBytes);
  }

  /**
   * Clear the database of all entries.
   */
  public synchronized void clear() throws IOException {
    if (!mode.equals("rw")) {
      throw new IOException("This file is opened Read only");
    }

    dirRaf.close();
    pagRaf.close();

    try {
      if (!dirFile.delete()) {
        throw new IOException("Unable to delete :" + dirFile);

      }
    } finally {
      if (!pagFile.delete()) {
        throw new IOException("Unable to delete :" + pagFile);
      }
    }

    dirRaf = new RandomAccessFile(dirFile, mode);
    pagRaf = new RandomAccessFile(pagFile, mode);
    // zero the dirbuf
    dirbuf = new byte[DBLKSIZ];

    curbit = 0;
    hmask = 0;
    page = null;

    // need the dirfile size to establish max bit number.
    // zero size: either a fresh database, or one with a single,
    // unsplit data page: dirpage is all zeros.
    dirbno = dirRaf.length() == 0 ? 0 : -1;
    maxbno = (int) dirRaf.length() * BITSINBYTE;
  }

  /**
   * Cleanes the dbm by reinserting the keys/values into a fresh copy.
   * This can reduce the size of the dbm and speed up many operations if
   * the database has become sparse due to a large number of removals.
   */
  public synchronized void clean() throws IOException {
    if (!mode.equals("rw")) {
      throw new IOException("This file is opened Read only");
    }

    if (rand == null) {
      rand = new Random();
    }

    // FIX use createTempFile instead.
    String name = "sdbmtmp" + rand.nextInt(Integer.MAX_VALUE);

    Sdbm tmp = new Sdbm(dirFile.getAbsoluteFile().getParentFile(),
        name, "rw");

    // Use a page enumerator to ensure that the elementCount is accurate,
    // considering that some pages may contain stale/invalid data.
    for (Enumeration en = pages(); en.hasMoreElements(); ) {
      Page p = (Page) en.nextElement();
      if (page == null) {
        page = p;
      }
      for (int i = 0; i < p.size(); i++) {
        byte[] key = p.getKeyAt(i);
        byte[] value = (byte[]) p.getElementAt(i);
        if (key != null && value != null) {
          tmp.put(key, value);
        }
      }
    }

    tmp.close();

    dirRaf.close();
    pagRaf.close();

    dirFile.delete();
    pagFile.delete();

    tmp.dirFile.renameTo(dirFile);
    tmp.pagFile.renameTo(pagFile);

    dirRaf = new RandomAccessFile(dirFile, mode);
    pagRaf = new RandomAccessFile(pagFile, mode);
    //zero the dirbuf
    dirbuf = new byte[DBLKSIZ];

    // need the dirfile size to establish max bit number.
    // zero size: either a fresh database, or one with a single,
    // unsplit data page: dirpage is all zeros.
    dirbno = dirRaf.length() == 0 ? 0 : -1;
    maxbno = (int) dirRaf.length() * BITSINBYTE;

    // re-count the elements because there may have been duplicate keys
    // in the old dbm.
  }

  /**
   * removes the value associated with the key
   * @returns the removed value, null if it didn't exist.
   */
  public synchronized String remove(byte[] keyBytes) throws IOException {
    checkKey(keyBytes);

    page = getPage(Hash.hash(keyBytes));
    if (page == null) {
      return null;
    }

    int n = page.size();
    byte[] removeBytes = page.remove(keyBytes);
    String val = null;
    if (removeBytes != null) {
      val = new String(removeBytes);
    }

    // update the page file
    pseek(pagRaf,OFF_PAG(page.bno));
    pagRaf.write(page.pag, 0, PBLKSIZ);

    return val;
  }

  /**
   * puts the value into the database using key as its key.
   * @returns the old value of the key.
   */
  public synchronized String put(byte[] keyBytes, byte[] value) throws IOException,
  SdbmException {
    checkKey(keyBytes);

    int need = keyBytes.length + value.length;
    // is the pair too big for this database ??
    if (need > PAIRMAX) {
      throw new SdbmException("Pair is too big for this database");
    }

    int hash = Hash.hash(keyBytes);

    page = getPage(hash);
    int bn = page.bno;
    // if we need to replace, delete the key/data pair
    // first. If it is not there, ignore.
    int n = page.size();
    byte[] valBytes = page.remove(keyBytes);
    String val = null;
    if (valBytes != null) {
      val = new String(valBytes);
    }

    /* 20070606 EL:
     * If the value hashes to a certain part of the current page and the page is split in makeRoom,
     * the pointer of the current page is updated and this value will be saved to the wrong
     * place. Fix: Get the new right page.
     */
    int times = 0;
    // if we do not have enough room, we have to split.
    while (!page.hasRoom(need)) {
      makeRoom(hash, need);
      page = getPage(hash);
      if (times != 0){
        Log.log("made room " + times +" times. Need " + need + " bytes.", Log.INFO);
      }
      times++;
    }
    
    // we have enough room or split is successful. insert the key,
    // and update the page file.
    page.put(keyBytes, value);
    //	page.print();

    /*      if( OFF_PAG(page.bno) > pagRaf.length() )
        Log.log("Seek beyoond eof",Log.FATALERROR);
      pagRaf.seek(OFF_PAG(page.bno));*/


    pseek(pagRaf,OFF_PAG(page.bno));
    pagRaf.write(page.pag, 0, PBLKSIZ);

    return val;
  }

  /**
   * @returns a random key from the dbm, null if empty.
   */
  /* syxaw-optimize-deadcode
    public synchronized String randomKey() throws IOException {
      Iterator it = randomKeys(1);
      return it.hasNext() ? (String) it.next() : null;
    }

    /**
   * @param n The number of desired keys.
   * @returns a number of random keys, up to n
   */
  /* syxaw-optimize-deadcode
    public synchronized Iterator randomKeys(int n) throws IOException {
      HashSet keys = new HashSet();

      if (rand == null) {
        rand = new Random();
      }

      // There is a chance that the dbm may be dirty and that there is
      // a misscount on the number of keys, or it is very sparse and thus
      // difficult to find random keys.  If this counter goes above N,
      // then clean the database before continueing.
      int i = 0;

      while (keys.size() < size() && keys.size() < n) {

        // The pages should be relatively balanced, so if we choose
        // a random value in a random page we should have a decent
        // distribution.

        Page p = null;
        do {
          // This dbm is not in good shape, clean it up.
          // This takes a long time, so don't do it often.
          if (i != 0 && i == 2 * Math.min(n, size())) {
            // FIX this should be augmented with a 'modified' flag
            // to avoid redundant cleans.
            clean();
          }

          i++;

          // Use rand to choose a random hash.
          p = getPage(rand.nextInt());
        }
        while (p.size() == 0);

        keys.add(new String(p.getKeyAt(rand.nextInt(p.size()))));

      }

      System.out.println("Took " + i + " iterations to find keys");

      return keys.iterator();
    }*/

  /**
   * makroom - make room by splitting the overfull page
   * this routine will attempt to make room for SPLTMAX times before
   * giving up.
   */
  private synchronized void makeRoom(int hash, int need) throws IOException,
  SdbmException {

    Page newPage;

    int smax = SPLTMAX;
    do {

      // Very important, don't want to write over newPage on loop.
      newPage = new Page(PBLKSIZ);
      // split the current page
      page.split(newPage, hmask + 1);

      // address of the new page
      newPage.bno = (hash & hmask) | (hmask + 1);

      // write delay, read avoidence/cache shuffle:
      // select the page for incoming pair: if key is to go to the new
      // page, write out the previous one, and copy the new one over,
      // thus making it the current page. If not, simply write the new
      // page, and we are still looking at the page of interest. current
      // page is not updated here, as put will do so, after it inserts
      // the incoming pair.
      if ( (hash & (hmask + 1)) != 0) {
        pseek(pagRaf,OFF_PAG(page.bno));
        pagRaf.write(page.pag, 0, PBLKSIZ);
        page = newPage;
      } else {
        /* if( OFF_PAG(newPage.bno) > pagRaf.length() )
            Log.log("Seek beyoond eof",Log.FATALERROR);
          pagRaf.seek(OFF_PAG(newPage.bno));*/
        pseek(pagRaf,OFF_PAG(newPage.bno));
        pagRaf.write(newPage.pag, 0, PBLKSIZ);
      }

      setdbit(curbit);

      // see if we have enough room now
      if (page.hasRoom(need))
        return;

      // try again... update curbit and hmask as getpage would have
      // done. because of our update of the current page, we do not
      // need to read in anything. BUT we have to write the current
      // [deferred] page out, as the window of failure is too great.
      curbit = 2 * curbit + ( (hash & (hmask + 1)) != 0 ? 2 : 1);
      hmask |= hmask + 1;

      pseek(pagRaf,OFF_PAG(page.bno));
      pagRaf.write(page.pag, 0, PBLKSIZ);

    }
    while (--smax != 0);

    // if we are here, this is real bad news. After SPLTMAX splits,
    // we still cannot fit the key. say goodnight.
    throw new SdbmException("AIEEEE! Cannot insert after SPLTMAX attempts");
  }

  /**
   * returns an enumeration of the pages in the database.
   */
  private Enumeration pages() {
    return new PageEnumerator();
  }

  class PageEnumerator implements Enumeration {
    int blkptr;
    PageEnumerator() {
    }

    public boolean hasMoreElements() {
      synchronized (Sdbm.this) {
        //If we're at the end of the file.
        try {
          if (OFF_PAG(blkptr) >= pagRaf.length()) {
            return false;
          }

        } catch (IOException e) {
          return false;
        }
        return true;
      }
    }

    public Object nextElement() {
      synchronized (Sdbm.this) {
        if (!hasMoreElements()) {
          throw new NoSuchElementException("PageEnumerator");
        }
        Page p = new Page(PBLKSIZ);
        if (page == null || page.bno != blkptr) {
          try {
            pseek(pagRaf,OFF_PAG(blkptr));
            readLots(pagRaf, p.pag, 0, PBLKSIZ);
          } catch (IOException e) {
            throw new NoSuchElementException(e.getMessage());
          }
        } else {
          p = page;
        }

        if (!p.isValid() || p == null)
          throw new NoSuchElementException("PageEnumerator");
        blkptr++;
        return p;
      }
    }
  }

  /**
   * returns an enumeration of the keys in the database.
   */
  public synchronized Enumeration keys() {
    return new Enumerator(true);
  }

  /**
   * returns an enumeration of the elements in the database.
   */
  public synchronized Enumeration elements() {
    return new Enumerator(false);
  }

  /**
   * @author  lagerspe
   */
  class Enumerator implements Enumeration {
    boolean key;
    Enumeration penum;
    Page p;
    Enumeration eenum;
    byte[] /*String*/ next;

    Enumerator(boolean key) {
      this.key = key;
      penum = pages();
      if (penum.hasMoreElements()) {
        p = (Page) penum.nextElement();
        eenum = key ? p.keys() : p.elements();
        next = getNext();
      } else {
        next = null;
      }
    }

    public boolean hasMoreElements() {
      synchronized (Sdbm.this) {
        return next != null;
      }
    }

    /**
     * @return  the next
     * @uml.property  name="next"
     */
    private byte[] /*String*/ getNext() {
      for (; ; ) {
        if (! (penum.hasMoreElements() || eenum.hasMoreElements())) {
          return null;
        }
        if (eenum.hasMoreElements()) {
          byte[] b = (byte[]) eenum.nextElement();
          if (b != null) {
            return b ; //new String(b);
          }
        } else if (penum.hasMoreElements()) {
          p = (Page) penum.nextElement();
          eenum = key ? p.keys() : p.elements();
        }
      }
    }

    public Object nextElement() {
      synchronized (Sdbm.this) {
        byte[] /*String*/ s = next;
        if (s == null) {
          throw new NoSuchElementException("Enumerator");
        }
        next = getNext();
        return s;
      }
    }
  }

  /**
   * all important binary tree traversal
   */
  protected synchronized Page getPage(int hash) throws IOException {
    int hbit = 0;
    int dbit = 0;
    int pagb;
    Page newPage;
    //System.out.println("maxbno:"+maxbno);
    //System.out.println("hash:"+hash);

    while (dbit < maxbno && getdbit(dbit) != 0) {
      dbit = 2 * dbit + ( (hash & (1 << hbit++)) != 0 ? 2 : 1);
    }

    //System.out.println("dbit: "+dbit+"...");

    curbit = dbit;
    hmask = masks[hbit];

    pagb = hash & hmask;

    //System.out.println("pagb: "+pagb);
    // see if the block we need is already in memory.
    // note: this lookaside cache has about 10% hit rate.
    if (page == null || pagb != page.bno) {

      pseek(pagRaf,OFF_PAG(pagb));
      byte[] b = new byte[PBLKSIZ];
      readLots(pagRaf, b, 0, PBLKSIZ);
      newPage = new Page(b);

      if (!newPage.isValid()) {
        // FIX maybe there is a better way to deal with corruption?
        // Corrupt page, return an empty one.
        b = new byte[PBLKSIZ];
        newPage = new Page(b);
      }

      newPage.bno = pagb;

      //System.out.println("pag read: "+pagb);
    } else {
      newPage = page;
    }

    return newPage;
  }

  protected synchronized int getdbit(int dbit) throws IOException {
    int c;
    int dirb;

    c = dbit / BITSINBYTE;
    dirb = c / DBLKSIZ;

    if (dirb != dirbno) {
      pseek(dirRaf,OFF_DIR(dirb));
      readLots(dirRaf, dirbuf, 0, DBLKSIZ);

      dirbno = dirb;

      //System.out.println("dir read: "+dirb);
    }

    return dirbuf[c % DBLKSIZ] & (1 << dbit % BITSINBYTE);
  }

  protected synchronized void setdbit(int dbit) throws IOException {
    int c = dbit / BITSINBYTE;
    int dirb = c / DBLKSIZ;

    if (dirb != dirbno) {
      clearByteArray(dirbuf);
      pseek(dirRaf,OFF_DIR(dirb));
      readLots(dirRaf, dirbuf, 0, DBLKSIZ);

      dirbno = dirb;

      //System.out.println("dir read: "+dirb);
    }

    dirbuf[c % DBLKSIZ] |= (1 << dbit % BITSINBYTE);

    if (dbit >= maxbno)
      maxbno += DBLKSIZ * BITSINBYTE;

    pseek(dirRaf,OFF_DIR(dirb));
    dirRaf.write(dirbuf, 0, DBLKSIZ);

  }

  public static void clearByteArray(byte[] arr) {
    for (int i = 0; i < arr.length; i++) {
      arr[i] = 0;
    }
  }

  public static void readLots(RandomAccessFile f, byte[] b, int off,
      int len) throws IOException {
    int n = 0;
    while (n < len) {
      int count = f.read(b, off + n, len - n);
      n += count;
      if (count < 0) {
        break;
      }
    }
  }

  public void print() throws IOException {
    System.out.print("[");
    for (Enumeration en = keys(); en.hasMoreElements(); ) {
      byte[] key = (byte[]) en.nextElement();
      System.out.print(key + "=" + get(key));
      if (en.hasMoreElements()) {
        System.out.print(",");
      }
    }
    System.out.println("]");
  }

  // FUTURE: Cache length to improve performance?
  //private long pagRafLen = 0l;
  //private long dirRafLen = 0l;
  /* pseek disabled on Desktop machines for size reasons. Sparse file support
   * is assumed. See fc.dessy.SdbmStressTest for reasons.
   */
  /**
   * Protected seek. Pseek fills the are between end of file and seek_off
   * with zeroes. While mandated behavior by POSIX, and de-facto-behavior
   * on Windows, Symbian does not perform zero-fills automatically, so
   * on that platform we need to take care of it ourselves.
   */
  private final void pseek(RandomAccessFile f, long seek_off) throws
  IOException {
    // FIXME: (Or don't fix) We expect sparse file support in this branch.
    /*
    long currentLen = f.length();
    if (seek_off > currentLen) {
      // Zero it out
      //Log.log("Zero padding, size= " + (seek_off - currentLen), Log.INFO);
      f.seek(currentLen);
      // BUGFIX-20070118-1: Cap size of zero fill array. When expanding large
      // databases, it may be that seek_off >> currentLen, and thus we cannot
      // alloc a byte[] of size seek_off-currentLen. Fixes OutOfMemory 
      // problems with large dbs.
      long toWrite = seek_off - currentLen;
      byte[] zeroes = toWrite > DBLKSIZ ? new byte[DBLKSIZ]: new byte[(int) toWrite];
      do {
        int w = toWrite > zeroes.length ? zeroes.length: (int) toWrite;
        f.write(zeroes, 0, w);
        toWrite -= zeroes.length;
        currentLen+=w;
        //f.seek(currentLen); // ensure that next loop starts at the correct position
      } while( toWrite > 0);
      // No need to seek; write already did that
      //Log.debug("Currently at "+f.getFilePointer()+", seek to "+seek_off);
    } else {*/
      f.seek(seek_off);
    //}
  }

  static class SdbmException extends java.io.IOException {

      public SdbmException() {
        super();
      }

      public SdbmException(String s) {
        super(s);
      }
    }

  static class MemorySdbm {

    static Map dbs = new HashMap();

    Map ix;

    public MemorySdbm(File baseDir, String name, String mode)
        throws IOException {
      String key = (new File(baseDir, name)).getCanonicalPath();
      ix = (Map) dbs.get(key);
      if (ix == null) {
        ix = new HashMap();
        dbs.put(key, ix);
      }
    }

    public boolean containsKey(String k) throws IOException {
      return ix.containsKey(k);
    }

    public void put(String k, byte[] data) throws IOException {
      ix.put(k, data);
    }

    public byte[] remove(String k) throws IOException {
      return (byte[]) ix.remove(k);
    }

    public byte[] get(String k) throws IOException {
      return (byte[]) ix.get(k);
    }

    public Enumeration keys() {
      final Iterator i = ix.keySet().iterator();
      return new Enumeration() {
        public boolean hasMoreElements() {
          return i.hasNext();
        }

        public Object nextElement() {
          return i.next();
        }
      };
      // return
    }

  }

  /**
   * Java rewrite of sdbm. sdbm - ndbm work-alike hashed database library
   * based on Per-Aake Larson's Dynamic Hashing algorithms. BIT 18 (1978).
   * original author: oz@nexus.yorku.ca status: public domain. keep it that
   * way.
   * 
   * @author Justin Chapweske (justin@chapweske.com)
   * @version .01 06/06/98 hashing routine
   */
  static class Hash {

    /**
     * polynomial conversion ignoring overflows [this seems to work remarkably
     * well, in fact better then the ndbm hash function. Replace at your own
     * risk] use: 65599 nice. 65587 even better.
     */

    public static final int hash(byte[] b) {
      // Optimized ver of the one below, not sure if its worth it on modern
      // CPUs
      int n = 0;
      int len = b.length;

      for (int i = 0; i < len; i++) {
        n = b[i] + (n << 6) + (n << 16) - n;
      }
      return n;

    }
    /*
     * int n = 0; int len = b.length;
     * 
     * for (int i=0;i<len;i++) { n = b[i] + 65599 *n; } return n; }
     */
  }

  /**
   * Java rewrite of sdbm sdbm - ndbm work-alike hashed database library based on
   * Per-Aake Larson's Dynamic Hashing algorithms. BIT 18 (1978). original author:
   * oz@nexus.yorku.ca status: public domain.
   * 
   * @author Justin F. Chapweske <justin@chapweske.com>
   * @version .01 06/06/98 page-level routines
   */
  
  static class Page implements Cloneable {
  
    public byte[] pag;
    public int bno; //FIX this should be seperate from the page.
    public int pageSize;
  
    public Page(int pageSize) {
      this.pageSize = pageSize;
      this.pag = new byte[pageSize];
    }
  
    public Page(byte[] b) {
      this.pageSize = b.length;
      this.pag = b;
    }
  
    protected Object clone() {
      byte[] b = new byte[pag.length];
      System.arraycopy(pag, 0, b, 0, pag.length);
      return new Page(b);
    }
  
    /**
     * Returns a short from two bytes with MSB last (little endian)
     */
    private short getIno(int i) {
      return (short) ( ( (pag[2 * i + 1] & 0xff) << 8) | (pag[2 * i] & 0xff));
    }
  
    /**
     * Sets a short from two bytes with MSB last (little endian)
     */
    private void setIno(int i, short val) {
      pag[2 * i + 1] = (byte) ( (val >>> 8) & 0xff);
      pag[2 * i] = (byte) (val & 0xff);
    }
  
    /**
     * <pre>
     * page format:
     *	    +------------------------------+
     * ino  | n | keyoff | datoff | keyoff |
     * 	    +------------+--------+--------+
     *	    | datoff | - - - ---->	   |
     *	    +--------+---------------------+
     *      |	    F R E E A R E A        |
     *	    +--------------+---------------+
     *	    |  <---- - - - | data          |
     *	    +--------+-----+----+----------+
     *	    |  key   | data     | key      |
     *	    +--------+----------+----------+
     *
     * </pre>
     *
     * calculating the offsets for free area:  if the number
     * of entries (ino[0]) is zero, the offset to the END of
     * the free area is the block size. Otherwise, it is the
     * nth (ino[ino[0]]) entry's offset.
     */
    public boolean hasRoom(int need) {
      int n;
      int off;
      int free;
  
      off = ( (n = getIno(0)) > 0) ? getIno(n) : pageSize;
      free = off - (n + 1) * Sdbm.SHORTSIZ;
      need += 2 * Sdbm.SHORTSIZ;
  
      //System.out.println("free "+new Integer(free)+" need "+
      //new Integer(need));
  
      return need <= free;
    }
  
    public byte[] put(byte[] key, byte[] val) {
      // Remove any previous values
      remove(key);
  
      if (!hasRoom(key.length + val.length)) {
        throw new IllegalStateException
        ("Not enough room for : key=" + new String(key) + ",val=" +
            new String(val) + " required: " + key.length +" + " + val.length );
      }
  
      int n;
      int off;
  
      off = ( (n = getIno(0)) > 0) ? getIno(n) : pageSize;
  
      // enter the key first
      off -= key.length;
      System.arraycopy(key, 0, pag, off, key.length);
      setIno(n + 1, (short) off);
  
      // now the data
      off -= val.length;
      System.arraycopy(val, 0, pag, off, val.length);
      setIno(n + 2, (short) off);
  
      // adjust item count
      setIno(0, (short) (getIno(0) + 2));
      return val;
    }
  
    public byte[] get(byte[] key) {
      int i;
      if ( (i = indexOfValue(key)) == -1) {
        return null;
      }
      byte[] b = new byte[getIno(i) - getIno(i + 1)];
      System.arraycopy(pag, getIno(i + 1), b, 0, getIno(i) - getIno(i + 1));
      return b;
    }
  
    public byte[] getKeyAt(int n) {
      if (n >= size()) {
        throw new ArrayIndexOutOfBoundsException(n);
      }
  
      int off = getIno(n * 2 + 1);
      int len = (n == 0 ? pageSize : getIno( (n * 2 + 1) - 1)) - off;
  
      byte[] b = new byte[len];
      System.arraycopy(pag, off, b, 0, len);
      return b;
    }
  
    public byte[] getElementAt(int n) {
      if (n >= size()) {
        throw new ArrayIndexOutOfBoundsException(n);
      }
  
      int off = getIno(n * 2 + 2);
      int len = getIno( (n * 2 + 2) - 1) - off;
  
      byte[] b = new byte[len];
      System.arraycopy(pag, off, b, 0, len);
      return b;
    }
  
    public Enumeration keys() {
      return new Enumerator(true);
    }
  
    public Enumeration elements() {
      return new Enumerator(false);
    }
  
     public byte[] remove(byte[] key) {
       int n;
       int i;
  
       if ( (n = getIno(0)) == 0) {
         return null;
       }
       if ( (i = indexOfValue(key)) == -1) {
         return null;
       }
  
       byte[] val = new byte[getIno(i) - getIno(i + 1)];
       System.arraycopy(pag, getIno(i + 1), val, 0, getIno(i) - getIno(i + 1));
       // found the key. if it is the last entry
       // [i.e. i == n - 1] we just adjust the entry count.
       // hard case: move all data down onto the deleted pair,
       // shift offsets onto deleted offsets, and adjust them.
       // [note: 0 < i < n]
       if (i < n - 1) {
         int m;
         int dst = (i == 1 ? pageSize : getIno(i - 1));
         int src = getIno(i + 1);
         int zoo = dst - src;
  
         //System.out.println("free-up "+zoo);
  
         //  shift data/keys down
         m = getIno(i + 1) - getIno(n);
         System.arraycopy(pag, src - m, pag, dst - m, m);
  
         //adjust offset index up
         while (i < n - 1) {
           setIno(i, (short) (getIno(i + 2) + zoo));
           i++;
         }
       }
       setIno(0, (short) (getIno(0) - 2));
       return val;
     }
  
     /**
      * @param key The key to check for existance
      *
      * @return true if the page contains the key
      */
     public boolean containsKey(byte[] key) {
       return indexOfValue(key) != -1;
     }
  
     /**
      * search for the key in the page.
      * return offset index in the range 0 < i < n.
      * return -1 if not found.
      */
     public int indexOfValue(byte[] key) {
       int n;
       int i;
       int off = pageSize;
       int siz = key.length;
  
       if ( (n = getIno(0)) == 0) {
         return -1;
       }
  
       //	System.out.println("Key:"+key);
       //print();
       for (i = 1; i < n; i += 2) {
         //System.out.println("siz:"+new Integer(siz));
         //System.out.println("off-ino:"+new Integer(off-getIno(i)));
         //if (siz == off - getIno(i)) {
         //System.out.println("key?:"+new String(pag).
         //		   substring(getIno(i),getIno(i)+siz).equals(key));
         //}
         if (siz == off - getIno(i) &&
             byteArraysEqual(pag, getIno(i), key, 0, siz)) {
           return i;
         }
         off = getIno(i + 1);
       }
       return -1;
     }
  
     /**
      * @return true if the page is empty.
      */
     public boolean isEmpty() {
       return size() == 0;
     }
  
     /**
      * @return the number of key/value pairs in this page
      */
     public int size() {
       return getIno(0) / 2;
     }
  
     /**
      * Fast function to compare two byte arrays, starts
      * from back because my strings are dissimilar from the
      * end
      */
     private static boolean byteArraysEqual(byte[] arr1, int start1, byte[] arr2,
         int start2, int len) {
       for (int i = len - 1; i >= 0; i--) {
         if (arr1[start1 + i] != arr2[start2 + i]) {
           return false;
         }
       }
       return true;
     }
  
     public static void clearByteArray(byte[] arr) {
       for (int i = 0; i < arr.length; i++) {
         arr[i] = 0;
       }
     }
  
     public void split(Page newPage, int sbit) {
       byte[] key;
       byte[] val;
  
       int n;
       int off = pageSize;
       Page cur = new Page(pageSize);
  
       System.arraycopy(pag, 0, cur.pag, 0, pageSize);
       clearByteArray(pag);
       clearByteArray(newPage.pag);
  
       n = cur.getIno(0);
       for (int i = 1; n > 0; i += 2) {
         key = new byte[off - cur.getIno(i)];
         System.arraycopy(cur.pag, cur.getIno(i), key, 0, off - cur.getIno(i));
         //Log.log("Key read in split is "+Util.getHexString(key),Log.INFO);
         val = new byte[cur.getIno(i) - cur.getIno(i + 1)];
         System.arraycopy(cur.pag, cur.getIno(i + 1), val, 0,
             cur.getIno(i) - cur.getIno(i + 1));
  
         //System.out.println("Hash:"+Hash.hash(key));
         // select the page pointer (by looking at sbit) and insert.
         Page p = (Hash.hash(key) & sbit) != 0 ? newPage : this;
         p.put(key, val);
  
         off = cur.getIno(i + 1);
         n -= 2;
       }
  
       //	System.out.println((short) cur.getIno(0)/2+" split "+
       //   (short) newPage.getIno(0)/2+"/"+(short) getIno(0)/2);
     }
  
     /**
      * check page sanity:
      * number of entries should be something
      * reasonable, and all offsets in the index should be in order.
      * this could be made more rigorous.
      */
     public boolean isValid() {
       int n;
       int off;
  
       if ( (n = getIno(0)) < 0 || n > pageSize / Sdbm.SHORTSIZ)
         return false;
  
       if (n > 0) {
         off = pageSize;
         for (int i = 1; n > 0; i += 2) {
           if (getIno(i) > off || getIno(i + 1) > off ||
               getIno(i + 1) > getIno(i))
             return false;
           off = getIno(i + 1);
           n -= 2;
         }
       }
       return true;
     }
  
     public boolean equals(Object obj) {
       if (! (obj instanceof Page)) {
         return false;
       }
       Page other = (Page) obj;
       return other.pag.length == pageSize &&
       byteArraysEqual(other.pag, 0, pag, 0, pageSize);
     }
  
     public void print() {
       int n = getIno(0);
       System.out.println("Num of Elements :" + n);
       for (int i = 1; i <= n; i++) {
         System.out.println
         ("[" + i + "] -> " + getIno(i) + " : " +
             (getIno(i) == 0 ? "" : new String(pag, getIno(i), (i == 1) ?
                 pageSize - getIno(i) :
                   getIno(i - 1) - getIno(i))));
       }
     }
     /**
      * @author  lagerspe
      */
     class Enumerator implements Enumeration {
        boolean key;
        int i = 0;
        byte[] next;
  
        Enumerator(boolean key) {
          this.key = key;
          next = getNext();
        }
  
        public boolean hasMoreElements() {
          return next != null;
        }
  
        /**
         * @return  the next
         * @uml.property  name="next"
         */
        private byte[] getNext() {
          byte[] b = null;
          if (i < size()) {
            b = key ? getKeyAt(i) : getElementAt(i);
            i++;
          }
          return b;
        }
  
        public Object nextElement() {
          byte[] b = next;
          if (b == null) {
            throw new NoSuchElementException("Enumerator");
          }
          next = getNext();
          return b;
        }
     }
  }  
}