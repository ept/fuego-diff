/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 * 
 * This file is a part of Fuego middleware. Fuego middleware is free software; you can redistribute
 * it and/or modify it under the terms of the MIT license, included as the file MIT-LICENSE in the
 * Fuego middleware source distribution. If you did not receive the MIT license with the
 * distribution, write to the Fuego Core project at fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util.log;

/**
 * A {@link Logger} implementation for logging into standard output. This class is intended for use
 * through the {@link Log#PROPERTY_LOGGER} setting as an externally-specified logger.
 * @author Jaakko Kangasharju
 */
public class SysoutLogger extends StreamLogger {

    public SysoutLogger() {
        super(System.out);
    }

}

// arch-tag: 4348f781-74ce-4d89-a10a-302cd580cf29
