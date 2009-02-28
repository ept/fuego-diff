/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.message.encoding;

/**
 * Various constants used for Xebu. This class collects the constant values needed by the Xebu
 * serializer and parser. This includes identifier tokens for event types, cache identifiers, and
 * common strings.
 */
public class XebuConstants {

    /*
     * Private constructor to prevent instantiation.
     */
    private XebuConstants() {
    }

    /**
     * Byte mask for the discriminator in a token.
     */
    public static final int TOKEN_SPACE = 0x0F;

    /**
     * Byte mask for the flags in a token.
     */
    public static final int FLAG_SPACE = 0xF0;

    /**
     * The token for the start of a document.
     */
    public static final int DOCUMENT = 0x01;

    /**
     * The token for a namespace prefix mapping.
     */
    public static final int NAMESPACE_START = 0x02;

    /**
     * The token for the start of an element.
     */
    public static final int ELEMENT_START = 0x03;

    /**
     * The token for the end of an element.
     */
    public static final int ELEMENT_END = 0x04;

    /**
     * The token for the start of an attribute.
     */
    public static final int ATTRIBUTE = 0x05;

    /**
     * The token for the end of an attribute list.
     */
    public static final int ATTRIBUTE_END = 0x06;

    /**
     * The token for the start of content.
     */
    public static final int DATA_START = 0x07;

    /**
     * The token for the end of a string.
     */
    public static final int DATA_END = 0x08;

    /**
     * The escape token used to escape {@link #DATA_END} in strings.
     */
    public static final int DATA_ESCAPE = 0x09;

    /**
     * The token for fetching a sequence from the sequence cache.
     */
    public static final int SEQUENCE_FETCH = 0x0A;

    /**
     * The token for entering a sequence into the sequence cache.
     */
    public static final int SEQUENCE_ENTRY = 0x0B;

    /**
     * The token for the start of typed content.
     */
    public static final int TYPED_DATA = 0x0C;

    /**
     * A token indicating that the proper type of the event is coded into the flags. This provides a
     * bit more extendibility when possibly adding tokens in the future.
     */
    public static final int SPECIAL = 0x0F;

    /**
     * The bitmask to use when extracting the type of a {@link #SPECIAL} token from the flags.
     */
    public static final int SPECIAL_MASK = 0x70;

    /**
     * The cache index for namespaces.
     */
    public static final int NAMESPACE_INDEX = 0;

    /**
     * The cache index for namespace-name pairs.
     */
    public static final int NAME_INDEX = 1;

    /**
     * The cache index for namespace-name-value triplets.
     */
    public static final int VALUE_INDEX = 2;

    /**
     * The cache index for element content.
     */
    public static final int CONTENT_INDEX = 3;

    /**
     * The number of item caches.
     */
    public static final int INDEX_NUMBER = 4;

    /**
     * The size of one item cache. This size is tightly coupled with the Xebu specification and must
     * not be increased without serious consideration of the consequences.
     */
    public static final int CACHE_SIZE = 0x100;

    /**
     * The size of the sequence cache. This size is tightly coupled with the Xebu specification and
     * must not be increased without serious consideration of the consequences.
     */
    public static final int SEQUENCE_CACHE_SIZE = 0x1000;

    /**
     * The flag for fetching a namespace from its item cache.
     */
    public static final int NAMESPACE_FETCH = 0x40;

    /**
     * The flag for fetching a namespace-name pair from its item cache.
     */
    public static final int NAME_FETCH = 0x80;

    /**
     * The flag for fetching a namespace-name-value triplet or content from its item cache.
     */
    public static final int VALUE_FETCH = 0xC0;

    /**
     * The flag to indicate a {@link #DATA_START} event may be coalesced with a previous
     * {@link #DATA_START} event.
     */
    public static final int COALESCE_FLAG = 0x20;

    /**
     * The flag to indicate that a {@link #TYPED_DATA} event contains more than one encodings of
     * typed content.
     */
    public static final int TYPED_MULTIDATA_FLAG = 0x20;

    /**
     * The name of the feature to turn item caching on or off.
     */
    public static final String FEATURE_ITEM_CACHING = "http://www.hiit.fi/fuego/fc/xebu/item-cache";

    /**
     * The flag to use with the {@link #DOCUMENT} token to indicate use of item caching in the
     * document.
     */
    public static final int FLAG_ITEM_CACHING = 0x80;

    /**
     * The name of the feature to turn content caching on or off.
     */
    public static final String FEATURE_CONTENT_CACHING = "http://www.hiit.fi/fuego/fc/xebu/content-cache";

    /**
     * The flag to use with the {@link #DOCUMENT} token to indicate use of content caching in the
     * document.
     */
    public static final int FLAG_CONTENT_CACHING = 0x40;

    /**
     * The name of the feature to turn element caching on or off.
     */
    public static final String FEATURE_SEQUENCE_CACHING = "http://www.hiit.fi/fuego/fc/xebu/elem-cache";

    /**
     * The flag to use with the {@link #DOCUMENT} token to indicate use of sequence caching in the
     * document.
     */
    public static final int FLAG_SEQUENCE_CACHING = 0x20;

    /**
     * The property to use for setting the initial item caches.
     */
    public static final String PROPERTY_INITIAL_CACHES = "http://www.hiit.fi/fuego/fc/xebu/init-cache";

    /**
     * The property to use for setting the initial sequence cache.
     */
    public static final String PROPERTY_INITIAL_SEQUENCE_CACHE = "http://www.hiit.fi/fuego/fc/xebu/init-elem-cache";

    /**
     * The property to use for setting at what depth does the element cacher start caching elements.
     */
    public static final String PROPERTY_SEQUENCE_CACHE_DEPTH = "http://www.hiit.fi/fuego/fc/xebu/elem-cache-depth";

    /**
     * The property to use for setting EOA and DOA machines for serializers and parsers.
     */
    public static final String PROPERTY_COA_MACHINE = "http://www.hiit.fi/fuego/fc/xebu/coa-machine";

    /**
     * The special flag value for a processing instructions. Processing instructions are cached into
     * the same cache as content, and their caching also depends on {@link #FEATURE_CONTENT_CACHING}
     * .
     */
    public static final int FLAG_PI = 0x00;

    /**
     * The special flag value for a comment. Comments are never cached, since there would probably
     * be no benefit in that.
     */
    public static final int FLAG_COMMENT = 0x10;

    /**
     * The special flag value for an entity reference. Entity references will be cached into the
     * same cache as content is, except that their caching depends on {@link #FEATURE_ITEM_CACHING},
     * since caching entity references is more useful in general than caching content.
     */
    public static final int FLAG_ENTITY = 0x20;

}
