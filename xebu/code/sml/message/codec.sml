(*
 * Copyright 2006 Helsinki Institute for Information Technology
 *
 * This file is a part of Fuego middleware.  Fuego middleware is free
 * software; you can redistribute it and/or modify it under the terms
 * of the MIT license, included as the file MIT-LICENSE in the Fuego
 * middleware source distribution.  If you did not receive the MIT
 * license with the distribution, write to the Fuego Core project at
 * fuego-core-users@hoslab.cs.helsinki.fi.
 *)

(* A converter from ORL to Java classes *)

structure Main = struct

local

    open TextIO

    val xsd = "http://www.w3.org/2001/XMLSchema-instance"

    val nspre = [("int",xsd),("string",xsd),("dateTime",xsd),("double",xsd)]
    val clpre = [("int","Integer"),("string","String"),
		 ("dateTime","java.util.Calendar"),("double","Double")]

    fun tmpize n = "tmp" ^ (Util.cap n)

    fun slurp file = Orl.parseTop(TextIO.inputAll (TextIO.openIn file))

    fun outputImports sm =
	(output(sm,"import java.io.IOException;\n");
	 output(sm,"import fuegocore.util.xas.ContentCodecFactory;\n");
	 output(sm,"import fuegocore.util.xas.ContentEncoder;\n");
	 output(sm,"import fuegocore.util.xas.ChainedContentEncoder;\n");
	 output(sm,"import fuegocore.util.xas.ContentDecoder;\n");
	 output(sm,"import fuegocore.util.xas.ChainedContentDecoder;\n");
	 output(sm,"import fuegocore.util.xas.TypedXmlSerializer;\n");
	 output(sm,"import fuegocore.util.xas.XmlWriter;\n");
	 output(sm,"import fuegocore.util.xas.XmlReader;\n");
	 output(sm,"import fuegocore.util.xas.EventList;\n");
	 output(sm,"import fuegocore.util.xas.Qname;\n");
	 output(sm,"import fuegocore.util.xas.XasUtil;\n");
	 output(sm,"import fuegocore.util.Util;\n\n"))

    fun outTypeMappings sm (ts:(string * Orl.pair list) list,nsmap,clmap) =
	let
	    val names = map (#1) ts
	    fun outMapping cl ns n =
		(output(sm,"ContentCodecFactory.addTypeMapping\n");
		 output(sm,"(Class.forName(\"" ^ cl ^ "\"),\n");
		 output(sm,"new Qname(\"" ^ ns ^ "\", \"" ^ n ^ "\"));\n"))
	    fun tocl n = valOf (Util.assocl clmap n)
	    fun tons n = valOf (Util.assocl nsmap n)
	in
	    app (fn n => outMapping (tocl n) (tons n) n) names
	end

    fun outNamespaces sm ds =
	let
	    fun outNamespace (p,n) =
		output(sm,"ContentCodecFactory.addNamespace(\"" ^ n ^ "\", \""
			  ^ p ^ "\");\n")
	in
	    app outNamespace ds
	end

    fun outFactory sm (cpk,cr,ts,ds,nsmap,clmap) =
	let
	    val clname = cr ^ "CodecFactory"
	    val encname = cr ^ "Encoder"
	    val decname = cr ^ "Decoder"
	in
	    (if size(cpk) > 0 then output(sm,"package " ^ cpk ^ ";\n\n")
	     else ();
	     outputImports sm;
	     output(sm,"public class " ^ clname ^ " extends "
		       ^ "ContentCodecFactory {\n\n");
	     output(sm,"private ContentCodecFactory factory;\n\n");
	     output(sm,"static {\ntry {\n");
	     outTypeMappings sm (ts,nsmap,clmap);
	     output(sm,"} catch (Exception ex) {\nex.printStackTrace();\n}\n}\n\n");
	     output(sm,"public " ^ clname ^ " () {\nthis(null);\n}\n\n");
	     output(sm,"public " ^ clname ^ " (ContentCodecFactory factory) {\n");
	     output(sm,"this.factory = factory;\n}\n\n");
	     output(sm,"public void register () {\n");
	     outNamespaces sm ds;
	     output(sm,"}\n\n");
	     output(sm,"public ContentEncoder getChainedEncoder "
		       ^ "(ContentEncoder chain) {\n");
	     output(sm,"if (factory != null) {\n");
	     output(sm,"chain = factory.getChainedEncoder(chain);\n}\n");
	     output(sm,"return new " ^ encname ^ "(chain);\n}\n\n");
	     output(sm,"public ContentDecoder getChainedDecoder "
		       ^ "(ContentDecoder chain) {\n");
	     output(sm,"if (factory != null) {\n");
	     output(sm,"chain = factory.getChainedDecoder(chain);\n}\n");
	     output(sm,"return new " ^ decname ^ "(chain);\n}\n\n}\n"))
	end

    fun outEncoder sm (cpk,cr,ts,ds,nsmap,clmap) =
	let
	    val clname = cr ^ "Encoder"
	    fun outSimple (s,n) =
		let
		    val ns = Util.assocl nsmap s
		in
		    if s = "object" then
			(output(sm,"Qname qname = ContentCodecFactory."
				   ^ "getXmlName(value.getClass());\n");
			 output(sm,"if (qname != null) {\n");
			 output(sm,"xw.typedElement(\"\", \"" ^ n ^ "\", "
				   ^ "qname.getNamespace(), qname.getName(), "
				   ^ "value);\n");
			 output(sm,"} else {\n");
			 output(sm,"throw new IOException(\"Cannot serialize "
				   ^ "type \" + value.getClass() + \" with "
				   ^ "value \" + value);\n}\n"))
		    else
			output(sm,"xw.typedElement(\"\", \"" ^ n ^ "\", \""
				  ^ (valOf ns) ^ "\", \"" ^ s
				  ^ "\", value);\n")
		end
	    fun outPair (t,n) =
		let
		    val get = "c.get" ^ (Util.cap n) ^ "()"
		in
		    (output(sm,"{\nObject value = " ^ get ^ ";\n");
		     case t of
			 Orl.Simple s => outSimple(s,n)
		       | Orl.Optional s =>
			 (output(sm,"if (value != null) {\n");
			  outSimple(s,n);
			  output(sm,"}\n"))
		       | Orl.List s =>
			 (output(sm,"java.util.Vector v = "
					^ "(java.util.Vector) value;");
			  output(sm,"for (java.util.Enumeration e = "
				    ^ "v.elements(); e.hasMoreElements(); ) {\n");
			  outSimple(s,n);
			  output(sm,"}\n"));
			 output(sm,"}\n"))
		end
	    fun outEncTs u (n,ps) =
		let
		    val ns = valOf (Util.assocl nsmap n)
		    val cl = valOf (Util.assocl clmap n)
		in
		    if ns <> u then () else
		    (output(sm,"if (Util.equals(name, \"" ^ n ^ "\")) {\n");
		     output(sm,"if (o instanceof " ^ cl ^ ") {\n");
		     output(sm,"putTypeAttribute(namespace, name, ser);\n");
		     output(sm,cl ^ " c = (" ^ cl ^ ") o;\n");
		     output(sm,"XmlWriter xw = new XmlWriter(ser);\n");
		     app outPair ps;
		     output(sm,"result = true;\n}\n}\n"))
		end
	    fun outEncNs (_,u) =
		(output(sm,"if (Util.equals(namespace, \"" ^ u ^ "\")) {\n");
		 app (outEncTs u) ts;
		 output(sm,"}\n"))
	in
	    (if size(cpk) > 0 then output(sm,"package " ^ cpk ^ ";\n\n")
	     else ();
	     outputImports sm;
	     output(sm,"public class " ^ clname ^ " extends "
		       ^ "ChainedContentEncoder {\n\n");
	     output(sm,"public " ^ clname ^ " (ContentEncoder chain) {\n");
	     output(sm,"this.chain = chain;\n}\n\n");
	     output(sm,"public boolean encode (Object o, String namespace, "
		       ^ "String name, TypedXmlSerializer ser)\n");
	     output(sm,"throws IOException {\n");
	     output(sm,"boolean result = false;\n");
	     app outEncNs ds;
	     output(sm,"if (!result && chain != null) {\n");
	     output(sm,"result = chain.encode(o, namespace, name, ser);\n");
	     output(sm,"}\nreturn result;\n}\n\n}\n"))
	end

    fun outDecoder sm (cpk,cr,ts,ds,nsmap,clmap) =
	let
	    val clname = cr ^ "Decoder"
	    fun strip (Orl.Simple s) = s
	      | strip (Orl.Optional s) = s
	      | strip (Orl.List s) = s
	    fun outList [] = ()
	      | outList [n] = output(sm,n)
	      | outList (n::ns) = (output(sm,n); output(sm,", "); outList ns)
	    fun outSimple (s,n) =
		let
		    val cl = Util.assocl clmap s
		in
		    if s = "object" then
			output(sm,"Object " ^ (tmpize n)
				  ^ " = expect(\"\", \"" ^ n
				  ^ "\", reader);\n")
		    else
			output(sm,(valOf cl) ^ " " ^ (tmpize n) ^ " = ("
				  ^ (valOf cl) ^ ") " ^ "expect(\"\", \"" ^ n
				  ^ "\", reader);\n")
		end
	    fun outPair (t,n) =
		case t of
		    Orl.Simple s => outSimple(s,n)
		  | Orl.Optional s => outSimple(s,n)
		  | Orl.List s => ()
	    fun outDecTs u (n,ps) =
		let
		    val ns = valOf (Util.assocl nsmap n)
		    val cl = valOf (Util.assocl clmap n)
		in
		    if ns <> u then () else
		    (output(sm,"if (Util.equals(name, \"" ^ n ^ "\")) {\n");
		     app outPair ps;
		     output(sm,"result = build" ^ (Util.cap n) ^ "(");
		     outList (map (tmpize o #2) ps);
		     output(sm,");\n}\n"))
		end
	    fun outDecNs (_,u) =
		(output(sm,"if (Util.equals(namespace, \"" ^ u ^ "\")) {\n");
		 app (outDecTs u) ts;
		 output(sm,"}\n"))
	    fun outBuildPair (t,n) =
		let
		    val s = strip t
		    val cl = if s = "object" then "Object"
			     else valOf (Util.assocl clmap s)
		in
		    output(sm,cl ^ " " ^ n)
		end
	    fun outBuildPairs [] = ()
	      | outBuildPairs [p] = outBuildPair p
	      | outBuildPairs (p::ps) = (outBuildPair p; output(sm,", ");
					 outBuildPairs ps)
	    fun outBuild (n,ps) =
		let
		    val cl = valOf (Util.assocl clmap n)
		in
		    (output(sm,"protected abstract " ^ cl ^ " build"
			       ^ (Util.cap n) ^ " (");
		     outBuildPairs ps;
		     output(sm,");\n\n"))
		end
	in
	    (if size(cpk) > 0 then output(sm,"package " ^ cpk ^ ";\n\n")
	     else ();
	     outputImports sm;
	     output(sm,"public abstract class Abs" ^ clname ^ " extends "
		       ^ "ChainedContentDecoder {\n\n");
	     output(sm,"public Abs" ^ clname ^ " (ContentDecoder chain) {\n");
	     output(sm,"super(null);\n");
	     output(sm,"if (chain == null) {\n");
	     output(sm,"throw new IllegalArgumentException(\"Chained decoder "
		       ^ "must be non-null\");\n}\n");
	     output(sm,"this.chain = chain;\n}\n\n");
	     output(sm,"public Object decode (String namespace, String name, "
		       ^ "XmlReader reader, EventList attributes) {\n");
	     output(sm,"Object result = null;\n");
	     app outDecNs ds;
	     output(sm,"if (result == null && chain != null) {\n");
	     output(sm,"result = chain.decode(namespace, name, reader, "
			   ^ "attributes);\n");
	     output(sm,"}\nreturn result;\n}\n\n");
	     app outBuild ts;
	     output(sm,"}\n"))
	end

    fun outFile file =
	case slurp file of
	    NONE => print ("File " ^ file ^ ": Syntax Error\n")
	  | SOME (t as Orl.Top(pk,cpk,cr,ds)) =>
	    let
		val root = Util.cap cr
		val nsnames = OrlEx.nsnames t @ nspre
		val clnames = OrlEx.clnames t @ clpre
		val (ns,ts) = OrlEx.decsplit ds
		val facfile = root ^ "CodecFactory.java"
		val facOut = TextIO.openOut facfile
		val encfile = root ^ "Encoder.java"
		val encOut = TextIO.openOut encfile
		val decfile = "Abs" ^ root ^ "Decoder.java"
		val decOut = TextIO.openOut decfile
	    in
		outFactory facOut (cpk,root,ts,ns,nsnames,clnames);
		outEncoder encOut (cpk,root,ts,ns,nsnames,clnames);
		outDecoder decOut (cpk,root,ts,ns,nsnames,clnames)
	    end

in

fun invoke n [] = print ("Usage: " ^ n ^ " <file>\n")
  | invoke n fs = app (fn s => outFile s) fs

end (* local *)

end (* struct Main *)

val _ = Main.invoke (CommandLine.name()) (CommandLine.arguments())
