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

package fc.xml.xas.security;

import java.io.IOException;

import fc.xml.xas.FragmentItem;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.MutableFragmentPointer;
import fc.xml.xas.StartTag;

class DsReference extends FragmentItem {

    public static final int REFERENCE = 0x2903;

    private DigestItem digest;
    private StartTag methodStart;
    private StartTag valueStart;

    public DsReference (MutableFragmentPointer pointer, int start, int length,
			StartTag context, String digestType) {
	super(REFERENCE, 1);
	firstItem = new StartTag(SecUtil.DS_REFERENCE_NAME, context);
	methodStart = new StartTag(SecUtil.DS_DIGEST_METHOD_NAME,
				   (StartTag) firstItem);
	valueStart = new StartTag(SecUtil.DS_DIGEST_VALUE_NAME,
				  (StartTag) firstItem);
	String uri = "#Dig-" + SecUtil.nextId();
	((StartTag) firstItem).addAttribute(SecUtil.URI_ATT_NAME, uri);
	digest = new DigestItem(uri.substring(1), digestType);
	digest.setContent(pointer, start, length);
	methodStart.addAttribute(SecUtil.ALGO_ATT_NAME, digestType);
    }

    public void appendTo (ItemTarget target) throws IOException {
	target.append(firstItem);
	target.append(methodStart);
	target.append(SecUtil.DS_DIGEST_METHOD_END);
	target.append(valueStart);
	target.append(digest);
	target.append(SecUtil.DS_DIGEST_VALUE_END);
	target.append(SecUtil.DS_REFERENCE_END);
    }

}

// arch-tag: ce72a7cf-2163-4d61-9d6b-5ffad6c5d996
