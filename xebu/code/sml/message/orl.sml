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
 * Orl: datatype and parser for ORL
 *)
structure Orl = struct

type uri = string
type prefix = string
type pkg = string
type name = string

datatype tname = Simple of name
	       | Optional of name
	       | List of name

type pair = tname * string

datatype decl = Type of name * pair list
	      | Namespace of prefix * uri

datatype top = Top of pkg * pkg * string * decl list

local

    open Parser

    datatype tsuffix = S | O | L

    fun literal [] = Stream.Nil
      | literal (t::ts) =
	if size(t) > 0 andalso String.sub(t,0) = #"\"" then
	    Stream.single(ts,String.extract(t,1,NONE))
	else Stream.Nil

    fun name [] = Stream.Nil
      | name (t::ts) =
	if size(t) > 0 andalso Char.isAlpha(String.sub(t,0)) then
	    Stream.single(ts,t)
	else Stream.Nil

    fun tqual ts =
	(token "?" *> succeed O
	       <|> token "*" *> succeed L
	       <|> succeed S
	 ) ts

    fun combineType (p,S) = Simple p
      | combineType (p,O) = Optional p
      | combineType (p,L) = List p

    fun tname ts =
	(name <*> tqual <@ combineType) ts

    fun pair ts =
	(tname <*> name) ts

    fun declare ts =
	((token "namespace" *> name <*> literal <@ Namespace)
	     <|> (token "type" *> name <*> (token "{" *> many pair
						  <* token "}")
			<@ Type)
	 ) ts

    fun topLevel ts =
	((token "package" *> name <|> succeed "")
	     <*> (token "codec-package" *> name <|> succeed "")
	     <*> (token "codec-name" *> name <|> succeed "")
	     <*> many declare <@ (fn (x,(y,(z,w))) => (x,y,z,w)) <@ Top
	 ) ts

in

(*
 * parses: string -> 'a parser -> 'a stream
 * Return a stream of all full parses of the string s parsed
 * according to the function f (normally either patt or topLevel)
 *)
fun parses s (f: 'a parser) =
    Stream.map (fn (_,v) => v)
	       (Stream.filter (fn (s,_) => length(s) = 0)
			      (f (Token.tokenize s)))

(*
 * parse: string -> 'a parser -> 'a option
 * Return the first full parse of the string s parsed according to
 * the function f (normally either patt or topLevel)
 *)
fun parse s (f: 'a parser) =
    case parses s f
     of Stream.Nil => NONE
      | Stream.Cons(a,f) => SOME a

fun parseTop s = parse s topLevel

end (* local *)

end (* struct Orl *)
