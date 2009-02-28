/*
 * Copyright 2006 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.xas;

import java.util.Hashtable;

import fuegocore.util.Util;

/**
 * An abstract class for decoding data from an event sequence.  This
 * class will be invoked in the decoding process to convert string
 * representations into events of type {@link Event#TYPED_CONTENT}.
 * There is support for decoding datatypes recursively to permit
 * construction of complex types from simpler ones.
 *
 * <p>This class needs to be extended by any application inputting
 * structured data using the XML Schema datatypes.  The {@link
 * #expect} method is used when decoding complex structures; it will
 * call the {@link #decode} method for the simpler structures that it
 * finds.
 *
 * <p>An application wishing to decode typed content will call the
 * public {@link #decode} method, which will parse any simple
 * components of the decodable structure itself and call {@link
 * #expect} for any complex types.  The stream containing the encoded
 * structure is passed along on these calls, and needs to be left in a
 * sensible state.
 */
public abstract class ContentDecoder {

    private Hashtable prefixMapping;
    private ContentDecoder root;

    protected ContentDecoder () {
	this(new Hashtable());
    }

    protected ContentDecoder (Hashtable prefixMapping) {
	this.prefixMapping = prefixMapping;
	root = this;
    }

    /**
     * Decode an encoded structure from the given reader.  This method
     * needs to be implemented for special-purpose encoding.  The
     * encoded value of the given type (presumably done by a {@link
     * ContentEncoder}) is in the given reader object at its current
     * position.
     *
     * <p>A requirement for this method is that if it returns
     * <code>null</code>, it leaves the current position of the
     * supplied reader the same as it was when called (the {@link
     * XmlReader#getCurrentPosition} and {@link
     * XmlReader#setCurrentPosition} methods are useful here).
     * Otherwise the position must be right after the last event
     * forming the decoded structure.
     *
     * @param typeNs the namespace of the type of the structure to be
     * decoded
     * @param typeName the name of the type of the structure to be
     * decoded
     * @param reader the reader containing the encoded value
     * @param attributes the attributes of the element that contains
     * this structure, may be <code>null</code>
     * @return the decoded structure
     */
    public abstract Object decode (String typeNs, String typeName,
				   XmlReader reader, EventList attributes);

    /**
     * Decode an encoded structure with the given element name.  This
     * method handles mutual recursion in decoding to permit decoding
     * of complex structures.  If an element of the given name is
     * found starting in the current sequence at the current offset,
     * its attributes will be scanned for a type name and {@link
     * #decode} called for the content of the element.
     *
     * <p>If this method does not find a decodable structure with the
     * given name, it will return <code>null</code> and leave the
     * supplied reader at the same position that it was before this
     * method was called.  This permits e.g. detection of missing
     * optional components and open-ended lists of components.  If a
     * structure was decoded, the position will be right after the
     * element end event of the decoded element.
     *
     * @param elementNs the namespace of the element that is starting
     * @param elementName the name of the element that is starting
     * @param reader the reader containing the encoded value
     * @return the decoded structure, or <code>null</code> if the
     * specified structure could not be decoded
     */
    protected Object expect (String elementNs, String elementName,
			     XmlReader reader) {
	//System.out.println("expect called with");
	//System.out.println("elementNs=" + elementNs);
	//System.out.println("elementName=" + elementName);
	Object result = null;
	int pos = reader.getCurrentPosition();
	Event ev = reader.advance();
	//System.out.println(ev.toString());
	if (ev != null && ev.getType() == Event.START_ELEMENT) {
	    if (Util.equals(ev.getNamespace(), elementNs)
		&& Util.equals(ev.getName(), elementName)) {
		ev = reader.advance();
		//System.out.println(ev.toString());
		String typeNs = null, typeName = null;
		EventList attributes = null;
		while (ev != null && ev.getType() == Event.ATTRIBUTE) {
		    if (Util.equals(ev.getNamespace(), XasUtil.XSI_NAMESPACE)
			&& Util.equals(ev.getName(), "type")) {
			String type = (String) ev.getValue();
			int i = type.indexOf(':');
			String typePrefix = type.substring(0, i);
			typeName = type.substring(i + 1);
			typeNs = mapNamespace(typePrefix);
			//System.out.println("xsi:type={" + typeNs + "}"
			//+ typeName);
		    } else {
			if (attributes == null) {
			    attributes = new EventList();
			}
			attributes.add(ev);
		    }
		    ev = reader.advance();
		    //System.out.println(ev.toString());
		}
		if (ev != null && ev.getType() == Event.TYPED_CONTENT) {
		    result = ev.getValue();
		} else {
		    reader.backup();
		    if (typeNs != null && typeName != null) {
			result = root.decode(typeNs, typeName, reader,
					     attributes);
		    }
		}
		reader.advance();
	    }
	}
	if (result == null) {
	    reader.setCurrentPosition(pos);
	}
	return result;
    }

    /**
     * Remember a namespace prefix mapping.  The type of element
     * content is given as an attribute, the value of which typically
     * contains a qualified name, which consists of a <prefix,local
     * name> pair, instead of a namespace URI, so determining the type
     * will require information on prefix mappings.  The object
     * calling on this <code>ContentDecoder</code> is expected to pass
     * encountered prefix mappings using this method.
     *
     * @param namespace the URI of the namespace
     * @param prefix the new prefix given to <code>namespace</code>
     */
    public void insertPrefixMapping (String namespace, String prefix) {
	//System.out.println(this.toString());
	//System.out.println("Mapping: " + namespace + "=" + prefix);
	prefixMapping.put(prefix, namespace);
    }

    /**
     * Forget a namespace prefix mapping.  Namespace prefix mappings
     * are only valid for the element they appear in, so this method
     * needs to be called whenever a prefix mapping goes out of scope.
     *
     * @param prefix the namespace prefix that went out of scope
     */
    public void deletePrefixMapping (String prefix) {
	prefixMapping.remove(prefix);
    }

    /**
     * Look up a namespace corresponding to a prefix.
     *
     * @param prefix the namespace prefix that needs to be mapped to
     * @return the namespace that <code>prefix</code> maps to, or
     * <code>null</code> if no applicable mapping currently exists
     */
    public String mapNamespace (String prefix) {
	//System.out.println(this.toString());
	//System.out.println("Getting namespace for " + prefix);
	return (String) prefixMapping.get(prefix);
    }

    /**
     * Set the root content decoder.  Typically a {@link
     * ContentDecoder} implementation will only handle a small number
     * of types and delegate the handling of other types to other
     * implementations.  By calling this method it is possible to
     * select which {@link ContentDecoder} object's {@link #decode}
     * method is called from {@link #expect}.
     *
     * <p>This method is designed to be mostly useful with {@link
     * ChainedContentDecoder}, but it is usable in other contexts.
     *
     * @param root the new root {@link ContentDecoder}.
     */
    public void setRoot (ContentDecoder root) {
	this.root = root;
    }

    public String toString () {
	return this.getClass().toString() + "(" + prefixMapping + ")";
    }

}
