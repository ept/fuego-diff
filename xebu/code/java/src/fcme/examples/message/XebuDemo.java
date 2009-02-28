/*
 * Copyright 2006 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-core-users@hoslab.cs.helsinki.fi.
 */

package fcme.examples.message;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

import fuegocore.util.Util;
import fuegocore.util.xas.Event;
import fuegocore.util.xas.EventList;
import fuegocore.util.xas.XasUtil;
import fuegocore.util.xas.TypedXmlSerializer;
import fuegocore.util.xas.CodecFactory;

import fcme.util.xas.CodecIndustry;
import fcme.message.encoding.XebuCodecFactory;

/**
 * A MIDlet demonstrating Xebu encodings.  This program allows the
 * user to input an XML document as a XAS event stream.  After the
 * document is complete, the user can see its representation as a XAS
 * event sequence or any Xebu subtype supported by FCME.
 */
public class XebuDemo extends MIDlet implements CommandListener {

    private Command exitCommand;
    private Command xebuCommand;
    private Command esCommand;
    private Command createCommand;
    private Command clearCommand;
    private Command nsCommand;
    private Command seCommand;
    private Command ieCommand;
    private Command nsFormCommand;
    private Command seFormCommand;
    private Command attFormCommand;
    private Command contFormCommand;

    private Displayable xebuWindow;
    private Displayable createWindow;
    private Displayable endWindow;
    private List nsWindow;
    private List seWindow;
    private List ieWindow;
    private Form nsForm;
    private Form seForm;
    private Form attForm;
    private Form contForm;

    private EventList es;
    private Object token = new Object();
    private String[] names = new String[16];
    private int sp = 0;

    private String[] types = {
	"application/x-ebu+none", "application/x-ebu+item",
	"application/x-ebu+data"
    };

    private void clear () {
	es = new EventList();
	Display.getDisplay(this).setCurrent(createWindow);
    }

    private void addStuff (int index) {
	switch (index) {
	case 0:
	    Display.getDisplay(this).setCurrent(nsForm);
	    break;
	case 1:
	    Display.getDisplay(this).setCurrent(seForm);
	    break;
	case 2:
	    Display.getDisplay(this).setCurrent(contForm);
	    break;
	case 3:
	    if (sp < 2) {
		throw new IllegalStateException("Element end");
	    }
	    sp -= 2;
	    es.add(Event.createEndElement(names[sp], names[sp + 1]));
	    if (sp == 0) {
		es.add(Event.createEndDocument());
		Display.getDisplay(this).setCurrent(endWindow);
	    } else {
		Display.getDisplay(this).setCurrent(ieWindow);
	    }
	    break;
	case 4:
	    Display.getDisplay(this).setCurrent(attForm);
	    break;
	}
    }

    public XebuDemo () {
	XebuCodecFactory.init();
	exitCommand = new Command("Exit", Command.EXIT, 1);
	createCommand = new Command("Create", Command.SCREEN, 2);
	clearCommand = new Command("Clear", Command.SCREEN, 2);
       	xebuCommand = new Command("Xebu", Command.SCREEN, 2);
	esCommand = new Command("ES", Command.SCREEN, 2);
	nsCommand = new Command("Next", Command.ITEM, 3);
	seCommand = new Command("Next", Command.ITEM, 3);
	ieCommand = new Command("Next", Command.ITEM, 3);
	nsFormCommand = new Command("Enter", Command.SCREEN, 0);
	seFormCommand = new Command("Enter", Command.SCREEN, 0);
	attFormCommand = new Command("Enter", Command.SCREEN, 0);
	contFormCommand = new Command("Enter", Command.SCREEN, 0);
	createWindow = new TextBox("Xebu Demo", "", 20, 0);
	createWindow.addCommand(exitCommand);
	createWindow.addCommand(createCommand);
	createWindow.setCommandListener(this);
	endWindow = new TextBox("Xebu Demo", "", 20, 0);
	endWindow.addCommand(exitCommand);
	endWindow.addCommand(esCommand);
	endWindow.addCommand(xebuCommand);
	endWindow.addCommand(clearCommand);
	endWindow.setCommandListener(this);
	xebuWindow = new List("Xebu", List.IMPLICIT, types, null);
	xebuWindow.setCommandListener(this);
	nsWindow = new List("Prefix", List.IMPLICIT,
			    new String[] { "Prefix", "Element" },
			    null);
	nsWindow.setCommandListener(this);
	seWindow = new List("Element", List.IMPLICIT,
			    new String[] { "Prefix", "Element", "Content",
					   "End", "Attribute" }, null);
	seWindow.setCommandListener(this);
	ieWindow = new List("Content", List.IMPLICIT,
			    new String[] { "Prefix", "Element", "Content",
					   "End" }, null);
	ieWindow.setCommandListener(this);
	nsForm = new Form("Prefix");
	nsForm.addCommand(nsFormCommand);
	nsForm.setCommandListener(this);
	seForm = new Form("Element");
	seForm.addCommand(seFormCommand);
	seForm.setCommandListener(this);
	attForm = new Form("Attribute");
	attForm.addCommand(attFormCommand);
	attForm.setCommandListener(this);
	contForm = new Form("Content");
	contForm.addCommand(contFormCommand);
	contForm.setCommandListener(this);
	nsForm.append(new TextField("Namespace", null, 40, TextField.URL));
	nsForm.append(new TextField("Value", null, 40, 0));
	seForm.append(new TextField("Namespace", null, 40, TextField.URL));
	seForm.append(new TextField("Name", null, 15, 0));
	attForm.append(new TextField("Namespace", null, 40, TextField.URL));
	attForm.append(new TextField("Name", null, 15, 0));
	attForm.append(new TextField("Value", null, 40, 0));
	contForm.append(new TextField("Value", null, 40, 0));
    }

    public void startApp () {
	clear();
    }

    public void pauseApp () {
    }

    public void destroyApp (boolean unconditional) {
    }

    public void commandAction (Command c, Displayable d) {
	if (c == exitCommand) {
	    destroyApp(false);
	    notifyDestroyed();
	} else if (c == createCommand) {
	    es.add(Event.createStartDocument());
	    Display.getDisplay(this).setCurrent(nsWindow);
	} else if (c == clearCommand) {
	    clear();
	} else if (c == xebuCommand) {
	    Display.getDisplay(this).setCurrent(xebuWindow);
	} else if (c == esCommand) {
	    Alert esWindow = new Alert("ES", es.toString(), null,
				       AlertType.INFO);
	    esWindow.setTimeout(Alert.FOREVER);
	    Display.getDisplay(this).setCurrent(esWindow);
	} else if (c == List.SELECT_COMMAND) {
	    List l = (List) d;
	    int index = l.getSelectedIndex();
	    if (d == xebuWindow) {
		CodecFactory factory = CodecIndustry.getFactory(types[index]);
		TypedXmlSerializer ser = factory.getNewEncoder(token);
		byte[] xebu;
		ByteArrayOutputStream xebuOut = new ByteArrayOutputStream();
		try {
		    ser.setOutput(xebuOut, "ISO-8859-1");
		    XasUtil.outputSequence(es, ser);
		    ser.flush();
		    xebu = xebuOut.toByteArray();
		} catch (Exception ex) {
		    Alert ioError = new Alert("Output error");
		    ioError.setTimeout(Alert.FOREVER);
		    Display.getDisplay(this).setCurrent(ioError);
		    return;
		}
		Alert window = new Alert("Xebu", Util.toPrintable(xebu), null,
					 AlertType.INFO);
		window.setTimeout(Alert.FOREVER);
		Display.getDisplay(this).setCurrent(window, endWindow);
	    } else {
		addStuff(index);
	    }
	} else if (c == nsFormCommand) {
	    if (d == nsForm) {
		Form f = (Form) d;
		TextField ns = (TextField) f.get(0);
		TextField v = (TextField) f.get(1);
		es.add(Event.createNamespacePrefix(ns.getString(),
						   v.getString()));
	    }
	    Display.getDisplay(this).setCurrent(nsWindow);
	} else if (c == seFormCommand) {
	    if (d == seForm) {
		Form f = (Form) d;
		TextField ns = (TextField) f.get(0);
		TextField n = (TextField) f.get(1);
		es.add(Event.createStartElement(ns.getString(),
						n.getString()));
		names = Util.ensureCapacity(names, sp + 2);
		names[sp] = ns.getString();
		names[sp + 1] = n.getString();
		sp += 2;
	    }
	    Display.getDisplay(this).setCurrent(seWindow);
	} else if (c == attFormCommand) {
	    if (d == attForm) {
		Form f = (Form) d;
		TextField ns = (TextField) f.get(0);
		TextField n = (TextField) f.get(1);
		TextField v = (TextField) f.get(2);
		es.add(Event.createAttribute(ns.getString(), n.getString(),
					     v.getString()));
	    }
	    Display.getDisplay(this).setCurrent(seWindow);
	} else if (c == contFormCommand) {
	    if (d == contForm) {
		Form f = (Form) d;
		TextField v = (TextField) f.get(0);
		es.add(Event.createContent(v.getString()));
	    }
	    Display.getDisplay(this).setCurrent(ieWindow);
	}
    }

}
