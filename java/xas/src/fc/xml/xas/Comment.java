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

package fc.xml.xas;

public class Comment extends Item {

    private String text;

    public Comment (String text) {
	super(COMMENT);
	Verifier.checkNotNull(text);
	this.text = text;
    }

    public String getText () {
	return text;
    }

    public boolean equals (Object o) {
	if (this == o) {
	    return true;
	} else if (!(o instanceof Comment)) {
	    return false;
	} else {
	    Comment c = (Comment) o;
	    return text.equals(c.text);
	}
    }

    public String toString () {
	return "C(" + text + ")";
    }

    @Override
    public int hashCode() {
	return text == null ? 0 : 37*text.hashCode();
    }

}

// arch-tag: daea83f8-cdc5-4063-9f1a-4e9f42fe8063
