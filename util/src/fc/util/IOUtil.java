/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Random;

import fc.util.log.Log;

/** IO utlities.
* @author Tancred Lindholm
*/
public class IOUtil {

    /** Stream close convenience method. If the given stream is not 
     * <code>null</code>, it is closed. If an exception occurs due to the
     * close, this is logged as a fatal error. 
     * @param in stream to close
     */
    public static void closeStream(InputStream in) {
	if( in != null ) {
	    try {
		in.close();
	    } catch( IOException ex) {
		Log.fatal("Cannot close input stream",ex);
	    }
	}
    }
    
    /** Stream close convenience method. If the given stream is not 
     * <code>null</code>, it is closed. If an exception occurs due to the
     * close, this is logged as a fatal error. 
     * @param in stream to close
     */
    public static void closeStream(OutputStream in) {
	if( in != null ) {
	    try {
		in.close();
	    } catch( IOException ex) {
		Log.fatal("Cannot close output stream",ex);
	    }
	}
    }
    /**
     * Size of byte buffer used when copying streams. Currently set to
     * {@value}
     */
    private static final int COPY_BUF_SIZE = 4096;
    private static LinkedList<byte[]> buffers = new LinkedList<byte[]>();

    
    private static Random rnd = new Random();
    private static Boolean canMoveOver=null;
    
    public static File getTempName(File file ) throws IOException {
	return getTempName(file.getParentFile(),file.getName(),".bak");
    }
    
    public static File getTempName(File dir, String prefix, String suffix) 
    	throws IOException {
	int left=10;
	prefix = Util.isEmpty(prefix) ? "" : prefix+"-";
	for(File f;(f=new File(dir,prefix+Integer.toHexString(
		((rnd.nextInt()&0xffff)|0x1000))+suffix))!=null;left--) {
	    if( left <= 0 )
		throw new IOException("Cannot make temporary name");
	    if( !f.exists() )
		return f;
	}
	assert false; // Not reached
	return null;
    }

    public static File createTempFile(File f) throws IOException {
	File nf = getTempName(f);
	if( !nf.createNewFile() )
	    throw new IOException("Cannot create "+f);
	return nf;
	
    }    
    
    public static File createTempFile(File dir, String prefix, String suffix) 
    	throws IOException {
	File f = getTempName(dir, prefix, suffix);
	if( !f.createNewFile() )
	    throw new IOException("Cannot create "+f);
	return f;
    }

    /** Replaces a file with another atomically. 
     * 
     * @param srcFile source file
     * @param targetFile target file
     * @return <code>true</code> on success
     * @throws IOException if replacement fails
     */
    
    public static boolean replace(File srcFile, File targetFile) 
    	throws IOException { 
    	backup(srcFile,targetFile);
	return true;
    }
    
    public static File backup(File f) throws IOException {
	return backup(f,getTempName(f));
    }    
    
    // Implementation note: tries to make an atomic backup by renaming f to
    // tmp. If the OS supports moving over an existing name, the name is
    // allocated before moving over. The move over capability of the host OS
    // is automagically determined.
    
    public static File backup(File f, File tmp) throws IOException {
	Log.debug("Atomically moving "+f+" to "+tmp);
	determineMoveOver(tmp);
	boolean tmpExists = tmp.exists();
	assert canMoveOver != null;
//	if( canMoveOver && !tmpExists ) 
//	    Log.debug("Creating (for move over)",tmp);
	if( canMoveOver && !tmpExists && !tmp.createNewFile() ) {
	    throw new IOException("Cannot create "+tmp);
	} else if ( canMoveOver && tmpExists ) {
	    ; // All is well, just move over it
	} else if( tmpExists && !tmp.delete() ) {
	    // Tmp is created, but OS cannot move over, so we need to delete it
	    throw new IOException("Cannot delete "+tmp);
	}
	if( !f.renameTo(tmp) ) {
	    if( canMoveOver && !tmp.delete() )
		    throw new IOException("Cannot delete "+tmp);		
	    throw new IOException("Cannot replace "+f+" with "+tmp+
		    (canMoveOver ? "(by move over)" : ""));
	}
	return tmp;
    }

    // NOTE: contents in tmp will not be deleted, if it already exists
    private static boolean determineMoveOver(File tmp) throws IOException {
	boolean tmpExists = tmp.exists();
	boolean tmpWasMade = false;
	if( canMoveOver == null ) {
	    // Determine canMoveOver on this platform
	    File tmp2 = null;
	    try {
		if( !tmpExists ) {
		    if( !tmp.createNewFile() )
			throw new IOException("Cannot create "+tmp);
		    Log.debug("Created",tmp);
		    tmpWasMade =true;
		}
		tmp2 = getTempName(tmp.getParentFile(),tmp.getName(),".tmp");
		if( !tmp2.createNewFile() )
		    throw new IOException("Cannot create "+tmp);
		File origTmp = new File(tmp.getPath()); 
//		Log.debug("Renaming (move over test) "+tmp+" to "+tmp2);
		canMoveOver = tmp.renameTo(tmp2) ? Boolean.TRUE : Boolean.FALSE;
//		if( canMoveOver )
//		    Log.debug("Renaming (move over cleanup) "+tmp2+" to "
//			    +origTmp);
		if( canMoveOver && !tmp2.renameTo(origTmp) ) {
		   
		    Log.fatal("Cannot move potentially valuable data in "+tmp2+
			    "back to file "+origTmp);
		    throw new IOException("Cannot move "+tmp2+" to "+origTmp);
		}
	    } finally {
//		if( tmp2.exists() )
//		    Log.debug("Attempting delete of "+tmp2);
		if( tmp2.exists() && !tmp2.delete() )
		    Log.fatal("Cannot delete temp file" + tmp2);
		if( canMoveOver == null && !tmpWasMade && tmp.exists() )
		    Log.debug("Attempting delete of "+tmp);
		if( canMoveOver == null && tmp.exists() && !tmp.delete())
		    Log.fatal("Cannot delete temp file" + tmp);
	    }
	    Log.debug("Host OS move over capability is",canMoveOver);
	}
	return tmpWasMade;
    }

    /**
         * Copy an input stream to an output stream with maximum length
         * @param src Source stream
         * @param dest Destination stream
         * @param maxLeft maximum number of bytes to copy
         * @throws IOException if an error occurs while copying
         */
    public static int copyStream (InputStream src, OutputStream dest,
            long maxLeft) throws IOException {
        byte[] buffer = IOUtil.getBuf();
        int total = 0, count = 0;
        do {
            int maxchunk = (int) (maxLeft > buffer.length ? buffer.length
        	    : maxLeft);
            count = src.read(buffer, 0, maxchunk);
            if (count > 0) {
        	dest.write(buffer, 0, count);
        	total += count;
        	maxLeft -= count;
            }
        } while (count > -1 && maxLeft > 0);
        IOUtil.freeBuf(buffer); // Will be gc'd if not returned, so we needn't
        // protect by try/catch
        return total;
    }

    /**
         * Copy an input stream to an output stream.
         * @param src Source stream
         * @param dest Destination stream
         * @throws IOException if an error occurs while copying
         */
    public static int copyStream (InputStream src, OutputStream dest)
            throws IOException {
        return copyStream(src, dest, Long.MAX_VALUE);
    }

    public static void copyFile(File src, File dst ) throws IOException {
	FileInputStream fin = null;
	FileOutputStream fout = null;
	try {
	    try {
		fin = new FileInputStream(src);
		fout = new FileOutputStream(dst);
		copyStream(fin,fout);
	    } finally {
		if( fout != null )
		    fout.close();
	    } 
	} finally {
	    if( fin != null )
		fin.close();	    
	}
    }
    
    static synchronized void freeBuf (byte[] buf) {
        buffers.addLast(buf);
    }

    static synchronized byte[] getBuf () {
        if (buffers.size() > 0) {
            return buffers.removeFirst();
        } else {
            return new byte[COPY_BUF_SIZE];
        }
    }

    /** Delete directory tree. Deletes the
	 *  directory tree rooted at <code>f</code> including <code>f</code>.
	 *
	 * @param f root of tree to delete
	 * @throws IOException if an I/O error occurs
	 */
	public static void delTree(File f) throws IOException {
		delTree(f, true, null);
	}

	/** Delete directory tree. Deletes the
	 * directory tree rooted at <code>f</code>. The directory
	 * <code>f</code> itself is deleted if <code>delRoot</code> is
	 * <code>true</code>.
	 *
	 * @param f root of tree to delete
	 * @param delRoot set to <code>true</code> to delete root.
	 * @throws IOException if an I/O error occurs
	 */
	public static void delTree(File f, boolean delRoot) throws IOException {
		delTree(f, delRoot, null);
	}

	/** Delete directory tree. Deletes the
	 * directory tree rooted at <code>f</code>. The directory
	 * <code>f</code> itself is deleted if <code>delRoot</code> is
	 * <code>true</code>.
	 *
	 * @param f root of tree to delete
	 * @param fi filter that an entry must pass in order to be deleted
	 *           (<code>null</code> means all pass)
	 * @param delRoot set to <code>true</code> to delete root.
	 * @throws IOException if an I/O error occurs
	 */

	public static void delTree(File f, boolean delRoot, FilenameFilter fi)
			throws IOException {
		// First, delete children
		if (f.isDirectory()) {
			File[] entries = f.listFiles();
			for (int i = 0; i < entries.length; i++)
				delTree(entries[i], true, fi);
		}
		// Then delete this node
		if (delRoot
				&& (fi == null || fi.accept(f.getParentFile(), f.getName()))
				&& !f.delete())
			throw new IOException("Can't delete " + f);
	}


    
}

// arch-tag: 966f6ea4-cffc-4aca-8cff-451ee7a36ff3
