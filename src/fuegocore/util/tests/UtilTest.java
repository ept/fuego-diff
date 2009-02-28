/*
 * Copyright 2006 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fuegocore.util.tests;

import junit.framework.TestCase;

import fuegocore.util.Util;

/**
 * Test the miscellaneous utility methods. This class implements tests for the methods in the
 * {@link Util} class of static methods.
 */
public class UtilTest extends TestCase {

    public UtilTest(String name) {
        super(name);
    }


    /**
     * Test that the hexadecimal encoding works correctly. The {@link Util#toPrintable} method is
     * defined to output printable ASCII as such and URL-escape all other characters. This method
     * compares the result of that method's output to a pre-calculated string with the correct
     * content.
     */
    public void testToHex() {
        String correct = "%00%01%02%03%04%05%06%07%08%09%0A%0B%0C%0D%0E%0F"
                         + "%10%11%12%13%14%15%16%17%18%19%1A%1B%1C%1D%1E%1F" + " !\"#$%&'()*+,-./"
                         + "0123456789:;<=>?" + "@ABCDEFGHIJKLMNO" + "PQRSTUVWXYZ[\\]^_"
                         + "`abcdefghijklmno" + "pqrstuvwxyz{|}~%7F"
                         + "%80%81%82%83%84%85%86%87%88%89%8A%8B%8C%8D%8E%8F"
                         + "%90%91%92%93%94%95%96%97%98%99%9A%9B%9C%9D%9E%9F"
                         + "%A0%A1%A2%A3%A4%A5%A6%A7%A8%A9%AA%AB%AC%AD%AE%AF"
                         + "%B0%B1%B2%B3%B4%B5%B6%B7%B8%B9%BA%BB%BC%BD%BE%BF"
                         + "%C0%C1%C2%C3%C4%C5%C6%C7%C8%C9%CA%CB%CC%CD%CE%CF"
                         + "%D0%D1%D2%D3%D4%D5%D6%D7%D8%D9%DA%DB%DC%DD%DE%DF"
                         + "%E0%E1%E2%E3%E4%E5%E6%E7%E8%E9%EA%EB%EC%ED%EE%EF"
                         + "%F0%F1%F2%F3%F4%F5%F6%F7%F8%F9%FA%FB%FC%FD%FE%FF";
        byte[] result = new byte[0x100];
        for (int i = 0; i < 0x100; i++) {
            result[i] = (byte) i;
        }
        assertEquals("Incorrect result", correct, Util.toPrintable(result, 0, result.length));
    }

}
