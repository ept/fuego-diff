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

package fc.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import fc.util.log.Log;

/** An input stream that produces its data when needed. When the first read
 * of the stream is encountered, the stream starts a data producer thread
 * and calls {@link #stream stream()} from within this thread.
 * <p<Override the {@link #stream stream} method to implement your own
 * stream data producer.
 *  @author Tancred Lindholm
 */

public abstract class DelayedInputStream extends InputStream {
	
	protected InputStream in = null;
	private boolean hasStream = false;
	private IOException delayedEx = null;
	
	protected boolean needStream(boolean needsData) throws IOException {
		final PipedOutputStream out = new PipedOutputStream();
		in = new PipedInputStream(out);
		(new Thread() {
			public void run() {
				try {
					stream(out);
				} catch (IOException x) {
					Log.log("IOExcept in delayed producer, bubbling up...",
					    Log.ERROR, x);
					delayedEx = x;
				} finally {
					try {
						out.close();
					} catch (IOException x) {
						;
						/*Intentional*/
					}
				}
			}
		}).start();
		return true;
		
	}
	
	/** Produce stream data. Called when somebody attempts to read from this
	 * stream. The method is called in a separate producer thread that is
	 * connected to this stream via piped streams.
	 *
	 * @param out stream to write produced data to.
	 * @throws IOException if an I/O error occurs
	 */
	protected abstract void stream(OutputStream out) throws IOException;
	
	/** Called when the stream data is no longer needed. */
	protected void streamDone() {}
	
	public int available() throws IOException {
		checkException();
		if (!hasStream)
			hasStream = needStream(false);
		return in.available();
	}
	
	// This operation won't trigger creation!
	public void close() throws IOException {
		checkException();
		streamDone();
		if (in != null)
			in.close();
	}
	
	public synchronized void mark(int readlimit) {
		try {
			if (!hasStream)
				hasStream = needStream(false);
		} catch (IOException x) {
			delayedEx = x;
			return;
		}
		in.mark(readlimit);
	}
	
	public boolean markSupported() {
		try {
			if (!hasStream)
				hasStream = needStream(false);
		} catch (IOException x) {
			delayedEx = x;
			return false;
		}
		return in.markSupported();
	}
	
	public int read() throws IOException {
		checkException();
		if (!hasStream)
			hasStream = needStream(true);
		return in.read();
	}
	
	public int read(byte[] b) throws IOException {
		checkException();
		if (!hasStream)
			hasStream = needStream(true);
		return in.read(b);
	}
	
	public int read(byte[] b, int off, int len) throws IOException {
		checkException();
		if (!hasStream)
			hasStream = needStream(true);
		return in.read(b, off, len);
	}
	
	public synchronized void reset() throws IOException {
		checkException();
		if (!hasStream)
			hasStream = needStream(false);
		in.reset();
	}
	
	public long skip(long n) throws IOException {
		checkException();
		if (!hasStream)
			hasStream = needStream(false);
		return in.skip(n);
	}
	
	final private void checkException() throws IOException {
		if (delayedEx != null) {
			IOException ex = delayedEx;
			delayedEx = null;
			throw ex;
		}
	}
}

// arch-tag: 7d7af2c0-ff04-4d63-8d62-87bd9171c70c
