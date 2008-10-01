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

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import fc.util.log.Log;

/** 
 * @author Tancred Lindholm
 * @author Jaakko Kangasharju
 */
public class RaInputStream extends InputStream implements SeekableInputStream {
    // NOTE on built-in buffer
    // buffer content is always valid from [0,end[, where end=offset+left
    // currentFilePos is the file pos of the first byte after buffer end
    
    private RandomAccessFile file;

    private byte[] buffer = new byte[1<<Utf8Reader.MAP_BITS];
    private long currentFilePos = 0l; // File offset for next read
    private final int bufLen = buffer.length, streamModeThreshold=bufLen<<4;
    private int leftToStreamMode = 0; // Start in streaming mode
    private int offset = 0; // Current pos in buffer
    private int left = 0; // Left in buffer

    public RaInputStream (RandomAccessFile file) {
	this.file = file;
    }

    public RandomAccessFile swapFile(RandomAccessFile f2) throws IOException {
	RandomAccessFile of = file;
	f2.seek(file.getFilePointer());
	file=f2;
	return of;
    }
    public int read () throws IOException {
	if( left <= 0 ) {
	 left=file.read(buffer);
	 //Log.debug("1-byte read got "+left+" bytes, offset is "+offset);
	 if( left < 0 )
	     return left;
	 currentFilePos+=left;
	 offset =0;
	}
	// assert left > 0; // By contract of file.read
	left--;
	//Log.debug("1-byte read returning char "+Debug.toPrintable(buffer, offset, 1)+" at "+offset+" left is = "+left);
	return buffer[offset++] & 0xFF;
    }

    public int read (byte[] b) throws IOException {
	return read(b, 0, b.length);
    }

    public int read (byte[] b, int off, int len) throws IOException {
	if( left > 0 ) {
	    // First, return any bytes left in internal buffer
	    int toCopy = len > left ? left : len;
	    System.arraycopy(buffer,offset,b,off,toCopy);
	    /*Log.debug("buffered read got "+toCopy+" bytes, at file offset "+
	    		(currentFilePos-left)+", requested="+len,
	    		Debug.toPrintable(b,off,toCopy));*/
	    left -= toCopy;
	    offset += toCopy;
	    //Log.debug("After buffered read left="+left+", offset="+offset+", currentFilePos="+currentFilePos);
	    return toCopy;
	}
	int read = file.read(b, off, len);
	/*Log.debug("multibyte read got "+read+" bytes, at file offset "+
		currentFilePos+", requested="+len,
		Debug.toPrintable(b,off,read));*/
	if( read < 0 )
	    return read;
	currentFilePos +=read;
	if( leftToStreamMode > 0 ) {
	    //Log.debug("Saving buffer, left to streaming mode="+leftToStreamMode);
	    // Buffer read data if we are not in streaming mode
	    if( bufLen < read) {
		System.arraycopy(b, off+(read-bufLen), buffer, 0, bufLen);
		offset=bufLen;
	    } else {
		System.arraycopy(b, off, buffer, 0, read);
		offset=read;
	    }
	    left=0;
	    leftToStreamMode-=read;
	}
	return read;
    }

    public void close () throws IOException {
	file.close();
    }

    public void seek (long pos) throws IOException {
	if (Log.isEnabled(Log.TRACE)) {
	    Log.log("seek(" + pos + "), bOff=" + currentFilePos + " offset="
		    + offset + ", length=" + left, Log.TRACE);
	}
	if( pos == currentFilePos - left ) // Check quickly for NOP seek
	    return;
	// BUGFIX-20071015-2: Bad calculation of active buffer window
	leftToStreamMode = streamModeThreshold;
	long bufFirstPos=currentFilePos-left;
	// Is it in window of current buffer?
	if ( pos >= bufFirstPos && pos < currentFilePos  ) {
	    // Need to discard as many bytes as pos is past bufFirstPos
	    int discard = (int) (pos-bufFirstPos); 
	    offset +=discard;
	    left -= discard;
	    //Log.debug("In buffer seek to "+pos+" yields offset="+offset+", left="+left);
	} else { 
	    // Fully outside buffer
	    file.seek(pos);
	    left = 0;
	    offset = 0;
	    currentFilePos = pos;
	    //Log.debug("Seek outside buffer to "+pos);
	} 
    }

    public String toString () {
	try {
	    return "RaInputStream(" + (currentFilePos-left) + ", "
		+ file.length() + ")";
	} catch (IOException ex) {
	    return "RaInputStream()";
	}
    }

}

// arch-tag: 98b06e60-695f-43f9-9ebf-52ef8d5bc0f7
