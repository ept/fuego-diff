/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas.index;

import fc.util.Util;
import fc.util.log.Log;
import fc.xml.xas.FragmentPointer;
import fc.xml.xas.StartTag;
import fc.xml.xas.Verifier;

public class VersionNode {

    private static enum Kind {
	INSERT, DELETE, MOVE
    };

    private Kind kind;
    private DeweyKey source;
    private FragmentPointer sourcePointer;
    private Index.Entry sourceEntry;
    private DeweyKey target;
    private FragmentPointer targetPointer;
    private Index.Entry targetEntry;
    private boolean isAfter;
    private VersionNode next;

    private VersionNode make (Kind kind, DeweyKey source,
	    FragmentPointer sourcePointer, DeweyKey target,
	    FragmentPointer targetPointer, boolean isAfter) {
	Verifier.checkNull(this.kind);
	if (Log.isEnabled(Log.TRACE)) {
	    Log.log("make(" + kind + "," + source + "," + target + ")",
		Log.TRACE);
	}
	this.kind = kind;
	this.source = source;
	this.sourcePointer = copy(sourcePointer);
	this.target = target;
	this.targetPointer = copy(targetPointer);
	this.isAfter = isAfter;
	this.next = new VersionNode();
	return next;
    }

    private FragmentPointer copy (FragmentPointer pointer) {
	if (pointer != null) {
	    return pointer.copy();
	} else {
	    return null;
	}
    }

    public boolean isSentinel () {
	return next == null;
    }

    public VersionNode getNext () {
	return next;
    }

    public DeweyKey update (DeweyKey key) {
	if (key == null) {
	    return null;
	}
	DeweyKey result = key;
	switch (kind) {
	case INSERT:
	    if (key.descendantFollowSibling(source)) {
		result = key.next(key.commonAncestor(source));
	    }
	    break;
	case DELETE:
	    if (key.descendantFollowSibling(source)) {
		result = key.prev(key.commonAncestor(source));
	    } else if (key.isDescendantSelf(source)) {
		result = null;
	    }
	    break;
	case MOVE:
	    if (Util.equals(result, source)) {
		if (target.followSibling(source) || !isAfter) {
		    result = target;
		} else {
		    result = target.next();
		}
	    } else if (result.isDescendant(source)) {
		if (target.followSibling(source) || !isAfter) {
		    result = result.replaceAncestor(source, target);
		} else {
		    result = result.replaceAncestor(source, target.next());
		}
	    } else {
		if (result.descendantFollowSibling(source)) {
		    result = result.prev(result.commonAncestor(source));
		}
		if (result.descendantFollowSibling(target)) {
		    result = result.next(result.commonAncestor(target));
		}
	    }
	    break;
	}
	return result;
    }

    FragmentPointer update (FragmentPointer pointer) {
	Verifier.checkNotNull(pointer);
	FragmentPointer result = pointer;
	switch (kind) {
	case INSERT:
	    if (pointer.behind(sourcePointer)) {
		result = pointer.translate(1);
	    }
	    break;
	case DELETE:
	    if (pointer.behind(sourcePointer)) {
		result = pointer.translate(-1);
	    }
	    break;
	case MOVE:
	    if (pointer.inside(sourcePointer)) {
		result = pointer.translate(sourcePointer, targetPointer);
	    } else if (pointer.behind(sourcePointer)
		    && !pointer.behind(targetPointer)) {
		result = pointer.translate(-1);
	    } else if (pointer.behind(targetPointer)
		    && !pointer.behind(sourcePointer)) {
		result = pointer.translate(1);
	    }
	    break;
	}
	return result;
    }

    private boolean between (int offset1, int offset2, int length2, int length1) {
	return offset1 <= offset2 && offset2 + length2 <= offset1 + length1;
    }

    public Index.Entry update (Index.Entry entry) {
	if (entry == null) {
	    return null;
	}
	Index.Entry result = entry;
	int offset = entry.getOffset();
	int length = entry.getLength();
	StartTag context = entry.getContext();
	int newOffset = offset;
	int newLength = length;
	StartTag newContext = context;
	switch (kind) {
	case INSERT:
	    if (offset >= sourceEntry.getEnd()) {
		newOffset += targetEntry.getLength();
	    } else if (between(offset, sourceEntry.getOffset(), sourceEntry
		.getLength(), length)) {
		newLength += targetEntry.getLength();
	    }
	    break;
	case DELETE:
	    if (offset >= sourceEntry.getEnd()) {
		newOffset -= sourceEntry.getLength();
	    } else if (between(offset, sourceEntry.getOffset(), sourceEntry
		.getLength(), length)) {
		newLength -= sourceEntry.getLength();
	    }
	    break;
	case MOVE:
	    if (targetEntry.getOffset() <= offset
		    && offset < sourceEntry.getOffset()) {
		newOffset += sourceEntry.getLength();
	    } else if (sourceEntry.getEnd() <= offset
		    && offset < targetEntry.getOffset()) {
		newOffset -= sourceEntry.getLength();
	    } else if (offset == sourceEntry.getOffset()) {
		newOffset = targetEntry.getOffset();
		newContext = targetEntry.getContext();
	    } else if (between(offset, sourceEntry.getOffset(), sourceEntry
		.getLength(), length)
		    && !between(offset, targetEntry.getOffset(), targetEntry
			.getLength(), length)) {
		newLength -= sourceEntry.getLength();
	    } else if (between(offset, targetEntry.getOffset(), targetEntry
		.getLength(), length)
		    && !between(offset, sourceEntry.getOffset(), sourceEntry
			.getLength(), length)) {
		newLength += sourceEntry.getLength();
	    } else if (between(sourceEntry.getOffset(), offset, length,
		sourceEntry.getLength())) {
		newOffset += targetEntry.getOffset() - sourceEntry.getOffset();
	    }
	    break;
	}
	if (offset != newOffset || length != newLength || context != newContext) {
	    result = new Index.Entry(newOffset, newLength, newContext);
	}
	return result;
    }

    public VersionNode insertAfter (DeweyKey key, FragmentPointer pointer) {
	Verifier.checkNotNull(key);
	Verifier.checkNotNull(pointer);
	return make(Kind.INSERT, key, pointer, null, null, true);
    }

    public VersionNode insertAt (DeweyKey key, FragmentPointer pointer) {
	Verifier.checkNotNull(key);
	Verifier.checkNotNull(pointer);
	return make(Kind.INSERT, key, pointer, null, null, false);
    }

    public VersionNode delete (DeweyKey key, FragmentPointer pointer) {
	Verifier.checkNotNull(key);
	Verifier.checkNotNull(pointer);
	return make(Kind.DELETE, key, pointer, null, null, true);
    }

    public VersionNode moveAfter (DeweyKey source,
	    FragmentPointer sourcePointer, DeweyKey target,
	    FragmentPointer targetPointer) {
	Verifier.checkNotNull(source);
	Verifier.checkNotNull(sourcePointer);
	Verifier.checkNotNull(target);
	Verifier.checkNotNull(targetPointer);
	return make(Kind.MOVE, source, sourcePointer, target, targetPointer,
	    true);
    }

    public VersionNode moveTo (DeweyKey source, FragmentPointer sourcePointer,
	    DeweyKey target, FragmentPointer targetPointer) {
	Verifier.checkNotNull(source);
	Verifier.checkNotNull(sourcePointer);
	Verifier.checkNotNull(target);
	Verifier.checkNotNull(targetPointer);
	return make(Kind.MOVE, source, sourcePointer, target, targetPointer,
	    false);
    }

    public String toString () {
	return "Ver(" + kind + "," + source
		+ (target != null ? "," + target : "") + "," + isAfter + ")";
    }

}

// arch-tag: 1fd67d0c-1c17-455a-9eb3-4fa3e4ddb638
