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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Stack;

import fc.util.log.Log;

// BUGFIX-20060921-2: the getPrefix methods now throw an exception instead of
// failing silently by returning null

public class XmlOutput implements SerializerTarget {

    private Writer writer;
    private TargetOutputStream out;
    private String encoding;
    private Stack<StartTag> sts;
    private boolean insideDocument;
    private boolean _checkCtx = false;
    
    private void writeValue (String value) throws IOException {
	for (int i = 0; i < value.length(); i++) {
	    char c = value.charAt(i);
	    switch (c) {
	    case '&':
		writer.write("&amp;");
		break;
	    case '<':
		writer.write("&lt;");
		break;
	    case '"':
		writer.write("&quot;");
		break;
	    case 0x9:
		writer.write("&#x9;");
		break;
	    case 0xA:
		writer.write("&#xA;");
		break;
	    case 0xD:
		writer.write("&#xD;");
		break;
	    default:
		writer.write(c);
	    }
	}
    }

    private void writeText (String text) throws IOException {
	for (int i = 0; i < text.length(); i++) {
	    char c = text.charAt(i);
	    switch (c) {
	    case '&':
		writer.write("&amp;");
		break;
	    case '<':
		writer.write("&lt;");
		break;
	    case 0xD:
		writer.write("&#xD;");
		break;
	    default:
		writer.write(c);
	    }
	}
    }

    public XmlOutput (OutputStream out, String encoding) throws IOException {
	this.writer = new OutputStreamWriter(out, encoding);
	this.out = new TargetOutputStream(this, out);
	this.encoding = encoding;
	this.sts = new Stack<StartTag>();
	sts.push(null);
	this.insideDocument = false;
    }

    public OutputStream getOutputStream () {
	return out;
    }

    public String getEncoding () {
	return encoding;
    }

    public StartTag getContext () {
	return sts.peek();
    }

    public void append (Item item) throws IOException {
	if (Log.isEnabled(Log.TRACE)) {
	    Log.log("Item", Log.TRACE, item);
	    Log.log("Type", Log.TRACE, item.getType());
	}
	switch (item.getType()) {
	case Item.START_DOCUMENT:
	    if (!insideDocument) {
		writer.write("<?xml version=\"1.0\" encoding=\"");
		writer.write(encoding);
		writer.write("\"?>");
	    } else {
		throw new IOException(
		    "Attempt to write StartDocument inside document");
	    }
	    break;
	case Item.END_DOCUMENT:
	    writer.flush();
	    insideDocument = false;
	    return;
	case Item.START_TAG: {
	    StartTag st = (StartTag) item;
	    StartTag stCtx = st.getContext();
	    StartTag realCtx = getContext();
	    if ( _checkCtx && realCtx != null && stCtx != realCtx) {
		/*for( StartTag scanCtx = realCtx.getContext();scanCtx!=stCtx;
			scanCtx = scanCtx.getContext() ) {
		    if( scanCtx != null )
			continue;*/
		    throw new IllegalStateException
		    ("Context " + (stCtx == null ? null : stCtx.getName()) + 
			    " of tag " + st.getName()
			    + " does not match serialization context "
			    + realCtx.getName());
		//}
	    }
	    writer.write('<');
	    String prefix = st.getPrefix();
	    if (prefix.length() > 0) {
		writer.write(prefix);
		writer.write(':');
	    }
	    writer.write(st.getName().getName());
	    Iterator<PrefixNode> it = null;
	    if (stCtx != null && realCtx == null) {
		it = st.detachedPrefixes();
	    } else {
		it = st.localPrefixes();
	    }
	    while (it.hasNext()) {
		PrefixNode pn = it.next();
		writer.write(" xmlns");
		String pr = pn.getPrefix();
		if (pr.length() > 0) {
		    writer.write(':');
		    writer.write(pn.getPrefix());
		}
		writer.write("=\"");
		writer.write(pn.getNamespace());
		writer.write('"');
	    }
	    sts.push(st);
	    for (Iterator<AttributeNode> ait = st.attributes(); ait.hasNext();) {
		AttributeNode an = ait.next();
		writer.write(' ');
		String ans = an.getName().getNamespace();
		if (ans.length() > 0) {
		    String pr = st.getPrefix(an.getName().getNamespace());
		    if (pr.length() > 0) {
			writer.write(pr);
			writer.write(':');
		    }
		}
		writer.write(an.getName().getName());
		writer.write("=\"");
		Object value = an.getValue();
		if (value instanceof SerializableItem) {
		    writer.flush();
		    ((SerializableItem) value).serialize(XasUtil.XML_MIME_TYPE,
			this);
		} else if (value instanceof String) {
		    writeValue((String) value);
		} else {
		    throw new IOException("Value of attribute " + an
			+ " not serializable");
		}
		writer.write('"');
	    }
	    writer.write('>');
	    break;
	}
	case Item.END_TAG: {
	    EndTag et = (EndTag) item;
	    StartTag st = sts.pop();
	    writer.write("</");
	    if (st != null) {
		String pr = st.getPrefix(et.getName().getNamespace());
		if (pr.length() > 0) {
		    writer.write(pr);
		    writer.write(':');
		}
	    } else {
		throw new IOException("Superfluous end tag " + et.getName()
		    + " encountered");
	    }
	    writer.write(et.getName().getName());
	    writer.write('>');
	    break;
	}
	case Item.TEXT: {
	    Text t = (Text) item;
	    writeText(t.getData());
	    break;
	}
	case Item.PI: {
	    Pi p = (Pi) item;
	    writer.write("<?");
	    writer.write(p.getTarget());
	    String instruction = p.getInstruction();
	    if (instruction.length() > 0) {
		writer.write(' ');
		writer.write(instruction);
	    }
	    writer.write("?>");
	    break;
	}
	case Item.COMMENT: {
	    Comment c = (Comment) item;
	    writer.write("<!--");
	    writer.write(c.getText());
	    writer.write("-->");
	    break;
	}
	case Item.ENTITY_REF: {
	    EntityRef e = (EntityRef) item;
	    writer.write('&');
	    writer.write(e.getName());
	    writer.write(';');
	    break;
	}
	case Item.DOCTYPE: {
	    Doctype dtd = (Doctype) item;
	    writer.write("<!DOCTYPE ");
	    writer.write(dtd.getName());
	    writer.write(' ');
	    String publicId = dtd.getPublicId();
	    if (publicId != null) {
		writer.write("PUBLIC \"");
		writer.write(publicId);
		writer.write('"');
	    } else {
		writer.write("SYSTEM");
	    }
	    writer.write(' ');
	    dtd.outputSystemLiteral(writer);
	    writer.write('>');
	    break;
	}
	default:
	    if (item instanceof SerializableItem) {
		writer.flush();
		((SerializableItem) item)
		    .serialize(XasUtil.XML_MIME_TYPE, this);
	    } else if (item instanceof AppendableItem) {
		((AppendableItem) item).appendTo(this);
	    } else {
		throw new IOException("Unrecognized item type "
		    + item.getType());
	    }
	}
	out.wroteItem();
	insideDocument = true;
    }

    public void flush () throws IOException {
	writer.flush();
    }

    
    /** Temporarily here to allow badly-behaved apps to run.
     */
    public void FIXMEdisableContextCheck() {
	_checkCtx = false;
    }
}

// arch-tag: c20cae74-8709-4bb9-a62a-c0f913d360fb
