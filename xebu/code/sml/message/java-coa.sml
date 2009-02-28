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

(*
 * Jcoa: Output COA machines to Java source files
 *)
structure Jcoa = struct

exception JcoaEx of string

local

    open TextIO
    open Coa

    fun inc node = !node before node := !node + 1

    fun outputPackage sm pkg =
	if size pkg > 0 then
	    output(sm,"package " ^ pkg ^ ";\n\n")
	else ()

    fun outputImports sm =
	(output(sm,"import fuegocore.message.encoding.OutCache;\n");
	 output(sm,"import fuegocore.message.encoding.CachePair;\n");
	 output(sm,"import fuegocore.message.encoding.IdentityEoaMachine;\n");
	 output(sm,"import fuegocore.message.encoding.IdentityDoaMachine;\n");
	 output(sm,"import fuegocore.message.encoding.XebuConstants;\n");
	 output(sm,"import fuegocore.util.Util;\n");
	 output(sm,"import fuegocore.util.xas.Event;\n");
	 output(sm,"import fuegocore.util.xas.XasUtil;\n\n"))

    fun evName node = "event" ^ (Int.toString (inc node))

    fun evCreate event =
	case event of
	    StartDocument => "StartDocument()"
	  | EndDocument => "EndDocument()"
	  | StartElement(u,n) => "StartElement(\"" ^ u ^ "\", \"" ^ n ^ "\")"
	  | Attrib(u,n,v) => "Attribute(\"" ^ u ^ "\", \"" ^ n ^ "\", \"" ^ v
			     ^ "\")"
	  | EndElement(u,n) => "EndElement(\"" ^ u ^ "\", \"" ^ n ^ "\")"
	  | Content(v) => "Content(\"" ^ v ^ "\")"
	  | TypedContent(u,n) => "TypedContent(\"" ^ u ^ "\", \"" ^ n
				 ^ "\", null)"
	  | NamespacePrefix(u,p) => "NamespacePrefix(\"" ^ u ^ "\", \"" ^ p
				    ^ "\")"
	  | Any => ""

    fun evType StartDocument = "Event.START_DOCUMENT"
      | evType EndDocument = "Event.END_DOCUMENT"
      | evType (StartElement _) = "Event.START_ELEMENT"
      | evType (Attrib _) = "Event.ATTRIBUTE"
      | evType (EndElement _) = "Event.END_ELEMENT"
      | evType (Content _) = "Event.CONTENT"
      | evType (TypedContent _) = "Event.TYPED_CONTENT"
      | evType (NamespacePrefix _) = "Event.NAMESPACE_PREFIX"
      | evType Any = ""

    fun isJustType (StartElement("*","*")) = true
      | isJustType (EndElement("*","*")) = true
      | isJustType (TypedContent _) = true
      | isJustType _ = false

    fun isAny Any = true
      | isAny _ = false

    fun outEvent sm (ev,name) =
	if isJustType ev orelse isAny ev then ()
	else output(sm,"private static Event " ^ name ^ " = Event.create"
		       ^ (evCreate ev) ^ ";\n")

    fun update 0 x [] = [[x]]
      | update n x [] = []::(update (n-1) x [])
      | update 0 x (l::ls) = ((x::l)::ls)
      | update n x (l::ls) = l::(update (n-1) x ls)

    fun graph [] = []
      | graph ((s,e,t)::es) =
	let
	    val nes = graph es
	in
	    update s (e,t) nes
	end

in

fun outCache sm pkg prefix (decs,names,values) =
    let
	val clname = prefix ^ "CachePair"
	fun outputName (Rng.Cname(Rng.Uri u,n)) =
	    output(sm,"CachePair.putName(outCaches, inCaches, \""
		      ^ u ^ "\", \"" ^ n ^ "\");\n")
	  | outputName _ = ()
	fun outputNamespace (p,u) =
	    (output(sm,"CachePair.putNamespace(outCaches, inCaches, \""
		       ^ u ^ "\");\n");
	     outputName (Rng.Cname(Rng.Uri u,p)))
	fun outputValue (Rng.Cname(Rng.Uri u,n),v) =
	    output(sm,"CachePair.putValue(outCaches, inCaches, \""
		      ^ u ^ "\", \"" ^ n ^ "\", \"" ^ v ^ "\");\n")
	  | outputValue _ = ()
    in
	(outputPackage sm pkg;
	 outputImports sm;
	 output(sm,"public class " ^ clname ^ " {\n\n");
	 output(sm,"private " ^ clname ^ " () {\n}\n\n");
	 output(sm,"private static CachePair createPair () {\n");
	 output(sm,"OutCache[] outCaches = new OutCache[XebuConstants."
		   ^ "INDEX_NUMBER];\n");
	 output(sm,"Object[][] inCaches = new Object[XebuConstants."
		   ^ "INDEX_NUMBER][XebuConstants.CACHE_SIZE];\n");
	 output(sm,"for (int i = 0; i < outCaches.length; i++) {\n");
	 output(sm,"outCaches[i] = new OutCache();\n}\n");
	 app outputNamespace decs;
	 app outputName names;
	 app outputValue values;
	 output(sm,"return new CachePair(outCaches, inCaches);\n}\n\n");
	 output(sm,"public static CachePair getNewPair () {\n");
	 output(sm,"return createPair();\n}\n\n}\n"))
    end

fun outEoa sm pkg prefix ((start,edges):eoa) =
    let
	val clname = prefix ^ "EoaMachine"
	val node = ref 0
	fun makeEdge (s,{event,kind},t) =
	    (s,{event=event,kind=kind,name=evName node,create=evCreate event},
	     t)
	val nes = map makeEdge edges
	fun strip {event,kind,name,create} = (event,name)
	val gr = graph nes
	fun outGraph l =
	    let
		fun outCases l =
		    let
			fun toCompare (ev,name) =
			    if isJustType ev then
				"ev.getType() == " ^ (evType ev)
			    else "Util.equals(ev, " ^ name ^ ")"
			fun outCases' _ [] = ()
			  | outCases' n (({event,kind,name,create},t)::es) =
			    let
				val init = if n = 0 then "" else "} else "
				val del =
				    case kind of
					Del => "ev = null;\n"
				      | Out => ""
			    in
				(output(sm,init ^ "if ("
					   ^ (toCompare(event,name))
					   ^ ") {\n");
				 output(sm,del);
				 output(sm,"state = " ^ (Int.toString t)
					   ^ ";\n");
				 outCases' 1 es)
			    end
		    in
			outCases' 0 l
		    end
		fun sort g =
		    let
			fun split [] (f,t) = f @ t
			  | split ((e as ({event,kind,name,create},_))::es)
				  (f,t) =
				  if isJustType event then
				      split es (f,e::t)
				  else split es (e::f,t)
		    in
			split g ([],[])
		    end
		fun outGraph' _ [] = ()
		  | outGraph' n ([]::gs) =
		    outGraph' (n+1) gs
		  | outGraph' n (g::gs) =
		    (output(sm,"case " ^ (Int.toString n) ^ ":\n");
		     outCases (sort g);
		     output(sm,"}\nbreak;\n");
		     outGraph' (n+1) gs)
	    in
		outGraph' 0 l
	    end
    in
	(outputPackage sm pkg;
	 outputImports sm;
	 output(sm,"public class " ^ clname ^ " extends IdentityEoaMachine "
		   ^ "{\n\n");
	 app ((outEvent sm) o strip o #2) nes;
	 output(sm,"\npublic " ^ clname ^ " () {\n");
	 output(sm,"state = " ^ (Int.toString start) ^ ";\n}\n\n");
	 output(sm,"public Event nextEvent (Event ev) {\n");
	 output(sm,"if (ev != null) {\nswitch (state) {\n");
	 outGraph gr;
	 output(sm,"default:\n");
	 output(sm,"throw new IllegalStateException(\"EOA machine in invalid "
		   ^ "state \" + state);\n}\n}\nreturn ev;\n}\n\n");
	 output(sm,"public boolean isInitialState () {\n");
	 output(sm,"return state == " ^ (Int.toString start) ^ ";\n}\n\n}\n"))
    end

fun outDoa sm pkg prefix ((start,edges):doa) =
    let
	val clname = prefix ^ "DoaMachine"
	val node = ref 0
	fun isPromise (_,{event,kind=Promise,queued,pushed},t) = true
	  | isPromise _ = false
	fun isPeek (_,{event=Any,kind=Peek,queued,pushed},_) = true
	  | isPeek _ = false
	fun convertLists (s,{event,kind,queued,pushed},t) =
	    let
		fun convert ev = (ev,evName node,evCreate ev)
	    in
		(s,{event=event,kind=kind,queued=map convert queued,
		    pushed=map convert pushed},t)
	    end
	fun makeEdge (s,{event,kind,queued,pushed},t) =
	    (s,{event=event,kind=kind,queued=queued,pushed=pushed,
		name=evName node,create=evCreate event},
	     t)
	fun strip {event,kind,queued,pushed,name,create} = (event,name)
	fun stripi (ev,name,_) = (ev,name)
	val nes = map makeEdge (map convertLists (List.filter
						      (not o isPromise) edges))
	val gr = graph nes
	val pes = map makeEdge (map convertLists (List.filter
						      (fn x => isPromise x
							       orelse isPeek x)
						      edges))
	val pgr = graph pes
	fun outGraph promise l =
	    let
		fun outCases l =
		    let
			fun toCompare (ev,name) =
			    if promise then
				"type == " ^ (evType ev)
			    else if isJustType ev then
				"ev.getType() == " ^ (evType ev)
			    else "Util.equals(ev, " ^ name ^ ")"
			fun outCases' _ [] = ()
			  | outCases' n (({event,kind,queued,pushed,name,
					   create},t)::es) =
			    let
				val init = if n = 0 then "" else "} else "
				fun outElem (_,n,_) =
				    output(sm,"queue.enqueue(" ^ n ^ ");\n")
				val comp =
				    if isAny event then ""
				    else "if (" ^ (toCompare(event,name))
					 ^ ") "
				val next = if promise then "promiseEvent(type)"
					   else "nextEvent(ev)"
			    in
				(output(sm,init ^ comp ^ "{\n");
				 app outElem pushed;
				 output(sm,"state = " ^ (Int.toString t)
					   ^ ";\n");
				 if isAny event then
				     (app outElem queued;
				      output(sm,next ^ ";\n"))
				 else if not promise then
				     output(sm,"queue.enqueue(ev);\n")
				 else ();
				 if not (isAny event) then
				     app outElem queued
				 else ();
				 if not promise then
				     output(sm,"handled = true;\n")
				 else ();
				 outCases' 1 es)
			    end
		    in
			outCases' 0 l
		    end
		fun sort g =
		    let
			fun split [] (f,t) = f @ t
			  | split ((e as ({event,kind,queued,pushed,name,
					   create},_))::es)
				  (f,t) =
				  if isJustType event orelse isAny event then
				      split es (f,e::t)
				  else split es (e::f,t)
		    in
			split g ([],[])
		    end
		fun outGraph' _ [] = ()
		  | outGraph' n ([]::gs) =
		    outGraph' (n+1) gs
		  | outGraph' n (g::gs) =
		    (output(sm,"case " ^ (Int.toString n) ^ ":\n");
		     outCases (sort g);
		     output(sm,"}\nbreak;\n");
		     outGraph' (n+1) gs)
	    in
		outGraph' 0 l
	    end
    in
	(outputPackage sm pkg;
	 outputImports sm;
	 output(sm,"public class " ^ clname ^ " extends IdentityDoaMachine "
		   ^ "{\n\n");
	 output(sm,"private int state = " ^ (Int.toString start) ^ ";\n\n");
	 app ((outEvent sm) o strip o #2) (nes @ pes);
	 app (outEvent sm) (map stripi (List.concat
					    (map (#queued o #2) (nes @ pes))));
	 app (outEvent sm) (map stripi (List.concat
					    (map (#pushed o #2) (nes @ pes))));
	 output(sm,"\npublic void nextEvent (Event ev) {\n");
	 output(sm,"if (ev != null) {\nboolean handled = false;\n");
	 output(sm,"switch (state) {\n");
	 outGraph false gr;
	 output(sm,"default:\n");
	 output(sm,"throw new IllegalStateException(\"DOA machine in invalid "
		   ^ "state \" + state);\n}\n");
	 output(sm,"if (!handled) {\nqueue.enqueue(ev);\n}\n}\n}\n\n");
	 output(sm,"public void promiseEvent (int type) {\n");
	 output(sm,"switch (state) {\n");
	 outGraph true pgr;
	 output(sm,"default:\n}\n");
	 output(sm,"}\n\n}\n"))
    end

end (* local *)

end (* struct Jcoa *)
