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
import java.util.concurrent.Semaphore;

import fc.util.log.Log;

/** An output stream that instantiates its consumer when needed. When the first
 * write
 * to the stream is encountered, the stream starts a data consumer thread
 * and calls {@link #stream stream()} from within this thread. The completion
 * of the consumer is synchronized on close, i.e. the <code>close()</code> method
 * waits until the consumer finishes. 
 * <p<Override the {@link #stream stream} method to implement your own
 * stream data consumer.
 * @author Tancred Lindholm
 */
public abstract class DelayedOutputStream extends OutputStream {
	
	private final Semaphore s = new Semaphore(0);
	
	// Used by close to wait for producer finishing up 
	protected OutputStream out = null;
	
	private boolean hasStream = false;
	
	private IOException delayedEx = null;
	
	protected boolean needStream(boolean needsData) throws IOException {
		
		out = new PipedOutputStream();
		final PipedInputStream in = new PipedInputStream(
				(PipedOutputStream) out);
		(new Thread() {
			public void run() {
				try {
					stream(in);
				} catch (IOException x) {
					Log.log("IOExcept in delayed consumer, bubbling up...",
							Log.ERROR, x);
					delayedEx = x;
				} finally {
					s.release();
					try {
						out.close();
					} catch (IOException x) {
						; /*Intentional*/
					}
				}
			}
		}).start();
		return true;
	}
	
	protected void streamDone() {
	}
	
	/** Consume stream data. Called when somebody attempts to write to this
	 * stream. The method is called in a separate consumer thread that is
	 * connected to this stream via piped streams. 
	 *
	 * @param in stream to read consumed data from.
	 * @throws IOException if an I/O error occurs
	 */
	protected abstract void stream(InputStream in) throws IOException;
	
	/** Close stream. Waits for the consumer to finish before returning.
	 */

	public void close() throws IOException {
		if (delayedEx != null)
			throw delayedEx;
		//      if( !hasStream ) hasStream = needStream(false);
		streamDone();
		if (out != null) {
			out.close();
			try {
				s.acquire(); // Only do this if there is a consumer
			} catch (InterruptedException x) {
			} //DELIB EMPTY
		}
	}
	
	// Trigger or not, that's the question... flush() is usually preceded by
	// writes, so I think it should trigger.
	
	public void flush() throws IOException {
		if (delayedEx != null)
			throw delayedEx;
		if (!hasStream)
			hasStream = needStream(false);
		out.flush();
	}
	
	public void write(byte[] b) throws IOException {
		if (delayedEx != null)
			throw delayedEx;
		if (!hasStream)
			hasStream = needStream(true);
		out.write(b);
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		if (delayedEx != null)
			throw delayedEx;
		if (!hasStream)
			hasStream = needStream(true);
		out.write(b, off, len);
	}
	
	public void write(int b) throws IOException {
		if (delayedEx != null)
			throw delayedEx;
		if (!hasStream)
			hasStream = needStream(true);
		out.write(b);
	}
	
}
//arch-tag: ce9884e1-dd62-4336-9f6d-1254d73b7a75
//
