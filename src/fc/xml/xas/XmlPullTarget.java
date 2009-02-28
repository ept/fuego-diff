/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Iterator;

import org.xmlpull.v1.XmlSerializer;

import fc.util.Stack;
import fc.util.log.Log;
import fc.util.log.LogLevels;
import fc.xml.xas.typing.ParsedPrimitive;

public class XmlPullTarget implements SerializerTarget {

    private XmlSerializer ser;
    private String type;
    private TargetOutputStream out;
    private String encoding;
    private Stack<StartTag> sts;


    protected XmlPullTarget(XmlSerializer ser) {
        this.ser = ser;
        sts = new Stack<StartTag>();
        sts.push(null);
    }


    public XmlPullTarget(XmlSerializer ser, String type, OutputStream out, String encoding)
            throws IOException {
        this.ser = ser;
        this.type = type;
        this.out = new TargetOutputStream(this, out);
        this.encoding = encoding;
        ser.setOutput(out, encoding);
        sts = new Stack<StartTag>();
        sts.push(null);
    }


    public XmlSerializer getSerializer() {
        return ser;
    }


    public OutputStream getOutputStream() {
        // XXX Kludge, XasBridgeTarget should not be XmlPullTarget
        if (out != null) {
            return out;
        } else {
            throw new IllegalStateException("XmlPullTarget constructed without OutputStream");
        }
    }


    public String getEncoding() {
        return encoding;
    }


    public StartTag getContext() {
        if (!sts.isEmpty()) {
            return sts.peek();
        } else {
            return null;
        }
    }


    public void append(Item item) throws IOException {
        if (Log.isEnabled(LogLevels.TRACE)) {
            Log.trace("Item", item);
        }
        switch (item.getType()) {
            case Item.START_DOCUMENT:
                ser.startDocument(encoding, null);
                break;
            case Item.END_DOCUMENT:
                ser.endDocument();
                sts.pop();
                break;
            case Item.START_TAG: {
                StartTag st = (StartTag) item;
                Iterator<PrefixNode> it = null;
                if (st.getContext() != null && sts.peek() == null) {
                    it = st.detachedPrefixes();
                } else {
                    it = st.localPrefixes();
                }
                sts.push(st);
                while (it.hasNext()) {
                    PrefixNode pn = it.next();
                    ser.setPrefix(pn.getPrefix(), pn.getNamespace());
                }
                Qname qn = st.getName();
                ser.startTag(qn.getNamespace(), qn.getName());
                for (Iterator<AttributeNode> ait = st.attributes(); ait.hasNext();) {
                    AttributeNode an = ait.next();
                    Qname aqn = an.getName();
                    Object value = an.getValue();
                    String stringValue = null;
                    if (value instanceof ParsedPrimitive) {
                        ParsedPrimitive pp = (ParsedPrimitive) value;
                        if (pp.getTypeName().equals(XasUtil.QNAME_TYPE)) {
                            Qname vqn = (Qname) pp.getValue();
                            String prefix = st.getPrefix(vqn.getNamespace());
                            stringValue = prefix + ":" + vqn.getName();
                        }
                    } else if (value instanceof String) {
                        stringValue = (String) value;
                    }
                    if (stringValue == null) { throw new IOException("Value of attribute " + an +
                                                                     " not serializable"); }
                    ser.attribute(aqn.getNamespace(), aqn.getName(), stringValue);
                }
                break;
            }
            case Item.END_TAG: {
                EndTag et = (EndTag) item;
                Qname qn = et.getName();
                ser.endTag(qn.getNamespace(), qn.getName());
                sts.pop();
                break;
            }
            case Item.TEXT: {
                Text t = (Text) item;
                ser.text(t.getData());
                break;
            }
            case Item.PI: {
                Pi p = (Pi) item;
                StringBuilder sb = new StringBuilder(p.getTarget());
                String instruction = p.getInstruction();
                if (instruction.length() > 0) {
                    sb.append(' ');
                    sb.append(instruction);
                }
                ser.processingInstruction(sb.toString());
                break;
            }
            case Item.COMMENT: {
                Comment c = (Comment) item;
                ser.comment(c.getText());
                break;
            }
            case Item.ENTITY_REF: {
                EntityRef e = (EntityRef) item;
                ser.entityRef(e.getName());
                break;
            }
            case Item.DOCTYPE: {
                Doctype dtd = (Doctype) item;
                StringBuilder sb = new StringBuilder(" ");
                sb.append(dtd.getName());
                sb.append(' ');
                if (dtd.getPublicId() != null) {
                    sb.append("PUBLIC \"");
                    sb.append(dtd.getPublicId());
                    sb.append("\" ");
                } else {
                    sb.append("SYSTEM ");
                }
                StringWriter wr = new StringWriter();
                dtd.outputSystemLiteral(wr);
                sb.append(wr.toString());
                ser.docdecl(sb.toString());
                break;
            }
            default:
                if (item instanceof SerializableItem) {
                    ser.flush();
                    ((SerializableItem) item).serialize(type, this);
                } else if (item instanceof AppendableItem) {
                    ((AppendableItem) item).appendTo(this);
                } else {
                    throw new IOException("Unrecognized item type: " + item.getType());
                }
        }
        if (out != null) {
            out.wroteItem();
        }
    }


    public void flush() throws IOException {
        ser.flush();
    }

}

// arch-tag: ba7e7538-d312-47b2-a44f-14f3a38fd8aa
