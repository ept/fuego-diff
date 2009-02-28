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

package fc.exper;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.kxml2.io.KXmlParser;

import fc.util.log.Log;
import fc.util.log.SysoutLogger;
import fc.xml.xas.AttributeNode;
import fc.xml.xas.EndTag;
import fc.xml.xas.Item;
import fc.xml.xas.ItemSource;
import fc.xml.xas.ItemTarget;
import fc.xml.xas.ItemTransform;
import fc.xml.xas.ParserSource;
import fc.xml.xas.Qname;
import fc.xml.xas.StartTag;
import fc.xml.xas.Text;
import fc.xml.xas.TransformSource;
import fc.xml.xas.XmlOutput;
import fc.xml.xas.XmlPullSource;

public class StructuredSVG implements ItemTransform {

    private static final Qname PATH_TAG_NAME = new Qname(
	    "http://www.w3.org/2000/svg", "path");

    private static final Qname LINETO_TAG_NAME = new Qname(
	    "http://www.w3.org/2000/svg", "lineto");
    
    private static final Qname MOVETO_TAG_NAME = new Qname(
	    "http://www.w3.org/2000/svg", "moveto");

    private static final Qname CURVETO_TAG_NAME = new Qname(
	    "http://www.w3.org/2000/svg", "curveto");

    private static final Qname CLOSE_TAG_NAME = new Qname(
	    "http://www.w3.org/2000/svg", "closepath");
    
    private static final Qname POINT_TAG_NAME = new Qname(
	    "http://www.w3.org/2000/svg", "coord");

    private static final Qname X_TAG_NAME = new Qname(
	    "http://www.w3.org/2000/svg", "x");

    private static final Qname Y_TAG_NAME = new Qname(
	    "http://www.w3.org/2000/svg", "y");

    
    private static final Qname PATH_ATTR = new Qname("", "d");

    
    boolean firstItem = true;
    LinkedList<Item> q = new LinkedList<Item>();
    
    public static void main(String[] args) throws Exception {
	Log.setLogger(new SysoutLogger());
	FileInputStream svgIn = new FileInputStream(args[0]);
	ParserSource pr = new XmlPullSource(new KXmlParser(), svgIn);
	FileOutputStream svgOut = new FileOutputStream(args[1]);
	ItemTarget t = new XmlOutput(svgOut, "UTF-8");
	ItemSource is = new TransformSource(pr, new StructuredSVG());
	long maxItem = Integer.MAX_VALUE;
	for (Item i = null; (i=is.next()) != null; ) {
	    //System.out.println(i);
	    t.append(i);
	    if (maxItem-- < 0) {
		break;
	    }
	}
	svgOut.close();
    }

    public void append(Item i) throws IOException {
	if (i.getType() == Item.START_TAG && 
		PATH_TAG_NAME.equals(((StartTag) i).getName())) {
	    StartTag pathTag = (StartTag) i;
	    AttributeNode pathAttr = pathTag.getAttribute(PATH_ATTR);
	    pathTag.removeAttribute(PATH_ATTR);
	    String path = pathAttr.getValue().toString(); 
	    q.add(pathTag);
	    emitPath(path, pathTag);
	} else {
	    q.add(i);
	}
    }

    enum State { COMMAND, // Expecting command at this point 
	POINTSCAN, // Scanning whitespace for number 
	POINT // Scanning number
	};
    
    private void emitPath(String path, StartTag ctx) {
	State state = State.COMMAND;
	EndTag opened = null;
	int expectedCoords = -1;
	List<Double> points = new ArrayList<Double>(2);
	StringBuilder pointBuf = new StringBuilder();
	for (int i=0; i <= path.length(); i++) {
	    char ch = i < path.length() ? path.charAt(i) : '\u0000';
	    if (state == State.COMMAND) {
		if (opened != null) {
		    q.add(opened);
		    opened = null;
		}
	    	switch (ch) {
	    	case 'L': // lineto
	    	    q.add(new StartTag(LINETO_TAG_NAME, ctx));
	    	    opened = new EndTag(LINETO_TAG_NAME);
	    	    expectedCoords = 1;
		    state = State.POINTSCAN;
	    	    break;	    	    
	    	case 'M': // moveto
	    	    q.add(new StartTag(MOVETO_TAG_NAME, ctx));
	    	    opened = new EndTag(MOVETO_TAG_NAME);
	    	    expectedCoords = 1;
		    state = State.POINTSCAN;
	    	    break;	    	    
	    	case 'C': // curveto
	    	    q.add(new StartTag(CURVETO_TAG_NAME, ctx));
	    	    opened = new EndTag(CURVETO_TAG_NAME);
	    	    expectedCoords = 3;
		    state = State.POINTSCAN;	    	    
	    	    break;
	    	case 'z': // closepath
	    	    q.add(new StartTag(CLOSE_TAG_NAME, ctx));
	    	    q.add(new EndTag(CLOSE_TAG_NAME));
	    	    break;
	    	case '\u0000': // end-of-string
	    	    break;
	    	default:
	    	    Log.error("Unknown command at " + path.substring(i));
	    	} 
	    } else if (state == State.POINTSCAN) {
		if (Character.isDigit(ch) || ch == '-' || ch == '+' || ch == '.') {
		    pointBuf.append(ch);
		    state = State.POINT;
		} else if (!Character.isWhitespace(ch)) {
		    Log.error("Expected point, got " + path.substring(i));
		}
	    } else if (state == State.POINT) {
		if (!Character.isDigit(ch) && ch != '-' && ch != '+' && ch != '.') {
		    double point = -0.0;
		    try {
			point = Double.parseDouble(pointBuf.toString());
			pointBuf.setLength(0);
		    } catch (NumberFormatException ex) {
			Log.error("Bad number: " + pointBuf.toString());
		    }
		    points.add(point);
		    if (points.size() == 2) {
			q.add(new StartTag(POINT_TAG_NAME, ctx));
			q.add(new StartTag(X_TAG_NAME, ctx));
			//q.add(new TypedItem(XasUtil.DOUBLE_TYPE, points.get(0)));
			q.add(new Text(points.get(0).toString()));
			q.add(new EndTag(X_TAG_NAME));
			q.add(new StartTag(Y_TAG_NAME, ctx));
			//q.add(new TypedItem(XasUtil.DOUBLE_TYPE, points.get(1)));
			q.add(new Text(points.get(1).toString()));
			q.add(new EndTag(Y_TAG_NAME));
			q.add(new EndTag(POINT_TAG_NAME));
			points.clear();
			expectedCoords --;
		    }
		    state = expectedCoords == 0 ? state.COMMAND : state.POINTSCAN;
		    i--; // Go back 1 and parse as command/point
		} else {
		    pointBuf.append(ch);
		}
	    } else {
		Log.error("Unknown state " + state);
	    }
	}
	if (points.size() > 0 || pointBuf.length() > 0) {
	    Log.error("Path ended with un-emitted coordinates " + points + ", "+ pointBuf);
	}
    }

    public boolean hasItems() {
	return !q.isEmpty();
    }

    public Item next() throws IOException {
	return q.removeFirst();
    }

}
// arch-tag: 5fb81a2c-6ab7-4b84-9536-4e0b03baf1c6
//
