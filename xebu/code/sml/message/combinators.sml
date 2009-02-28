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
 * Parser: some general parser combinators for use in SML programs
 *)
(*
 * This stuff is grabbed from "Functional Parsers" by Jeroen Fokker in
 * _Advanced Functional Programming_, Lecture Notes in Computer Science
 * 925, Springer-Verlag.
 *)
infixr 6 <*> <* *>
infix 5 <@
infixr 4 <|>

structure Parser = struct

(*
 * A parser returns a stream of results, each consisting of a value of
 * the parsed type along with the strings in the parsed token list that
 * were not included in constructing the value.  A parser is said to
 * return the parsed value.
 *)
type 'a result = (string list * 'a) Stream.stream;
type 'a parser = string list -> 'a result;

(*
 * If the next token in the list matches the argument, return the token
 *)
fun token a =
    (fn [] => Stream.Nil
      | t::ts => if a <> t then Stream.Nil
		 else Stream.single(ts,a))
    : string parser

(*
 * Always return the argument value without consuming tokens
 *)
fun succeed v =
    (fn s => Stream.single(s,v))
    : 'a parser

(*
 * Always fail without consuming tokens
 *)
val fail =
    (fn s => Stream.Nil)
    : 'a parser

(*
 * Parse two values in succession and return them as a pair
 *)
fun (p1: 'a parser) <*> (p2: 'b parser) 
  = (fn s => 
	Stream.concat
	    (Stream.map (fn (s1, v1) =>
			    (Stream.map (fn (s2,v2) => (s2,(v1,v2))) (p2 s1)))
	     (p1 s)))
    : ('a * 'b) parser;

(*
 * Return all parses that match either the left or the right argument
 *)
fun (p1: 'a parser) <|> (p2: 'a parser) =
    (fn s => Stream.append((p1 s),(p2 s)))
    : 'a parser;

(*
 * Take the parse of the argument parser and apply the argument function
 * to it, returning the mapped value
 *)
fun (p: 'a parser) <@ (f: 'a -> 'b) =
    (fn s => Stream.map (fn (s1, v1) => (s1, f v1)) (p s))
    : 'b parser;

(*
 * Parse as <*> but only return the left member of the pair
 *)
fun (p: 'a parser) <* (q: 'b parser) =
    p <*> q <@ #1

(*
 * Parse as <*> but only return the right member of the pair
 *)
fun (p: 'a parser) *> (q: 'b parser) =
    p <*> q <@ #2

(*
 * manymany: 'a list parser -> 'a list parser
 * Take a parser returning a list as an argument, parse several consecutive
 * such cases and return the concatenation of all the result lists
 *)
fun manymany p ts =
    (p <*> manymany p <@ (op @) <|> succeed []) ts

(*
 * many: 'a parser -> 'a list parser
 * Parse several consecutive cases of the argument parser and return their
 * return values as a list
 *)
fun many p ts =
    (p <*> many p <@ (op ::) <|> succeed []) ts

(*
 * several: 'a parser -> 'a list parser
 * Parse at least one and possibly more cases of the argument parser and
 * return the return values as a list
 *)
fun several p ts =
    (p <*> many p <@ (op ::)) ts

(*
 * listOf: 'a parser -> 'b parser -> 'a list parser
 * Parse a list of argument parser cases separated by the argument s and
 * return the values as a list
 *)
fun listOf p s ts =
    (p <*> many (s *> p) <@ (op ::)) ts

end
