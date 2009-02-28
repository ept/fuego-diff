package fuegocore.message.encoding.coa;

import java.util.Arrays;

import fuegocore.util.xas.Event;
import fuegocore.util.Util;

class GenericEdge implements Comparable {

    public static final int OUT = 0;
    public static final int DEL = 1;
    public static final int READ = 2;
    public static final int PEEK = 3;
    public static final int PROMISE = 4;

    public int type;
    public int et;
    public String ens;
    public String en;
    public Object ev;
    public Event[] push;
    public Event[] queue;
    public int next;

    public boolean matches (Event e) {
	if (e.getType() == Event.COMMENT
	    || e.getType() == Event.PROCESSING_INSTRUCTION) {
	    return false;
	} else if (et == -1) {
	    return true;
	} else if (et != e.getType()) {
	    return false;
	} else if (ens != null && !Util.equals(ens, e.getNamespace())) {
	    return false;
	} else if (en != null && !Util.equals(en, e.getName())) {
	    return false;
	} else if (ev != null && !Util.equals(ev, e.getValue())) {
	    return false;
	} else {
	    return true;
	}
    }

    private int wildcardValue () {
	return (ens == null ? 1 : 0) + (en == null ? 1 : 0)
	    + (ev == null ? 1 : 0);
    }

    public int compareTo (Object o) {
	if (!(o instanceof GenericEdge)) {
	    throw new ClassCastException("Object " + o + " not a GenericEdge");
	}
	GenericEdge edge = (GenericEdge) o;
	if (et == -1) {
	    return edge.et == -1 ? 0 : 1;
	} else if (edge.et == -1) {
	    return -1;
	} else {
	    int wc = this.wildcardValue();
	    int ewc = edge.wildcardValue();
	    if (wc != ewc) {
		return wc - ewc;
	    } else {
		return type - edge.type;
	    }
	}
    }

    public String toString () {
	StringBuffer result = new StringBuffer();
	result.append("<" + type + "> ");
	result.append(et);
	result.append("(");
	result.append(ens);
	result.append(" ");
	result.append(en);
	result.append(" ");
	result.append(ev);
	result.append(")");
	if (push != null) {
	    result.append(" ");
	    result.append(Arrays.toString(push));
	    result.append(" ");
	    result.append(Arrays.toString(queue));
	}
	result.append(":" + next);
	return result.toString();
    }

}

// arch-tag: 07294344-5011-4114-a470-efd702cf5daa
