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
 * Print: functions for printing various COA processor types
 * Each output function takes a stream to output into as first argument.
 * The print functions do the same as the corresponding output functions
 * with the stream set to standard output.
 *)
structure Print = struct

local
    open Coa
    open Rng
    open TextIO
in

(*
 * outputList: outstream -> (outstream -> 'a -> unit) -> vector ->
 *             'a list -> unit
 * Output the last argument list into the stream sm outputting each
 * element using the function p and outputting sep as a separator
 * between each two elements
 *)
fun outputList _ _ _ [] = ()
  | outputList sm p _ [n] = p sm n
  | outputList sm p sep (n::ns) = (p sm n; output(sm, sep);
				   outputList sm p sep ns)
fun printList sep = outputList stdOut (fn s => (fn x => output(s,x))) sep

(*
 * outputLiteral: outstream -> vector -> unit
 * Output the value l surrounded by double quotes
 *)
fun outputLiteral sm l = (output(sm,"\""); output(sm, l); output(sm,"\""))
val printLiteral = outputLiteral stdOut

(*
 * outputCname: outstream -> cname -> unit
 * Output the qualified name using either the prefix:local or
 * {uri}local notation
 *)
fun outputCname sm (Cname(Prefix "",n)) = output(sm, n)
  | outputCname sm (Cname(Prefix "*","*")) = output(sm, "*")
  | outputCname sm (Cname(Prefix "*",n)) = (output(sm, "*:"); output(sm, n))
  | outputCname sm (Cname(Prefix p,n)) = (output(sm, p); output(sm, ":");
					  output(sm, n))
  | outputCname sm (Cname(Uri u,n)) = (output(sm, "{"); output(sm, u);
				       output(sm, "}"); output(sm, n))

(*
 * outputName: outstream -> nameclass -> unit
 * Output the full name in RELAX NG parsable notation
 *)
fun outputName sm (Name(n)) = outputCname sm n
  | outputName sm (Except(n,m)) = (output(sm, "("); outputCname sm n;
				   output(sm, " - "); outputName sm m;
				   output(sm, ")"))
  | outputName sm (List(l)) = (output(sm, "(");
			       outputList sm outputName " | " l;
			       output(sm, ")"))

fun outputParam sm (s,l) = (output(sm, s); output(sm, "="); outputLiteral sm l)

fun outputData sm (DataName n) = outputCname sm n
  | outputData sm (DataValue s) = outputLiteral sm s
  | outputData sm (NameValue(n,s)) = (outputCname sm n; output(sm, " ");
				      outputLiteral sm s)
  | outputData sm (ParamName(n,ps)) = (outputCname sm n; output(sm, " { ");
				       outputList sm outputParam " " ps;
				       output(sm, "}"))

fun outputMethod sm Assign = output(sm, " = ")
  | outputMethod sm Or = output(sm, " |= ")
  | outputMethod sm And = output(sm, " &= ")

(*
 * outputPattern: outstream -> pattern -> unit
 * outputContent: outstream -> content -> unit
 * Output the pattern or content trying to do some primitive
 * prettyprinting.  The result is not fully parsable back as RELAX NG.
 *)
fun outputPattern sm (Element(n,p)) = (output(sm, "element "); outputName sm n;
				       output(sm, " {\n"); outputPattern sm p;
				       output(sm, "\n}\n"))
  | outputPattern sm (Attribute(n,p)) = (output(sm, "attribute ");
					 outputName sm n; output(sm, " { ");
					 outputPattern sm p; output(sm, " }"))
  | outputPattern sm (Group(l)) = (output(sm, "(");
				   outputList sm outputPattern ", " l;
				   output(sm, ")"))
  | outputPattern sm (Interleave(l)) = (output(sm, "(");
					outputList sm outputPattern " & " l;
					output(sm, ")"))
  | outputPattern sm (Choice(l)) = (output(sm, "(");
				    outputList sm outputPattern " | " l;
				    output(sm, ")"))
  | outputPattern sm (Question(p)) = (outputPattern sm p; output(sm, "?"))
  | outputPattern sm (Star(p)) = (outputPattern sm p; output(sm, "*"))
  | outputPattern sm (Plus(p)) = (outputPattern sm p; output(sm, "+"))
  | outputPattern sm (Id(n)) = output(sm, n)
  | outputPattern sm (Parent(n)) = (output(sm, "parent "); output(sm, n))
  | outputPattern sm Empty = output(sm, "empty")
  | outputPattern sm Text = output(sm, "text")
  | outputPattern sm (Mixed p) = (output(sm, "mixed { "); outputPattern sm p;
				  output(sm, " }"))
  | outputPattern sm (Data(d)) = outputData sm d
  | outputPattern sm (Plist(p)) = (output(sm, "list { "); outputPattern sm p;
				   output(sm, " }"))
  | outputPattern sm NotAllowed = output(sm, "notAllowed")
  | outputPattern sm (External(u)) = (output(sm, "External ");
				      outputLiteral sm u)
  | outputPattern sm (Grammar(l)) = (output(sm, "Grammar\n");
				     app (outputContent sm) l)
and outputContent sm (Start(m,p)) = (output(sm, "start"); outputMethod sm m;
				     outputPattern sm p; output(sm, "\n"))
  | outputContent sm (Define(n,m,p)) = (output(sm, n); outputMethod sm m;
					outputPattern sm p; output(sm, "\n"))
  | outputContent sm (Divv(l)) = (output(sm, "div {\n");
				  app (outputContent sm) l; output(sm, "}\n"))
  | outputContent sm (Incl(u,l)) = (output(sm, "include "); outputLiteral sm u;
				    output(sm, " {\n");
				    app (outputContent sm) l;
				    output(sm, "}\n"))
val printPattern = outputPattern stdOut

fun outputDecl sm (Namespace("",u)) = (output(sm, "default namespace = ");
				       outputLiteral sm u; output(sm, "\n"))
  | outputDecl sm (Namespace(p,u)) = (output(sm, "namespace "); output(sm, p);
				      output(sm, " = "); outputLiteral sm u;
				      output(sm, "\n"))
  | outputDecl sm (Datatype(p,u)) = (output(sm, "datatypes "); output(sm, p);
				     output(sm, " = "); outputLiteral sm u;
				     output(sm, "\n"))

fun outputTop sm (Top(l,p)) = (app (outputDecl sm) l; output(sm, "\n");
			       outputPattern sm p)
val printTop = outputTop stdOut

(*
 * outputEvent: outstream -> event -> unit
 * Output a XAS event in the standard XAS style
 *)
fun outputEvent sm StartDocument = output(sm, "SD()")
  | outputEvent sm EndDocument = output(sm, "ED()")
  | outputEvent sm (StartElement(u,n)) = (output(sm, "SE("); output(sm, u);
					  output(sm, " "); output(sm, n);
					  output(sm, ")"))
  | outputEvent sm (Attrib(u,n,v)) = (output(sm, "A("); output(sm, u);
				      output(sm, " "); output(sm, n);
				      output(sm, " "); output(sm, v);
				      output(sm, ")"))
  | outputEvent sm (EndElement(u,n)) = (output(sm, "EE("); output(sm, u);
					output(sm, " "); output(sm, n);
					output(sm, ")"))
  | outputEvent sm (Content(t)) = (output(sm, "C("); output(sm, t);
				   output(sm, ")"))
  | outputEvent sm (TypedContent(u,n)) = (output(sm, "TC("); output(sm, u);
					  output(sm, " "); output(sm, n);
					  output(sm, ")"))
  | outputEvent sm (NamespacePrefix(u,p)) = (output(sm, "NP("); output(sm, u);
					     output(sm, " "); output(sm, p);
					     output(sm, ")"))
  | outputEvent sm Any = output(sm, "AN()")

fun outputEoaKind sm Del = output(sm, "<del>")
  | outputEoaKind sm Out = output(sm, "<out>")

fun outputDoaKind sm Read = output(sm, "<read>")
  | outputDoaKind sm Peek = output(sm, "<peek>")
  | outputDoaKind sm Promise = output(sm, "<promise>")
  | outputDoaKind sm Null = output(sm, "<null>")

fun outputEoaEdge sm (s,{event,kind},t) =
    (output(sm, Int.toString(s)); output(sm, ":"); outputEoaKind sm kind;
     output(sm, " "); outputEvent sm event; output(sm, ":");
     output(sm, Int.toString(t)))
val printEoaEdge = outputEoaEdge stdOut

fun outputDoaEdge sm (s,{event,kind,pushed,queued},t) =
    (output(sm, Int.toString(s)); output(sm, ":"); outputDoaKind sm kind;
     output(sm, " "); outputEvent sm event; output(sm, " [");
     outputList sm outputEvent " " pushed; output(sm, "] [");
     outputList sm outputEvent " " queued; output(sm, "]:");
     output(sm, Int.toString(t)))
val printDoaEdge = outputDoaEdge stdOut

end (* local *)

end (* struct Print *)
