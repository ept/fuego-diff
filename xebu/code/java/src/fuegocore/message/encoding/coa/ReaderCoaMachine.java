package fuegocore.message.encoding.coa;

import java.util.List;
import java.util.ArrayList;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import fuegocore.util.xas.Event;
import fuegocore.message.encoding.OutCache;
import fuegocore.message.encoding.CachePair;
import fuegocore.message.encoding.DoaMachine;
import fuegocore.message.encoding.EoaMachine;
import fuegocore.message.encoding.XebuConstants;

public class ReaderCoaMachine {

    List namespaces = new ArrayList();
    List names = new ArrayList();
    List values = new ArrayList();

    GenericEoaMachine eoa;
    GenericDoaMachine doa;

    private static final String[] types = { "SD", "ED", "SE", "A", "EE", "C",
					    "TC", "NP", "CM", "PI", "ER" };

    private Event parseEvent (String line, int index) {
	//System.out.println("parseEvent(" + line + ", " + index + ")");
	String ns = null, n = null, v = null;
	int et = -1;
	int par = line.indexOf('(', index);
	String type = line.substring(index, par);
	for (int i = 0; i < types.length; i++) {
	    if (types[i].equals(type)) {
		et = i;
		break;
	    }
	}
	if (et == -1) {
	    return null;
	}
	int close = line.indexOf(')', par);
	String[] comps = line.substring(par + 1, close).split(" ");
	if (comps.length > 0) {
	    ns = comps[0];
	    if (ns.equals("*")) {
		ns = null;
	    }
	    if (comps.length > 1) {
		n = comps[1];
		if (n.equals("*")) {
		    n = null;
		}
		if (comps.length > 2) {
		    v = comps[2];
		    if (v.equals("*")) {
			v = null;
		    }
		}
	    }
	}
	switch (et) {
	case Event.START_DOCUMENT:
	    return Event.createStartDocument();
	case Event.END_DOCUMENT:
	    return Event.createEndDocument();
	case Event.START_ELEMENT:
	    return Event.createStartElement(ns, n);
	case Event.ATTRIBUTE:
	    return Event.createAttribute(ns, n, v);
	case Event.END_ELEMENT:
	    return Event.createEndElement(ns, n);
	case Event.CONTENT:
	    return Event.createContent(ns);
	case Event.TYPED_CONTENT:
	    return Event.createTypedContent(ns, n, v);
	case Event.NAMESPACE_PREFIX:
	    return Event.createNamespacePrefix(ns, n);
	case Event.COMMENT:
	    return Event.createComment(ns);
	case Event.PROCESSING_INSTRUCTION:
	    return Event.createProcessingInstruction(ns);
	case Event.ENTITY_REFERENCE:
	    return Event.createEntityReference(ns);
	}
	return null;
    }

    private Event[] parseEventList (String line, int index) {
	ArrayList result = new ArrayList();
	while (line.charAt(index) != ']') {
	    if (line.charAt(index) == ' ') {
		index += 1;
	    }
	    result.add(parseEvent(line, index));
	    index = line.indexOf(')', index) + 1;
	}
	return (Event[]) result.toArray(new Event[result.size()]);
    }

    private int parseEdge (String line, GenericEdge edge) {
	int index = line.indexOf(':');
	int start = Integer.parseInt(line.substring(0, index));
	switch (line.charAt(index + 2)) {
	case 'o':
	    edge.type = GenericEdge.OUT;
	    break;
	case 'd':
	    edge.type = GenericEdge.DEL;
	    break;
	case 'p':
	    if (line.charAt(index + 3) == 'e') {
		edge.type = GenericEdge.PEEK;
	    } else {
		edge.type = GenericEdge.PROMISE;
	    }
	    break;
	case 'r':
	    edge.type = GenericEdge.READ;
	    break;
	}
	index = line.indexOf('>', index);
	Event ev = parseEvent(line, index + 2);
	if (ev == null) {
	    edge.et = -1;
	} else {
	    edge.et = ev.getType();
	    edge.ens = ev.getNamespace();
	    edge.en = ev.getName();
	    edge.ev = ev.getValue();
	}
	int dindex = line.indexOf('[', index);
	if (dindex >= 0) {
	    edge.push = parseEventList(line, dindex + 1);
	    dindex = line.indexOf('[', dindex + 1);
	    edge.queue = parseEventList(line, dindex + 1);
	}
	index = line.lastIndexOf(':');
	edge.next = Integer.parseInt(line.substring(index + 1));
	return start;
    }

    public ReaderCoaMachine (Reader reader) throws IOException {
	BufferedReader r = new BufferedReader(reader);
	String line;
	boolean parsingCoa = false;
	while ((line = r.readLine()) != null) {
	    if (line.startsWith("NS(")) {
		int i = 3;
		int j = line.lastIndexOf(')');
		if (j != line.length() - 1) {
		    throw new IOException("Malformed namespace line <" + line
					  + ">");
		}
		namespaces.add(line.substring(i, j));
		names.add(null);
		values.add(null);
	    } else if (line.startsWith("N(")) {
		int i = 2;
		int j = line.indexOf(' ', i);
		int k = line.lastIndexOf(')');
		if (j < i || k < j || k != line.length() - 1) {
		    throw new IOException("Malformed name line <" + line
					  + ">");
		}
		namespaces.add(line.substring(i, j));
		names.add(line.substring(j + 1, k));
		values.add(null);
	    } else if (line.startsWith("V(")) {
		int i = 2;
		int j = line.indexOf(' ', i);
		int k = line.indexOf(' ', j + 1);
		int l = line.lastIndexOf(')');
		if (j < i || k < j || l < k || l != line.length() - 1) {
		    throw new IOException("Malformed value line <" + line
					  + ">");
		}
		namespaces.add(line.substring(i, j));
		names.add(line.substring(j + 1, k));
		values.add(line.substring(k + 1, l));
	    } else if (line.startsWith("EOA(")) {
		parsingCoa = true;
		int i = 4;
		int j = line.lastIndexOf(')');
		int eoaStart;
		try {
		    eoaStart = Integer.parseInt(line.substring(i, j));
		} catch (NumberFormatException ex) {
		    IOException e = new IOException(ex.getMessage());
		    e.initCause(ex);
		    throw e;
		}
		eoa = new GenericEoaMachine(eoaStart);
	    } else if (line.startsWith("DOA(")) {
		eoa.freeze();
		int i = 4;
		int j = line.lastIndexOf(')');
		int doaStart;
		try {
		    doaStart = Integer.parseInt(line.substring(i, j));
		} catch (NumberFormatException ex) {
		    IOException e = new IOException(ex.getMessage());
		    e.initCause(ex);
		    throw e;
		}
		doa = new GenericDoaMachine(doaStart);
	    } else if (!parsingCoa) {
		throw new IOException("Invalid line <" + line + ">");
	    } else {
		GenericEdge edge = new GenericEdge();
		int start = parseEdge(line, edge);
		if (doa != null) {
		    doa.addEdge(start, edge);
		} else {
		    eoa.addEdge(start, edge);
		}
	    }
	}
	doa.freeze();
    }

    public CachePair createNewPair () {
	OutCache[] outCaches = new OutCache[XebuConstants.INDEX_NUMBER];
	Object[][] inCaches =
	    new Object[XebuConstants.INDEX_NUMBER][XebuConstants.CACHE_SIZE];
	for (int i = 0; i < XebuConstants.INDEX_NUMBER; i++) {
	    outCaches[i] = new OutCache();
	}
	for (int i = 0; i < namespaces.size(); i++) {
	    if (values.get(i) != null) {
		CachePair.putValue(outCaches, inCaches,
				   (String) namespaces.get(i),
				   (String) names.get(i), values.get(i));
	    } else if (names.get(i) != null) {
		CachePair.putName(outCaches, inCaches,
				  (String) namespaces.get(i),
				  (String) names.get(i));
	    } else {
		CachePair.putNamespace(outCaches, inCaches,
				       (String) namespaces.get(i));
	    }
	}
	return new CachePair(outCaches, inCaches);
    }

    public EoaMachine createNewEoa () {
	return new GenericEoaMachine(eoa);
    }

    public DoaMachine createNewDoa () {
	return new GenericDoaMachine(doa);
    }

    public String toString () {
	StringBuffer buffer = new StringBuffer();
	buffer.append(namespaces);
	buffer.append("\n");
	buffer.append(names);
	buffer.append("\n");
	buffer.append(values);
	buffer.append("\n");
	buffer.append(eoa);
	buffer.append("\n");
	buffer.append(doa);
	buffer.append("\n");
	return buffer.toString();
    }

}
