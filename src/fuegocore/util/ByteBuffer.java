/*
 * ByteBuffer.java -- Growable strings Copyright (C) 1998, 1999, 2000, 2001, 2002 Free Software
 * Foundation, Inc. Copyright (C) 2006 Helsinki Institute for Information Technology
 * 
 * This file is a modifed version of StringBuffer, a part of GNU Classpath.
 * 
 * GNU Classpath is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2, or (at
 * your option) any later version.
 * 
 * GNU Classpath is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with GNU Classpath; see
 * the file COPYING. If not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite
 * 330, Boston, MA 02111-1307 USA.
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based
 * on this library. Thus, the terms and conditions of the GNU General Public License cover the whole
 * combination.
 * 
 * As a special exception, the copyright holders of this library give you permission to link this
 * library with independent modules to produce an executable, regardless of the license terms of
 * these independent modules, and to copy and distribute the resulting executable under terms of
 * your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived
 * from or based on this library. If you modify this library, you may extend this exception to your
 * version of the library, but you are not obligated to do so. If you do not wish to do so, delete
 * this exception statement from your version.
 */

/*
 * Modified to implement a ByteBuffer for the Fuego Core project by Jaakko Kangasharju.
 */
package fuegocore.util;

import java.io.Serializable;

/**
 * <code>ByteBuffer</code> represents a changeable array of <code>byte</code>s. It provides the
 * operations required to modify the <code>ByteBuffer</code>, including insert, replace, delete,
 * append, and reverse. It is thread-safe; meaning that all modifications to a buffer are in
 * synchronized methods.
 * <p>
 * <code>ByteBuffer</code>s are variable-length in nature, so even if you initialize them to a
 * certain size, they can still grow larger than that. <em>Capacity</em> indicates the number of
 * characters the <code>ByteBuffer</code> can have in it before it has to grow (growing the byte
 * array is an expensive operation involving <code>new</code>).
 * @author Paul Fisher
 * @author John Keiser
 * @author Tom Tromey
 * @author Eric Blake <ebb9@email.byu.edu>
 */
public final class ByteBuffer implements Serializable {

    /**
     * Compatible with JDK 1.0+. Not anymore, it isn't.
     */
    private static final long serialVersionUID = 3388685877147921108L;

    /**
     * Index of next available character (and thus the size of the current string contents). Note
     * that this has permissions set this way so that String can get the value.
     * @serial the number of characters in the buffer
     */
    int count;

    /**
     * The buffer. Note that this has permissions set this way so that String can get the value.
     * @serial the buffer
     */
    byte[] value;

    /**
     * True if the buffer is shared with another object (ByteBuffer or String); this means the
     * buffer must be copied before writing to it again. Note that this has permissions set this way
     * so that String can get the value.
     * @serial whether the buffer is shared
     */
    boolean shared;

    /**
     * The default capacity of a buffer.
     */
    private final static int DEFAULT_CAPACITY = 16;


    /**
     * Create a new ByteBuffer with default capacity 16.
     */
    public ByteBuffer() {
        this(DEFAULT_CAPACITY);
    }


    /**
     * Create an empty <code>ByteBuffer</code> with the specified initial capacity.
     * @param capacity
     *            the initial capacity
     * @throws NegativeArraySizeException
     *             if capacity is negative
     */
    public ByteBuffer(int capacity) {
        value = new byte[capacity];
    }


    /**
     * Get the length of the <code>String</code> this <code>ByteBuffer</code> would create. Not to
     * be confused with the <em>capacity</em> of the <code>ByteBuffer</code>.
     * @return the length of this <code>ByteBuffer</code>
     * @see #capacity()
     * @see #setLength(int)
     */
    public synchronized int length() {
        return count;
    }


    /**
     * Get the total number of characters this <code>ByteBuffer</code> can support before it must be
     * grown. Not to be confused with <em>length</em>.
     * @return the capacity of this <code>ByteBuffer</code>
     * @see #length()
     * @see #ensureCapacity(int)
     */
    public synchronized int capacity() {
        return value.length;
    }


    /**
     * Increase the capacity of this <code>ByteBuffer</code>. This will ensure that an expensive
     * growing operation will not occur until <code>minimumCapacity</code> is reached. The buffer is
     * grown to the larger of <code>minimumCapacity</code> and <code>capacity() * 2 + 2</code>, if
     * it is not already large enough.
     * @param minimumCapacity
     *            the new capacity
     * @see #capacity()
     */
    public synchronized void ensureCapacity(int minimumCapacity) {
        ensureCapacity_unsynchronized(minimumCapacity);
    }


    /**
     * Set the length of this ByteBuffer. If the new length is greater than the current length, all
     * the new characters are set to '\0'. If the new length is less than the current length, the
     * first <code>newLength</code> characters of the old array will be preserved, and the remaining
     * characters are truncated.
     * @param newLength
     *            the new length
     * @throws IndexOutOfBoundsException
     *             if the new length is negative (while unspecified, this is a
     *             StringIndexOutOfBoundsException)
     * @see #length()
     */
    public synchronized void setLength(int newLength) {
        if (newLength < 0) throw new StringIndexOutOfBoundsException(newLength);

        ensureCapacity_unsynchronized(newLength);
        while (count < newLength)
            value[count++] = '\0';
        count = newLength;
    }


    /**
     * Get the character at the specified index.
     * @param index
     *            the index of the character to get, starting at 0
     * @return the character at the specified index
     * @throws IndexOutOfBoundsException
     *             if index is negative or &gt;= length() (while unspecified, this is a
     *             StringIndexOutOfBoundsException)
     */
    public synchronized byte charAt(int index) {
        if (index < 0 || index >= count) throw new StringIndexOutOfBoundsException(index);
        return value[index];
    }


    /**
     * Get the specified array of characters. <code>srcOffset - srcEnd</code> characters will be
     * copied into the array you pass in.
     * @param srcOffset
     *            the index to start copying from (inclusive)
     * @param srcEnd
     *            the index to stop copying from (exclusive)
     * @param dst
     *            the array to copy into
     * @param dstOffset
     *            the index to start copying into
     * @throws NullPointerException
     *             if dst is null
     * @throws IndexOutOfBoundsException
     *             if any source or target indices are out of range (while unspecified, source
     *             problems cause a StringIndexOutOfBoundsException, and dest problems cause an
     *             ArrayIndexOutOfBoundsException)
     */
    public synchronized void getChars(int srcOffset, int srcEnd, byte[] dst, int dstOffset) {
        int todo = srcEnd - srcOffset;
        if (srcOffset < 0 || srcEnd > count || todo < 0)
            throw new StringIndexOutOfBoundsException();
        System.arraycopy(value, srcOffset, dst, dstOffset, todo);
    }


    /**
     * Set the character at the specified index.
     * @param index
     *            the index of the character to set starting at 0
     * @param ch
     *            the value to set that character to
     * @throws IndexOutOfBoundsException
     *             if index is negative or &gt;= length() (while unspecified, this is a
     *             StringIndexOutOfBoundsException)
     */
    public synchronized void setCharAt(int index, byte ch) {
        if (index < 0 || index >= count) throw new StringIndexOutOfBoundsException(index);
        // Call ensureCapacity to enforce copy-on-write.
        ensureCapacity_unsynchronized(count);
        value[index] = ch;
    }


    /**
     * Append the <code>ByteBuffer</code> value of the argument to this <code>ByteBuffer</code>.
     * @param stringBuffer
     *            the <code>ByteBuffer</code> to convert and append
     * @return this <code>ByteBuffer</code>
     * @since 1.4
     */
    public synchronized ByteBuffer append(ByteBuffer stringBuffer) {
        if (stringBuffer != null) {
            synchronized (stringBuffer) {
                int len = stringBuffer.count;
                ensureCapacity_unsynchronized(count + len);
                System.arraycopy(stringBuffer.value, 0, value, count, len);
                count += len;
            }
        }
        return this;
    }


    /**
     * Append the <code>byte</code> array to this <code>ByteBuffer</code>. This is similar (but more
     * efficient) than <code>append(new String(data))</code>, except in the case of null.
     * @param data
     *            the <code>byte[]</code> to append
     * @return this <code>ByteBuffer</code>
     * @throws NullPointerException
     *             if <code>str</code> is <code>null</code>
     * @see #append(byte[], int, int)
     */
    public ByteBuffer append(byte[] data) {
        return append(data, 0, data.length);
    }


    /**
     * Append part of the <code>byte</code> array to this <code>ByteBuffer</code>. This is similar
     * (but more efficient) than <code>append(new String(data, offset, count))</code>, except in the
     * case of null.
     * @param data
     *            the <code>byte[]</code> to append
     * @param offset
     *            the start location in <code>str</code>
     * @param count
     *            the number of characters to get from <code>str</code>
     * @return this <code>ByteBuffer</code>
     * @throws NullPointerException
     *             if <code>str</code> is <code>null</code>
     * @throws IndexOutOfBoundsException
     *             if offset or count is out of range (while unspecified, this is a
     *             StringIndexOutOfBoundsException)
     */
    public synchronized ByteBuffer append(byte[] data, int offset, int count) {
        ensureCapacity_unsynchronized(this.count + count);
        System.arraycopy(data, offset, value, this.count, count);
        this.count += count;
        return this;
    }


    /**
     * Append the <code>byte</code> to this <code>ByteBuffer</code>.
     * @param ch
     *            the <code>byte</code> to append
     * @return this <code>ByteBuffer</code>
     */
    public synchronized ByteBuffer append(byte ch) {
        ensureCapacity_unsynchronized(count + 1);
        value[count++] = ch;
        return this;
    }


    /**
     * Delete characters from this <code>ByteBuffer</code>. <code>delete(10, 12)</code> will delete
     * 10 and 11, but not 12. It is harmless for end to be larger than length().
     * @param start
     *            the first character to delete
     * @param end
     *            the index after the last character to delete
     * @return this <code>ByteBuffer</code>
     * @throws StringIndexOutOfBoundsException
     *             if start or end are out of bounds
     * @since 1.2
     */
    public synchronized ByteBuffer delete(int start, int end) {
        if (start < 0 || start > count || start > end)
            throw new StringIndexOutOfBoundsException(start);
        if (end > count) end = count;
        // This will unshare if required.
        ensureCapacity_unsynchronized(count);
        if (count - end != 0) System.arraycopy(value, end, value, start, count - end);
        count -= end - start;
        return this;
    }


    /**
     * Delete a character from this <code>ByteBuffer</code>.
     * @param index
     *            the index of the character to delete
     * @return this <code>ByteBuffer</code>
     * @throws StringIndexOutOfBoundsException
     *             if index is out of bounds
     * @since 1.2
     */
    public ByteBuffer deleteCharAt(int index) {
        return delete(index, index + 1);
    }


    /**
     * Creates a substring of this ByteBuffer, starting at a specified index and ending at the end
     * of this ByteBuffer.
     * @param beginIndex
     *            index to start substring (base 0)
     * @return new String which is a substring of this ByteBuffer
     * @throws StringIndexOutOfBoundsException
     *             if beginIndex is out of bounds
     * @see #substring(int, int)
     * @since 1.2
     */
    public String substring(int beginIndex) {
        return substring(beginIndex, count);
    }


    /**
     * Creates a substring of this ByteBuffer, starting at a specified index and ending at one
     * character before a specified index. This is implemented the same as
     * <code>substring(beginIndex, endIndex)</code>, to satisfy the CharSequence interface.
     * @param beginIndex
     *            index to start at (inclusive, base 0)
     * @param endIndex
     *            index to end at (exclusive)
     * @return new String which is a substring of this ByteBuffer
     * @throws IndexOutOfBoundsException
     *             if beginIndex or endIndex is out of bounds
     * @see #substring(int, int)
     * @since 1.4
     */
    public CharSequence subSequence(int beginIndex, int endIndex) {
        return substring(beginIndex, endIndex);
    }


    /**
     * Creates a substring of this ByteBuffer, starting at a specified index and ending at one
     * character before a specified index.
     * @param beginIndex
     *            index to start at (inclusive, base 0)
     * @param endIndex
     *            index to end at (exclusive)
     * @return new String which is a substring of this ByteBuffer
     * @throws StringIndexOutOfBoundsException
     *             if beginIndex or endIndex is out of bounds
     * @since 1.2
     */
    public synchronized String substring(int beginIndex, int endIndex) {
        int len = endIndex - beginIndex;
        if (beginIndex < 0 || endIndex > count || len < 0)
            throw new StringIndexOutOfBoundsException();
        if (len == 0) return "";
        // Share the byte[] unless 3/4 empty.
        shared = (len << 2) >= value.length;
        // Package constructor avoids an array copy.
        return new String(value, beginIndex, len);
    }


    /**
     * Insert a subarray of the <code>byte[]</code> argument into this <code>ByteBuffer</code>.
     * @param offset
     *            the place to insert in this buffer
     * @param str
     *            the <code>byte[]</code> to insert
     * @param str_offset
     *            the index in <code>str</code> to start inserting from
     * @param len
     *            the number of characters to insert
     * @return this <code>ByteBuffer</code>
     * @throws NullPointerException
     *             if <code>str</code> is <code>null</code>
     * @throws StringIndexOutOfBoundsException
     *             if any index is out of bounds
     * @since 1.2
     */
    public synchronized ByteBuffer insert(int offset, byte[] str, int str_offset, int len) {
        if (offset < 0 || offset > count || len < 0 || str_offset < 0 ||
            str_offset + len > str.length) throw new StringIndexOutOfBoundsException();
        ensureCapacity_unsynchronized(count + len);
        System.arraycopy(value, offset, value, offset + len, count - offset);
        System.arraycopy(str, str_offset, value, offset, len);
        count += len;
        return this;
    }


    /**
     * Insert the <code>byte[]</code> argument into this <code>ByteBuffer</code> .
     * @param offset
     *            the place to insert in this buffer
     * @param data
     *            the <code>byte[]</code> to insert
     * @return this <code>ByteBuffer</code>
     * @throws NullPointerException
     *             if <code>data</code> is <code>null</code>
     * @throws StringIndexOutOfBoundsException
     *             if offset is out of bounds
     * @see #insert(int, byte[], int, int)
     */
    public ByteBuffer insert(int offset, byte[] data) {
        return insert(offset, data, 0, data.length);
    }


    /**
     * Insert the <code>byte</code> argument into this <code>ByteBuffer</code>.
     * @param offset
     *            the place to insert in this buffer
     * @param ch
     *            the <code>byte</code> to insert
     * @return this <code>ByteBuffer</code>
     * @throws StringIndexOutOfBoundsException
     *             if offset is out of bounds
     */
    public synchronized ByteBuffer insert(int offset, byte ch) {
        if (offset < 0 || offset > count) throw new StringIndexOutOfBoundsException(offset);
        ensureCapacity_unsynchronized(count + 1);
        System.arraycopy(value, offset, value, offset + 1, count - offset);
        value[offset] = ch;
        count++;
        return this;
    }


    /**
     * Reverse the characters in this ByteBuffer. The same sequence of characters exists, but in the
     * reverse index ordering.
     * @return this <code>ByteBuffer</code>
     */
    public synchronized ByteBuffer reverse() {
        // Call ensureCapacity to enforce copy-on-write.
        ensureCapacity_unsynchronized(count);
        for (int i = count >> 1, j = count - i; --i >= 0; ++j) {
            byte c = value[i];
            value[i] = value[j];
            value[j] = c;
        }
        return this;
    }


    /**
     * Convert this <code>ByteBuffer</code> to a <code>String</code>. The String is composed of the
     * characters currently in this ByteBuffer. Note that the result is a copy, and that future
     * modifications to this buffer do not affect the String.
     * @return the characters in this ByteBuffer
     */
    public String toString() {
        // The string will set this.shared = true.
        return new String(this.value, 0, count);
    }


    /**
     * Get the byte array behind this <code>ByteBuffer</code>. The returned array is still owned by
     * the buffer and must not be modified.
     * @return the bytes in this ByteBuffer
     */
    public byte[] getBytes() {
        byte[] result = new byte[count];
        System.arraycopy(value, 0, result, 0, count);
        return result;
    }


    /**
     * An unsynchronized version of ensureCapacity, used internally to avoid the cost of a second
     * lock on the same object. This also has the side effect of duplicating the array, if it was
     * shared (to form copy-on-write semantics).
     * @param minimumCapacity
     *            the minimum capacity
     * @see #ensureCapacity(int)
     */
    private void ensureCapacity_unsynchronized(int minimumCapacity) {
        if (shared || minimumCapacity > value.length) {
            // We don't want to make a larger vector when `shared' is
            // set. If we do, then setLength becomes very inefficient
            // when repeatedly reusing a ByteBuffer in a loop.
            int max = (minimumCapacity > value.length ? value.length * 2 + 2 : value.length);
            minimumCapacity = (minimumCapacity < max ? max : minimumCapacity);
            byte[] nb = new byte[minimumCapacity];
            System.arraycopy(value, 0, nb, 0, count);
            value = nb;
            shared = false;
        }
    }
}
