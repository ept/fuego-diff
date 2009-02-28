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

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

/**
 * An interface for creating chained encoders and decoders.
 * Applications typically need to use specialized {@link
 * ContentEncoder} and {@link ContentDecoder} objects for their
 * specific data types.  Such an application should implement this
 * interface and register it to a lower-level processing component so
 * that existing low-level codecs are also used.
 *
 * <p>The typical scenario is this: A low-level component of the
 * system needs to either serialize or parse a document and use typed
 * content codecs.  It has pre-registered codecs for some standard
 * low-level types.  An application would like for it to also
 * recognize its own higher-level types in all cases.  By registering
 * an object implementing this interface it is possible for the
 * low-level component to permit this, even if the low-level codec is
 * not constant.
 */
public abstract class ContentCodecFactory {

    private static Hashtable typeMappings = new Hashtable();
    private static Vector namespaces = new Vector();

    static {
	try {
	    typeMappings.put(Class.forName("java.lang.Boolean"),
			     new Qname(XasUtil.XSD_NAMESPACE, "boolean"));
	    typeMappings.put(Class.forName("java.lang.Integer"),
			     new Qname(XasUtil.XSD_NAMESPACE, "int"));
	    typeMappings.put(Class.forName("java.lang.String"),
			     new Qname(XasUtil.XSD_NAMESPACE, "string"));
	    typeMappings.put(Class.forName("java.util.Calendar"),
			     new Qname(XasUtil.XSD_NAMESPACE, "dateTime"));
	    typeMappings.put(Class.forName("[B"),
			     new Qname(XasUtil.XSD_NAMESPACE, "base64Binary"));
	    typeMappings.put(Class.forName("java.lang.Long"),
			     new Qname(XasUtil.XSD_NAMESPACE, "long"));
	    typeMappings.put(Class.forName("java.lang.Short"),
			     new Qname(XasUtil.XSD_NAMESPACE, "short"));
	    typeMappings.put(Class.forName("java.lang.Byte"),
			     new Qname(XasUtil.XSD_NAMESPACE, "byte"));
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

    protected static void addNamespace (String namespace, String prefix) {
	namespaces.addElement(new Qname(namespace, prefix));
    }

    public static Qname getXmlName (Class cls) {
	Qname pair = (Qname) typeMappings.get(cls);
	if (pair == null) {
	    for (Enumeration en = typeMappings.keys();
		 en.hasMoreElements(); ) {
		/*
		 * XXX - Note that this should fetch the most specific
		 * class.
		 */
		Class cl = (Class) en.nextElement();
		if (cl.isAssignableFrom(cls)) {
		    pair = (Qname) typeMappings.get(cl);
		    break;
		}
	    }
	}
	return pair;
    }

    /**
     * Add a new mapping from a Java type to an XML type name.  To
     * convert arbitrary Java objects into {@link Event#TYPED_CONTENT}
     * events, it must be known how to encode each possible value.
     * This method is used to map Java classes to XML type names,
     * which are then used by the encoding process to select the
     * appropriate encoder.
     *
     * @param cls the class to map
     * @param name the name of the XML type, with name the namespace
     * URI and value the local name
     */
    public static void addTypeMapping (Class cls, Qname name) {
	typeMappings.put(cls, name);
    }

    /**
     * Return all registered prefix mappings.  Each {@link
     * ContentCodecFactory} is responsible for certain namespaces.
     * When initialized, it needs to register these namespaces and
     * their associated prefixes using the {@link #addNamespace}
     * method.
     *
     * @return the list of all registered namespace-prefix pairs as
     * {@link Qname} objects
     */
    public static Vector getNamespaces () {
	return namespaces;
    }

    /**
     * Register this factory as known.  If a factory needs to be used,
     * it will be registered somewhere.  The registrar is responsible
     * for invoking this method to signal to the factory that it needs
     * to call the {@link #addNamespace} method for each of its
     * namespace responsibilities.
     */
    public void register () {
    }

    /**
     * Get a new chained content encoder.  This method needs to return
     * a {@link ContentEncoder} that understands all the types
     * understood by the specified {@link ContentEncoder}.  It may
     * override the encoding of some of these types, and implement
     * encodings for new types.  Using the Chained Codec idiom is
     * recommended.
     *
     * @param chain the encoder that is to be used for types
     * unrecognized by the new encoder; may be <code>null</code>
     * @return an encoder recognizing all types that
     * <code>chain</code> does and possibly some more
     */
    public abstract ContentEncoder getChainedEncoder (ContentEncoder chain);

    /**
     * Get a new chained content decoder.  This method needs to return
     * a {@link ContentDecoder} that understands all the types
     * understood by the specified {@link ContentDecoder}.  It may
     * override the decoding of some of these types, and implement
     * decodings for new types.  Using the Chained Codec idiom through
     * the {@link ChainedContentDecoder} class is recommended.
     *
     * @param chain the decoder that is to be used for types
     * unrecognized by the new decoder; may be <code>null</code>
     * @return an decoder recognizing all types that
     * <code>chain</code> does and possibly some more
     */
    public abstract ContentDecoder getChainedDecoder (ContentDecoder chain);

}
