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

public class Pi extends Item {

    private String target;
    private String instruction;

    public Pi (String target, String instruction) {
	super(PI);
	Verifier.checkName(target);
	Verifier.checkNotNull(instruction);
	this.target = target;
	this.instruction = instruction;
    }

    public String getTarget () {
	return target;
    }

    public String getInstruction () {
	return instruction;
    }

    public boolean equals (Object o) {
	if (this == o) {
	    return true;
	} else if (!(o instanceof Pi)) {
	    return false;
	} else {
	    Pi p = (Pi) o;
	    return target.equals(p.target)
		&& instruction.equals(p.instruction);
	}
    }

    public int hashCode () {
	return 37 * target.hashCode() + instruction.hashCode();
    }

    public String toString () {
	return "PI(" + target + " " + instruction + ")";
    }

}

// arch-tag: 492c82fd-5861-47b9-8f42-43fc2530d0cf
