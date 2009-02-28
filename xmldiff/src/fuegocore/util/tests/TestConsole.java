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

/*
 * $Id: TestConsole.java,v 1.2 2006/02/24 15:36:17 jkangash Exp $
 */
package fuegocore.util.tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SocketHandler;
import java.util.regex.Pattern;

/**
 * This is a simple console application for setting up different
 * parametrized test environments. The idea is that coders implement classes
 * derived from <code>fuegocore.util.test.AdapterClass</code> and introduce
 * these to the class list in the beginning of the source code of this
 * class.
 * <br/>
 * The console enables the user to create arbitrary instances of each test
 * class. These instances may then be further parametrized and configured to
 * suit different situations. The components may have arbitrary functionality.
 * <br/>
 * The advantage over regression tests is that TestConsole provides fast and
 * interactive test reconfigurability. It can also be used to troubleshoot
 * problems revealed by tests, particularily if the components support some
 * kind of reflectivity.
 * <br/>
 * The console can be run interactively or passing a ready-made batch of
 * commands to the stdin.
 * <br>
 * Next improvement: adjusting loggers and log levels.
 *
 * @author Marko Saaresto (@hiit.fi)
 * @version $Revision: 1.2 $
 */
public class TestConsole {

    private Logger log = Logger.getLogger("TestConsole");

    static final HashMap KNOWN_CLASSES = new HashMap();

    // Add class declarations here
    // to avoid dependencies to module specific classes we have to use
    // dynamic class loading
    static {
        KNOWN_CLASSES.put("Root", new TestClass("Root",
            "fuegocore.util.tests.TestConsole$RootClass"));
        KNOWN_CLASSES.put("Trigger", new TestClass("Trigger",
            "fuegocore.util.tests.TestConsole$TriggerClass"));
        KNOWN_CLASSES.put("Logger", new TestClass("Logger",
            "fuegocore.util.tests.TestConsole$LogAdapter"));
        KNOWN_CLASSES.put("PrSend", new TestClass("PrSend",
            "fuegocore.presence.tests.PresenceSender"));
        KNOWN_CLASSES.put("PrRcv", new TestClass("PrRcv",
            "fuegocore.presence.tests.PresenceReceiver"));
        KNOWN_CLASSES.put("PrCtl", new TestClass("PrCtl",
            "fuegocore.presence.tests.PresenceController"));
        KNOWN_CLASSES.put("EventC", new TestClass("EventC",
            "fuegocore.presence.tests.EventController"));
        KNOWN_CLASSES.put("PrTrans", new TestClass("PrTrans",
            "fuegocore.presence.tests.PresenceTransportController"));
    };

    /** this map stores variables String->AdapterClass*/
    HashMap variables = new HashMap();

    /**
     * How long each instruction is allowed to execute before it is
     * considered blocked. The execution is then abandoned (not killed)
     * and the control retuns to the UI.
     */
    private int timeout = 0;

    /**
     * Empty constructor, really!
     */
    private TestConsole() { }

    public static final Pattern alpha = Pattern.compile("^\\w*$");

    private void mainLoop(BufferedReader reader, BufferedWriter writer,
        String initParameter) {

        String thisLine = null;

        AdapterClass root = new RootClass();
        root.create(this, initParameter, "Root");
        root.setLogger(this.log);

        this.variables.put("", root);

        try {
            writer.write("Welcome to test console!\n# ");
            while (true) {
                writer.flush();
                thisLine = reader.readLine();
                if (thisLine==null) {
                    writer.write("** INPUT closed, goodbye! **");
                    return;
                } else {
                    String result = this.uiExec(thisLine);
                    writer.write(result!=null ? result : "[NULL]");
                }
                writer.write("\n# ");
            }
        }
        catch (IOException ex) {
            log.log(Level.SEVERE, "** Exiting due to exectpion", ex);
        }
    }

    /**
     * Executes a line of text as if it was given by the user.
     *
     * @param line The command line
     * @return String The result of the execution
     */
    public String exec(String line) {
        String command, parameter;
        int dot = line.indexOf('.');
        int space = line.indexOf(' ');

        if (dot>=0 && (dot<space||space<0)) {
            command = line.substring(0, dot);
            parameter = line.substring(dot+1);
        } else {
            command = "";
            parameter = line;
        }

        if (!alpha.matcher(command).matches()) {
            return "** Malformed variable name\n# ";
        }

        AdapterClass adapter =
            (AdapterClass)this.variables.get(command);

        if (adapter==null) {
            return "** Unrecognized variable: " + command + "\n# ";
        } else {
            return adapter.invoke(parameter);
        }
    }

    private String uiExec(String line) {
        Executor executor = new Executor(this, line);
        executor.start();
        try {
            executor.join(timeout);
        }
        catch (InterruptedException ignored) { }

        if (executor.isAlive()) {
            executor.interrupt();
            return "** ERROR operation still blocked after " + timeout/1000
                + " seconds. Returning.";
        } else {
            return executor.getResult();
        }
    }

    /**
     * A helper class to perform actual execution in a different thread.
     */
    static final class Executor extends Thread {
        String command;
        String returnee = null;
        TestConsole console;
        private Executor(TestConsole console, String command) {
            super(command);
            super.setDaemon(true);
            this.command = command;
            this.console = console;
        }
        public void run() { this.returnee = this.console.exec(command); }
        private String getResult() { return this.returnee; }
    }


    public static void main(String[] args) {
        TestConsole console = new TestConsole();
        String parameter = null;
        for (int i=0; i<args.length; i++) {
            parameter = parameter==null ? args[i]
                : parameter + " " + args[i];
        }

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(System.in), 1);
        BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(System.out));

        console.mainLoop(reader, writer, parameter);
    }

    /**
     * Internal, half-unnecessary helper class for declaring adapters.
     */
    public final static class TestClass {
        public final String name;
        public final String test;

        public TestClass(String name, String test) {
            this.name = name;
            this.test = test;
        }
    }

    /**
     * Derivates of this class can be instantiated in the TestConsole.
     */
    public static abstract class AdapterClass {
        protected static final String SUCCESS = "** SUCCESS";
        protected Logger log = Logger.getLogger("TestConsole");
        protected Writer writer = null;
        protected TestConsole console;
        protected String name;

        /**
         * General "constructor". All AdapterClasses are instantiated using
         * a construct with no arguments. This method will be called right
         * after instantiation.
         *
         * @param console A reference to the instantiating console, is stored
         *                to fiel <code>console</code>
         * @param parameter An arbitrary user-given string, not stored
         * @param name The UI class name assigned for this Java Class, stored
         *             in the field <code>name</name>
         * @return String An arbitrary message that is printed for the user.
         *                <code>null</code> should be interpreted as a failure.
         */
        public String create(TestConsole console, String parameter,
                             String name) {
            this.name = name;
            this.console = console;
            return SUCCESS;
        }

        /**
         * Called when this instance is disposed.
         *
         * @param parameter An arbitrary parameter given by the user
         * @return String A result string printed for the user
         */
        public abstract String destroy(String parameter);

        /**
         * Called when the user invokes a function on this instance.
         * @param parameter Any parameters the user has given.
         * @return String The result of the execution to be printed.
         */
        public abstract String invoke(String parameter);

        /**
         * Get help on the usage of this instance. This text may well vary
         * depending on the current state of the instance.
         * @return String Any helpful message.
         */
        public abstract String getHelp();

        /** Just returns the name, do not mess with */
        public String getName() { return this.name; }

        /**
         * Request that if the test adapter has any logging to do, it will
         * do it to the provided <code>java.util.logging.Logger</code>
         * instance. The default implementation just stores the reference
         * in the field <code>log</code>.
         * @param log The reference *DUH*
         */
        public void setLogger(Logger log) {  this.log = log; }

        /**
         * This requests that if the test has any significant amount of data
         * to write, it should be written to the given Writer. Especially
         * useful for backgroup operation. The default implementation just
         * stores the the Writer reference to field <code>writer</code>.
         * @param writer The java.io.Writer reference
         */
        public void setWriter(Writer writer) { this.writer = writer; }
    }

    /**
     * The default class to handle the basic UI functionality. Namely
     * instantiating new classes and handling invocations.
     */
    public static final class RootClass extends AdapterClass {
        public String destroy(String parameter) { return SUCCESS; }
        public String invoke(String parameter) {
            if ("?".equals(parameter)) {
                return this.getHelp();
            } else
            if (parameter.startsWith("? ")) {
                parameter = parameter.substring(2);
                AdapterClass variable = (AdapterClass)
                    console.variables.get(parameter);
                return (variable!=null)
                    ? variable.getHelp() : "** ERROR no such variable";
            } else
            if (parameter.startsWith("new ")) {
                parameter = parameter.substring(4);
                int space = parameter.indexOf(' ');
                if (space<=0) { return "** ERROR Invalid class name"; }
                String className = parameter.substring(0, space);
                parameter = parameter.substring(space+1);

                TestClass aClass = (TestClass)KNOWN_CLASSES.get(className);
                if (aClass==null) { return "** ERROR No such class name"; }

                if (parameter.length()<1) {
                    return "** ERROR Invalid variable name";
                }

                String variable = parameter;
                space = parameter.indexOf(' ');
                if (space>0) {
                    variable = parameter.substring(0, space);
                    parameter = parameter.substring(space+1);
                } else {
                    parameter = "";
                }

                AdapterClass adapter = null;
                try {
                    adapter = (AdapterClass)
                        Class.forName(aClass.test).newInstance();
                }
                catch (InstantiationException ex) {
                    log.log(Level.WARNING,
                            "Error instantiating " + aClass.test, ex);
                    return "** ERROR: " + ex.getMessage();
                }
                catch (IllegalAccessException ex) {
                    log.log(Level.WARNING,
                            "Error instantiating " + aClass.test, ex);
                    return "** ERROR: " + ex.getMessage();
                }
                catch (ClassNotFoundException ex) {
                    log.log(Level.WARNING,
                        "Adapter class not found: " + aClass.test, ex);
                    return "** ERROR: Adapter class not available";
                }

                String returnee =
                    adapter.create(this.console, parameter, aClass.name);
                if (returnee!=null) {
                    console.variables.put(variable, adapter);
                }
                return returnee;
            } else
            if (parameter.startsWith("del ")) {
                parameter = parameter.substring(4);
                int space = parameter.indexOf(' ');
                String option = "";
                if (space>0) {
                    option = parameter.substring(space+1);
                    parameter = parameter.substring(0, space);
                }
                AdapterClass removable =
                    (AdapterClass)console.variables.remove(parameter);
                if (removable!=null) {
                    return removable.destroy(option);
                } else
                    return "** ERROR no such variable";
            } else
            if ("list".equals(parameter)) {
                StringBuffer returnee = new StringBuffer();
                Iterator cIter = KNOWN_CLASSES.values().iterator();
                Iterator vIter =
                    console.variables.entrySet().iterator();
                returnee.append("** Known CLASSES:");
                while (cIter.hasNext()) {
                    returnee.append(' ').append(((TestClass)cIter.next()).name);
                }
                returnee.append("\n** Known variables:");
                while (vIter.hasNext()) {
                    java.util.Map.Entry entry =
                        (java.util.Map.Entry)vIter.next();
                    returnee.append(" ").append(entry.getKey()).append("(")
                        .append(((AdapterClass)entry.getValue()).getName())
                        .append(")");
                }
                return returnee.toString();
            } else
            if (parameter.startsWith("write ")) {
                parameter = parameter.substring(6);
                int space = parameter.indexOf(' ');
                if (space<1) { return "** ERROR missing filename"; }
                String filename = parameter.substring(space+1);
                parameter = parameter.substring(0, space);
                AdapterClass adapter =
                    (AdapterClass)this.console.variables.get(parameter);
                if (adapter==null) { return "** ERROR no such object"; }
                try {
                    FileWriter file = new FileWriter(filename, true);
                    adapter.setWriter(file);
                }
                catch (IOException ex) {
                    log.log(Level.WARNING, "Error opening file " + filename,ex);
                    return "** ERROR: " + ex.getMessage();
                }

                return "** Output from " + parameter + " requested to " +
                    filename;
            } else
            if (parameter.startsWith("wait ")) {
                parameter = parameter.substring(5);
                try {
                    int waitTime = Integer.parseInt(parameter);
                    if (waitTime<0) return "** ERROR negative time";
                    Thread.sleep(waitTime*1000);
                }
                catch (NumberFormatException ex) {
                    return "** ERROR invalid numeric format";
                }
                catch (InterruptedException ignored) { }
                return SUCCESS;
            } else
            if (parameter.startsWith("timeout ")) {
                parameter = parameter.substring(8);
                try {
                    int waitTime = Integer.parseInt(parameter);
                    if (waitTime<0) return "** ERROR negative time";
                    this.console.timeout = waitTime*1000;
                }
                catch (NumberFormatException ex) {
                    return "** ERROR invalid numeric format";
                }
                return SUCCESS;
            } else
            if ("quit".equals(parameter)) {
                System.exit(0);
                return "** GOODBYE **";
            } else
                return "** ERROR Unsupported operation";
        }

        public String getHelp() {
            return
                "?\tThis help\n" +
                "? NAME\tCommands that a particular object accepts\n" +
                "new CLASS NAME PARAMETERS\tCreate a new object of " +
                "type CLASS to NAME with PARAMETERS\n" +
                "del NAME\tRemove the object NAME\n" +
                "list\tList available CLASSES and VARIABLES\n" +
                "write NAME FILE\tDirect data output from object NAME to " +
                "file FILE\n" +
                "wait N\tWait for N seconds (for batch processing)\n" +
                "timeout N\tChange the default (0) non-responsiveness " +
                "timeout to N seconds\n" +
                "quit\tTerminates Test Console\n";
        }
    } //RootClass

    /**
     * A core supporting Class that allows grouping a set of commands
     * to a set that can be invoked together either at the same time
     * (paraller) of as a batch (serial). When firing the execution this
     * class will return only after all invocations are complete and then
     * return the results of each at the same time, in the order they were
     * given.
     */
    public static final class TriggerClass extends AdapterClass {
        private boolean serial = true;
        private ArrayList invocations = new ArrayList();

        public String destroy(String parameter) { return SUCCESS; }
        public String invoke(String parameter) {
            if ("serial".equals(parameter)) {
                this.serial = true;
                return "** SERIAL MODE";
            } else
            if ("paraller".equals(parameter)) {
                this.serial = false;
                return "** PARALLER MODE";
            } else
            if ("list".equals(parameter)) {
                StringBuffer returnee = new StringBuffer();
                returnee.append("Current execution queue")
                    .append(this.serial ? "(serial)" : "(paraller)");
                Iterator iter = this.invocations.iterator();
                while (iter.hasNext()) {
                    returnee.append("\n\t").append(iter.next().toString());
                }
                return returnee.toString();
            } else
            if (parameter.startsWith("import ")) {
                parameter = parameter.substring(7);
                try {
                    BufferedReader reader =
                        new BufferedReader(new FileReader(parameter));
                    String line;
                    while ((line=reader.readLine())!=null) {
                        if (line.length()>0 && !line.startsWith("#")) {
                            this.invocations.add(line);
                        }
                    }
                    reader.close();
                }
                catch (FileNotFoundException ex) {
                    log.log(Level.WARNING, "not found: " + parameter, ex);
                    return "** ERROR file not found";
                }
                catch (IOException ex) {
                    log.log(Level.WARNING, "failed to access: "+parameter, ex);
                    return "** ERROR could not read file: " + ex.getMessage();
                }
                return SUCCESS;
            } else
            if ("fire".equals(parameter)) {
                return this.serial ? fireSerial() : fireParaller();
            } else
            if (parameter.startsWith("add ")) {
                String command = parameter.substring(4);
                if (command.length()>0) {
                    this.invocations.add(command);
                    return "** ADDED";
                } else {
                    return "** EMPTY COMMAND, NOT ADDED";
                }
            } else
                return "** ERROR unsupported operation";
        }

        public String getHelp() {
            return
                "Triggers several object invocations at one time" +
                "serial\tInvoke objects in serial order (default)\n" +
                "paraller\tInvoke objects in separate threads\n" +
                "add INVOCATION\tAdd an invocation to the list\n" +
                "import FILE\tImport text file as a list of commands\n" +
                "list\tList current invocation list\n" +
                "fire\tDo the invocations\n";
        }

        /** Serial execution of commands */
        private String fireSerial() {
            Iterator iter = this.invocations.iterator();
            StringBuffer returnee = new StringBuffer();
            returnee.append("Executing queue of ")
                .append(this.invocations.size()).append(" commands");
            while (iter.hasNext()) {
                String next = iter.next().toString();
                returnee.append("\n>> executing: " + next).append("\n")
                    .append(this.console.exec(next));
            }
            return returnee.toString();
        }

        /** Paraller execution of commands */
        private String fireParaller() {
            Iterator iter = this.invocations.iterator();
            StringBuffer returnee = new StringBuffer();
            returnee.append("Executing queue of ")
                .append(this.invocations.size()).append(" commands");
            ArrayList threads = new ArrayList();
            while (iter.hasNext()) {
                String next = iter.next().toString();
                Thread thread = new Executor(this.console, next);
                threads.add(thread);
                thread.start();
            }

            iter = threads.iterator();
            while (iter.hasNext()) {
                Executor thread = (Executor)iter.next();
                try { thread.join(); }
                catch (InterruptedException ignored) { }
                returnee.append("\n>> execution: ").append(thread.command)
                    .append(thread.getResult());
            }

            return returnee.toString();
        }
    } //TriggerClass

    /**
     * This adapter can configure the Java2 logging facility, redirecting
     * loggers to files or stdout.
     */
    public static final class LogAdapter extends AdapterClass {
        private String thisName = null;
        private String thisOutput = null;
        private Logger myLogger = null;
        private Handler logHandler = null;

        public String create(
            TestConsole console,
            String parameter,
            String name) {

            if (parameter!=null && parameter.length()>0) {
                this.myLogger = Logger.getLogger(parameter);
                this.thisName = parameter;
                Handler[] handlers = this.myLogger.getHandlers();
                for (int i=0; i<handlers.length; i++) {
                    handlers[i].setLevel(Level.OFF);
                }
                this.logHandler = new ConsoleHandler();
                this.thisOutput = "<stderr>";
                this.myLogger.addHandler(this.logHandler);
                if (this.myLogger.getLevel()==null) {
                    this.myLogger.setLevel(Level.INFO);
                }
                this.logHandler.setLevel(this.myLogger.getLevel());
            }

            return super.create(console, parameter, name);
        }

        public String destroy(String parameter) {
            if (this.logHandler instanceof SocketHandler
                || this.logHandler instanceof FileHandler) {

                this.myLogger.addHandler(new ConsoleHandler());
                this.myLogger.removeHandler(this.logHandler);
                this.logHandler.close();
            }

            return SUCCESS;
        }

        public String getHelp() {
            return
                "list\tList all currently known Loggers\n" +
                "out -|FILE|host:port\tSet the output of this Logger\n" +
                "attach name\tAttach this Logger for the given object\n" +
                "level LEVEL\tSet the log level, either name or number " +
                "[0-1000]\n" +
                "THIS: " + this.thisName + "\n" +
                "OUTPUT: "+ this.thisOutput + "\nLEVEL: " +
                (this.myLogger==null ? "" : this.myLogger.getLevel().getName());
        }

        public String invoke(String parameter) {
            assert parameter!=null && parameter.length()>0
                : "Invocation with empty parameter is illegal";

            if ("list".equals(parameter)) {
                LogManager manager = LogManager.getLogManager();
                Enumeration en = manager.getLoggerNames();
                StringBuffer returnee = new StringBuffer("Existing loggers:");
                while (en.hasMoreElements()) {
                    returnee.append("\n\t").append(en.nextElement().toString());
                }
                return returnee.toString();
            } else
            if (parameter.startsWith("out ")) {
                if (this.myLogger==null) {
                    return "** ERROR no logger associated";
                }

                parameter = parameter.substring(4);
                Handler newHandler;

                if ("-".equals(parameter)) {
                    newHandler = new ConsoleHandler();
                    this.thisOutput = "<stderr>";
                } else
                if (parameter.indexOf(':')>0) {
                    int index = parameter.indexOf(':');
                    if (index>=parameter.length()) {
                        return "** ERROR port must be defined";
                    }
                    String host = parameter.substring(0, index);
                    String port = parameter.substring(index+1);
                    try {
                        newHandler = new SocketHandler(
                            host, Integer.parseInt(port));
                        this.thisOutput = "network: " + parameter;
                    }
                    catch (NumberFormatException ex) {
                        return "** ERROR port must be numeric: " + port;
                    }
                    catch (IllegalArgumentException ex) {
                        return "** ERROR invalid host or port: "
                            + parameter
                            + "(" + ex.getMessage() + ")";
                    }
                    catch (IOException ex) {
                        return "** ERROR can not connect: "
                            + parameter
                            + "(" + ex.getMessage() + ")";
                    }
                } else {
                    try {
                        newHandler = new FileHandler(parameter);
                        this.thisOutput = "file: " + parameter;
                    }
                    catch (IOException ex) {
                        return "** ERROR opening file " + parameter
                            + "(" + ex.getMessage() + ")";
                    }
                    catch (SecurityException ex) {
                        return "** ERROR opening file " + parameter
                            + "(" + ex.getMessage() + ")";
                    }
                } // done selecting the right handler

                this.myLogger.addHandler(newHandler);
                newHandler.setLevel(this.myLogger.getLevel());
                this.myLogger.removeHandler(this.logHandler);
                this.logHandler = newHandler;

                return SUCCESS + " (" + this.thisOutput + ")";
            } else
            if (parameter.startsWith("attach ")) {
                if (this.myLogger==null) {
                    return "** ERROR no logger associated";
                }

                parameter = parameter.substring(7);
                AdapterClass object =
                    (AdapterClass)this.console.variables.get(parameter);
                if (object!=null) {
                    object.setLogger(this.myLogger);
                    return SUCCESS;
                } else {
                    return "** ERROR no such object";
                }
            } else
            if (parameter.startsWith("level ")) {
                if (this.myLogger==null) {
                    return "** ERROR no logger associated";
                }

                parameter = parameter.substring(6);
                try {
                    Level level = Level.parse(parameter);
                    this.myLogger.setLevel(level);
                    this.logHandler.setLevel(level);
                    return SUCCESS + " level: " + level.getName();
                }
                catch (IllegalArgumentException ex) {
                    return "** ERROR unrecognized log level: " + parameter
                        + " (" + ex.getMessage() + ")";
                }
            } else {
                return "** ERROR unrecognized command";
            }
        } //invoke()

    } //LogAdapter
}

