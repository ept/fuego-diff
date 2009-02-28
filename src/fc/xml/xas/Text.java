/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fuego-xas-users@hoslab.cs.helsinki.fi.
 */

package fc.xml.xas;

public class Text extends Item {

    private String data;


    public Text(String data) {
        super(TEXT);
        Verifier.checkText(data);
        this.data = data;
    }


    public String getData() {
        return data;
    }


    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Text)) {
            return false;
        } else {
            Text t = (Text) o;
            return data.equals(t.data);
        }
    }


    public int hashCode() {
        return 37 * TEXT + data.hashCode();
    }


    public String toString() {
        return "T(" + data + ")";
    }

}

// arch-tag: 88732435-7f02-4c1a-995a-72369b9cd588
