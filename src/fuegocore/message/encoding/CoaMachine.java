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
 * A class for associating an EOA and a DOA machine. An object of this class holds a pair consisting
 * of a {@link EoaMachine} and a {@link DoaMachine}. The assumption is that these machines are the
 * inverses of each other. The class is provided so that the association between inverse machines is
 * easily preservable in systems.
 */
public class CoaMachine {

    private EoaMachine eoa;
    private DoaMachine doa;


    /**
     * Construct a new COA machine pair.
     */
    public CoaMachine(EoaMachine eoa, DoaMachine doa) {
        this.eoa = eoa;
        this.doa = doa;
    }


    /**
     * Return the EOA machine part of this pair.
     */
    public EoaMachine getEoaMachine() {
        return eoa;
    }


    /**
     * Return the DOA machine part of this pair.
     */
    public DoaMachine getDoaMachine() {
        return doa;
    }

}
