package fuegocore.message.encoding.coa;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import fuegocore.util.xas.Event;
import fuegocore.message.encoding.IdentityDoaMachine;

public class GenericDoaMachine extends IdentityDoaMachine {

    private int initial;
    private int state;
    private boolean ready;
    private ArrayList edges;

    public GenericDoaMachine (int initial) {
	this.initial = initial;
	this.state = initial;
	this.ready = false;
	edges = new ArrayList(initial + 1);
    }

    public GenericDoaMachine (GenericDoaMachine doa) {
	this.initial = doa.initial;
	this.state = this.initial;
	this.ready = true;
	this.edges = doa.edges;
    }

    private void ensureCapacity (int state) {
	edges.ensureCapacity(state + 1);
	int diff = state + 1 - edges.size();
	for (int i = 0; i < diff; i++) {
	    edges.add(null);
	}
    }

    public void addEdge (int start, GenericEdge edge) {
	if (ready) {
	    throw new IllegalStateException("Machine already initialized");
	}
	ensureCapacity(start);
	List curr = (List) edges.get(start);
	if (curr == null) {
	    curr = new ArrayList();
	    edges.set(start, curr);
	}
	curr.add(edge);
    }

    public void freeze () {
	ready = true;
	for (Iterator i = edges.iterator(); i.hasNext(); ) {
	    List curr = (List) i.next();
	    if (curr != null) {
		Collections.sort(curr);
	    }
	}
	if (edges.size() == 0) {
	    edges.add(Collections.emptyList());
	}
    }

    public void nextEvent (Event ev) {
	List curr = (List) edges.get(state);
	if (curr == null) {
	    throw new IllegalStateException("State " + state
					    + " not a valid state");
	}
	int n = curr.size();
	for (int i = 0; i < n; i++) {
	    GenericEdge e = (GenericEdge) curr.get(i);
	    if (e.matches(ev)) {
		if (e.push != null) {
		    for (int j = 0; j < e.push.length; j++) {
			queue.enqueue(e.push[j]);
		    }
		}
		if (e.type == GenericEdge.READ) {
		    queue.enqueue(ev);
		}
		if (e.queue != null) {
		    for (int j = 0; j < e.queue.length; j++) {
			queue.enqueue(e.queue[j]);
		    }
		}
		state = e.next;
		break;
	    }
	}
    }

    public void promiseEvent (int type) {
	List curr = (List) edges.get(state);
	if (curr == null) {
	    throw new IllegalStateException("State " + state
					    + " not a valid state");
	}
	int n = curr.size();
	for (int i = 0; i < n; i++) {
	    GenericEdge e = (GenericEdge) curr.get(i);
	    if (e.et == type) {
		if (e.push != null) {
		    for (int j = 0; j < e.push.length; j++) {
			queue.enqueue(e.push[j]);
		    }
		}
		if (e.queue != null) {
		    for (int j = 0; j < e.queue.length; j++) {
			queue.enqueue(e.queue[j]);
		    }
		}
		state = e.next;
		break;
	    }
	}
    }

    public String toString () {
	StringBuffer result = new StringBuffer();
	result.append("DOA(");
	result.append(initial);
	result.append(")\n");
	for (int i = 0; i < edges.size(); i++) {
	    List curr = (List) edges.get(i);
	    if (curr != null) {
		for (Iterator it = curr.iterator(); it.hasNext(); ) {
		    result.append(i);
		    result.append(":");
		    result.append(it.next());
		    result.append("\n");
		}
	    }
	}
	return result.toString();
    }

}

// arch-tag: 9d4650c9-478a-4942-b7a5-8bd0b6703b7d
