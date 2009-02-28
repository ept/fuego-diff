package fuegocore.message.encoding.coa;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import fuegocore.util.xas.Event;
import fuegocore.message.encoding.IdentityEoaMachine;

public class GenericEoaMachine extends IdentityEoaMachine {

    private int initial;
    private boolean ready;
    private ArrayList edges;

    public GenericEoaMachine (int initial) {
	this.initial = initial;
	this.state = initial;
	this.ready = false;
	edges = new ArrayList(initial + 1);
    }

    public GenericEoaMachine (GenericEoaMachine eoa) {
	this.initial = eoa.initial;
	this.state = this.initial;
	this.ready = true;
	this.edges = eoa.edges;
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

    public Event nextEvent (Event ev) {
	//System.out.println("nextEvent(" + ev + "), state=" + state);
	List curr = (List) edges.get(state);
	if (curr == null) {
	    throw new IllegalStateException("State " + state
					    + " not a valid state");
	}
	int n = curr.size();
	for (int i = 0; i < n; i++) {
	    GenericEdge e = (GenericEdge) curr.get(i);
	    //System.out.println(e);
	    if (e.matches(ev)) {
		state = e.next;
		if (e.type == GenericEdge.DEL) {
		    ev = null;
		}
		break;
	    }
	}
	return ev;
    }

    public boolean isInitialState () {
	return state == initial;
    }

    public String toString () {
	StringBuffer result = new StringBuffer();
	result.append("EOA(");
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

// arch-tag: 5e588ebc-9175-4c50-b290-9829a93a0810
