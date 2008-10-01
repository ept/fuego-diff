/*
 * Copyright 2005--2008 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fc-util-users@hoslab.cs.helsinki.fi.
 */

package fc.util;

import java.util.LinkedList;

/** A non-vector based stack.
 * 
 * @author Tancred Lindholm
 *
 * @param <T>
 */

public class Stack<T> {
    
    LinkedList<T> stack = new LinkedList<T>();
    
    public final void push(T o) {
	stack.addFirst(o); // BUGFIX-20061017-3: Push to wrong end of queue
    }

    public final T pop() {
      return stack.removeFirst();
    }
    
    public final T peek() {
	return stack.peek();
    }
    
    public final boolean isEmpty() {
	return stack.isEmpty();
    }
    
    public final void clear() {
	stack.clear();
    }
}

// arch-tag: fc15cebe-7c97-4fba-9366-76a3f1cf6548
