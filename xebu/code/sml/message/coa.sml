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

(* A converter from RELAX NG to COA automata *)

structure Main = struct

local

    fun slurp file = Rng.parseTop(TextIO.inputAll (TextIO.openIn file))

    fun printHelp name =
	(print (name ^ ": transform a schema to a COA machine\n\n");
	 print "Available commands:\n";
	 print "help\tPrint this help text\n";
	 print "print\tPrint schemas from argument files after simplification\n";
	 print "raw\tPrint schemas from argument files without simplification\n";
	 print "defs\tFind all undefined names in argument files\n";
	 print "out\tOutput a textual specification for COA machines\n";
	 print "java\tOutput Java classes for COA machines\n")

    fun printRaw file =
	case slurp file of
	    NONE => print ("File " ^ file ^ ": Syntax Error\n")
	  | SOME g => Print.printTop g

    fun printFile file =
	case slurp file of
	    NONE => print ("File " ^ file ^ ": Syntax Error\n")
	  | SOME g => Print.printPattern (Transform.simplify g)

    (*
     * Parse and check the file for use of undefined names
     *)
    fun checkDef file =
	case slurp file of
	    NONE => print ("File " ^ file ^ ": Syntax Error\n")
	  | SOME g =>
	    let
		val p = Transform.simplify g
		val undefs = Transform.checkPatt (Transform.defPatt p) p
	    in
		(print ("File " ^ file ^ "\n");
		 case undefs of
		     [] => print "No undefined symbols\n"
		   | l => (print "Undefined symbols: ";
			   Print.printList ", " (Util.uniquify l);
			   print "\n"))
	    end

    (*
     * Parse file and output the initial tokenization as well as the
     * EOA and DOA machines.  Machine edges are output using the
     * functions in the Print structure.
     *)
    fun outCoa file =
	if String.extract(file, size(file)-4, NONE) <> ".rnc" then
	    print ("File name " ^ file ^ " does not end '.rnc'\n")
	else
	    case slurp file of
		NONE => print ("File " ^ file ^ ": Syntax Error\n")
	      | SOME (g as Rng.Top(d,p)) =>
		let 
		    val defPair =
			("xsd", "http://www.w3.org/2001/XMLSchema-datatypes")
		    val ds = d @ (Transform.declPatt p)
		    val patt = Transform.simplify g
		    fun pairify (Rng.Namespace(p,u)) = (p,u)
		      | pairify (Rng.Datatype(p,u)) = (p,u)
		    val tokFile = (substring(file, 0, size(file) - 4)) ^ ".tok"
		    val tokOut = TextIO.openOut tokFile
		    val decs = Util.uniquify (defPair :: (map pairify ds))
		    val names =
			Util.uniquify (Transform.namePatt patt)
		    val values =
			Util.uniquify (Transform.valuePatt decs patt)
		    fun expand (p,u) = Rng.Cname(Rng.Uri u,p)
		    fun trunc l =
			let fun trunc' 0 _ = []
			      | trunc' _ [] = []
			      | trunc' n (x::xs) = x::(trunc' (n-1) xs)
			in
			    trunc' 256 l
			end
		    fun outNs u = TextIO.output(tokOut, ("NS(" ^ u ^ ")\n"))
		    fun outPair (p,u) =
			TextIO.output(tokOut, ("N(" ^ u ^ " " ^ p ^ ")\n"))
		    fun outName (Rng.Cname(Rng.Uri u,l)) =
			TextIO.output(tokOut, "N(" ^ u ^ " " ^ l ^ ")\n")
		      | outName _ = ()
		    fun outValue (Rng.Cname(Rng.Uri u,l),v) =
			TextIO.output(tokOut, "V(" ^ u ^ " " ^ l ^ " " ^ v
					      ^ ")\n")
		      | outValue _ = ()
		    val (eoaStart,eoaEdges) = Coa.buildEoa decs patt
			handle OutEx => (0,[])
		    val (doaStart,doaEdges) = Coa.buildDoa decs patt
			handle OutEx => (0,[])
		in
		    (app outNs (trunc (Util.uniquify (map (#2) decs)));
		     app outName (trunc (Util.uniquify
					     ((map expand decs) @ names)));
		     app outValue (trunc values);
		     TextIO.output(tokOut, "EOA(" ^ Int.toString(eoaStart)
					   ^ ")\n");
		     app (fn x => (Print.outputEoaEdge tokOut x;
				   TextIO.output(tokOut, "\n")))
			 eoaEdges;
		     TextIO.output(tokOut, "DOA(" ^ Int.toString(doaStart)
					   ^ ")\n");
		     app (fn x => (Print.outputDoaEdge tokOut x;
				   TextIO.output(tokOut, "\n")))
			 doaEdges)
		end
		    handle (e as Coa.OutEx s) => (print ("Error: " ^ s ^ "\n");
						  raise e)

    fun outJava pkg file =
	if String.extract(file, size(file)-4, NONE) <> ".rnc" then
	    print ("File name " ^ file ^ " does not end '.rnc'\n")
	else
	    case slurp file of
		NONE => print ("File " ^ file ^ ": Syntax Error\n")
	      | SOME (g as Rng.Top(d,p)) =>
		let 
		    val root = Util.cap (substring(file, 0, size(file) - 4))
		    val ds = d @ (Transform.declPatt p)
		    val patt = Transform.simplify g
		    fun pairify (Rng.Namespace(p,u)) = (p,u)
		      | pairify (Rng.Datatype(p,u)) = (p,u)
		    val cacheFile = root ^ "CachePair.java"
		    val cacheOut = TextIO.openOut cacheFile
		    val eoaFile = root ^ "EoaMachine.java"
		    val eoaOut = TextIO.openOut eoaFile
		    val doaFile = root ^ "DoaMachine.java"
		    val doaOut = TextIO.openOut doaFile
		    val decs = Util.uniquify (map pairify ds)
		    val names =
			Util.uniquify (Transform.namePatt patt)
		    val values =
			Util.uniquify (Transform.valuePatt decs patt)
		    val (eoaStart,eoaEdges) = Coa.buildEoa decs patt
		    val (doaStart,doaEdges) = Coa.buildDoa decs patt
		in
		    (Jcoa.outCache cacheOut pkg root (decs,names,values);
		     Jcoa.outEoa eoaOut pkg root (eoaStart,eoaEdges);
		     Jcoa.outDoa doaOut pkg root (doaStart,doaEdges))
		end
		    handle (e as Coa.OutEx s) => (print ("Error: " ^ s ^ "\n");
						  raise e)

in

fun invoke n [] = print ("Usage: " ^ n ^ " <command> <files...>\n")
  | invoke n ("help"::_) = (printHelp n; OS.Process.exit OS.Process.success)
  | invoke _ ("print"::fs) = app (fn s => (printFile s; print "\n")) fs
  | invoke _ ("raw"::fs) = app (fn s => (printRaw s; print "\n")) fs
  | invoke _ ("defs"::fs) = app (fn s => checkDef s) fs
  | invoke _ ("out"::fs) = app (fn s => outCoa s) fs
  | invoke _ ("java"::p::fs) = app (fn s => outJava p s) fs
  | invoke n (f::_) = (print (n ^ ": Unknown command " ^ f ^ "\n");
		       OS.Process.exit OS.Process.failure)

end (* local *)

end (* struct Main *)

val _ = Main.invoke (CommandLine.name()) (CommandLine.arguments())
