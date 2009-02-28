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
 * Rng: datatype and parser for RELAX NG
 *)
structure Rng = struct

(*
 * Declare type names for these so they can be distinguished in mappings
 *)
type uri = string
type prefix = string
type literal = string

(*
 * An XML namespace, either as a prefix or (after prefix mapping has been
 * done) a URI
 *)
datatype namespace = Prefix of prefix
		   | Uri of uri

(*
 * A qualified name, may contain wildcards
 *)
datatype cname = Cname of namespace * string

(*
 * A RELAX NG name, either simple name or widlcarded name with exceptions
 * or a list of alternates
 *)
datatype nameclass = Name of cname
		   | Except of cname * nameclass
		   | List of nameclass list

(*
 * A datatype parameter
 *)
type param = string * literal

(*
 * Possible cases of typed data
 *)
datatype data = DataName of cname
	      | DataValue of string
	      | NameValue of cname * string
	      | ParamName of cname * param list

(*
 * The assignment method of name definitions
 *)
datatype method = Assign | Or | And

(*
 * The pattern and content types, as far as is reasonable here
 *)
datatype pattern = Element of nameclass * pattern
		 | Attribute of nameclass * pattern
		 | Group of pattern list
		 | Interleave of pattern list
		 | Choice of pattern list
		 | Question of pattern
		 | Star of pattern
		 | Plus of pattern
		 | Id of string
		 | Parent of string
		 | Empty
		 | Text
		 | Mixed of pattern
		 | Data of data
		 | Plist of pattern
		 | NotAllowed
		 | External of uri
		 | Grammar of content list
and content = Start of method * pattern
  | Define of string * method * pattern
  | Divv of content list
  | Incl of uri * content list

(*
 * A namespace or a datatype prefix declaration
 *)
datatype decl = Namespace of prefix * uri
	      | Datatype of prefix * uri

(*
 * A RELAX NG file
 *)
datatype top = Top of decl list * pattern

local
    open Parser

    val keywords = ["attribute","default","datatypes","div","element","empty",
		    "external","grammar","include","inherit","list","mixed",
		    "namespace","notAllowed","parent","start","string","text",
		    "token"]

    datatype suffix = Q | S | P | E

    datatype datafollow = DV of literal
			| DP of param list
			| DE

    datatype namelist = ON of nameclass list
		      | EN

    datatype nameexcept = MN of nameclass
			| NN

    datatype sequence = G of pattern list
		      | I of pattern list
		      | C of pattern list
		      | D

    fun isKeyword s = Util.exists s keywords

    (*
     * Parse a literal segment
     *)
    fun segment [] = Stream.Nil
      | segment (t::ts) =
	if size(t) > 0 andalso String.sub(t,0) = #"\"" then
	    Stream.single(ts,String.extract(t,1,NONE))
	else Stream.Nil

    (*
     * Parse a literal
     *)
    val literal = listOf segment (token "~") <@ String.concat

    (*
     * Parse a simple name
     *)
    fun ncname [] = Stream.Nil
      | ncname (t::ts) =
	if size(t) > 0 andalso Char.isAlpha(String.sub(t, 0)) then
	    Stream.single(ts, t)
	else Stream.Nil

    (*
     * Parse a keyword
     *)
    fun keyword [] = Stream.Nil
      | keyword (t::ts) =
	if isKeyword t then Stream.single(ts, t)
	else Stream.Nil

    (*
     * Parse a string that is not a keyword
     *)
    fun nonKeyword [] = Stream.Nil
      | nonKeyword (t::ts) =
	if isKeyword t then Stream.Nil
	else Stream.single(ts, t)

    (*
     * Parse an identifier, either a non-keyword string or an escaped
     * keyword
     *)
    fun identifier ts =
	(nonKeyword
	     <|> token "\\" *> ncname
	 ) ts

    val idOrKey = keyword <|> identifier

    (*
     * Parse a qualified name
     *)
    fun cname ts =
	(ncname <*> token ":" *> ncname <@ (fn (p,n) => Cname(Prefix p,n))) ts

    (*
     * Parse a general name, either qualified or unqualified,
     * with or without wildcards
     *)
    fun name ts =
	(cname
	     <|> idOrKey <@ (fn x => Cname(Prefix "",x))
	 ) ts
    fun nsname ts =
	(ncname <* token ":" <* token "*" <@ (fn x => Cname(Prefix x,"*"))) ts
    fun anyname ts =
	(token "*" *> succeed (Cname(Prefix "*","*"))) ts

    (*
     * Two utility functions to combine the result of general name parsing
     *)
    fun combinesimple (n,MN(m)) = Except(n,m)
      | combinesimple (n,NN) = Name(n)
    fun combinename (n,ON(l)) = List(n::l)
      | combinename (n,EN) = n

    (*
     * Parse a full RELAX NG name by first splitting the name into
     * a list of |-separated simpler names and then parsing each for
     * exceptions
     *)
    fun simplenameClass' ts =
	(token "-" *> nameClass <@ MN
	       <|> succeed NN
	 ) ts
    and simplenameClass ts =
	(((anyname <|> nsname) <*> simplenameClass' <@ combinesimple)
	     <|> (token "(" *> nameClass <* token ")")
	     <|> (name <@ Name)
	 ) ts
    and nameClass' ts =
	(several (token "|" *> simplenameClass) <@ ON
		 <|> succeed EN
	 ) ts
    and nameClass ts =
	(simplenameClass <*> nameClass' <@ combinename) ts

    (*
     * Parse an assignment operator
     *)
    fun assign ts =
	(token "=" *> succeed Assign
	       <|> token "|" *> token "=" *> succeed Or
	       <|> token "&" *> token "=" *> succeed And
	 ) ts

    (*
     * Parse a datatype name, either a predefined one or a qualified name
     *)
    fun datatypeName ts =
	(token "string" *> succeed (Cname(Prefix "*","string"))
	       <|> token "token" *> succeed (Cname(Prefix "*","token"))
	       <|> cname
	       ) ts

    (*
     * Three utility functions for combining the results of pattern parsing
     *)
    fun combineSuffix (p,Q) = Question p
      | combineSuffix (p,S) = Star p
      | combineSuffix (p,P) = Plus p
      | combineSuffix (p,E) = p
    fun combineSequence (p,G(l)) = Group(p::l)
      | combineSequence (p,C(l)) = Choice(p::l)
      | combineSequence (p,I(l)) = Interleave(p::l)
      | combineSequence (p,D) = p
    fun combineData (n,DV(v)) = NameValue(n,v)
      | combineData (n,DP(l)) = ParamName(n,l)
      | combineData (n,DE) = DataName(n)

    val param = (idOrKey <* token "=") <*> literal

    fun data' ts =
	(literal <@ DV
		 <|> token "{" *> many param <* token "}" <@ DP
		 <|> succeed DE
	) ts
    fun data ts =
	(datatypeName <*> data' <@ combineData) ts

    (*
     * Parse a pattern or content.  The main pattern function is patt
     * and pattern parsing is split into three layers: on the highest
     * layer a list of separated patterns is parsed, on the second layer
     * a possible suffix for a pattern is parsed, and on the lowest
     * layer patterns not starting recursively with a pattern are parsed.
     * The main content function is content.
     *)
    fun simplePatt ts =
	((token "attribute" *> nameClass <*> (token "{" *> patt <* token "}")
		<@ Attribute)
	     <|> (token "element" *> nameClass <*> (token "{" *> patt
							  <* token "}")
			<@ Element)
	     <|> (token "list" *> (token "{" *> patt <* token "}") <@ Plist)
	     <|> (token "empty" *> succeed Empty)
	     <|> (token "text" *> succeed Text)
	     <|> (token "mixed" *> (token "{" *> patt <* token "}") <@ Mixed)
	     <|> (token "notAllowed" *> succeed NotAllowed)
	     <|> (token "parent" *> identifier <@ Parent)
	     <|> (token "external" *> literal <@ External)
	     <|> (token "grammar" *> token "{" *> many content <* token "}"
			<@ Grammar)
	     <|> (token "(" *> patt <* token ")")
	     <|> (literal <@ (Data o DataValue))
	     <|> (data <@ Data)
	     <|> (identifier <@ Id)
	 ) ts
    and suffixPatt' ts =
	(token "?" *> succeed Q
	       <|> token "*" *> succeed S
	       <|> token "+" *> succeed P
	       <|> succeed E
	 ) ts
    and suffixPatt ts =
	(simplePatt <*> suffixPatt' <@ combineSuffix) ts
    and patt' ts =
	(several (token "," *> suffixPatt) <@ G
		 <|> several (token "|" *> suffixPatt) <@ C
		 <|> several (token "&" *> suffixPatt) <@ I
		 <|> succeed D
	 ) ts
    and patt ts =
	(suffixPatt <*> patt' <@ combineSequence) ts
    and start ts =
	(token "start" *> assign <*> patt <@ Start) ts
    and divv ts =
	(token "div" *> token "{" *> many content <* token "}" <@ Divv) ts
    and incl' ts =
	(token "{" *> many icontent <* token "}"
	       <|> succeed []) ts
    and incl ts =
	(token "include" *> literal <*> incl' <@ Incl) ts
    and define ts =
	(identifier <*> assign <*> patt <@ (fn (x,(y,z)) => Define(x,y,z))) ts
    and content ts =
	(start <|> divv <|> incl <|> define) ts
    and icontent ts =
	(start <|> divv <|> define) ts

    (*
     * Parse a declaration.  The purpose of defdecl is to return a
     * stream consisting of two declarations in the case of a named
     * declaration being also defined as default.
     *)
    fun defdecl ts =
	((token "=" *> literal
		<@ (fn x => Stream.single(Namespace("",x))))
	     <|> (keyword <|> identifier) <*> token "=" *> literal
	     <@ (fn (x,y) =>
		    Stream.Cons(Namespace(x,y),
				fn () => Stream.single(Namespace("",y))))
	 ) ts
    fun decl ts =
	(((token "namespace" *> identifier <*> token "=" *> literal
		 <@ (Stream.single o Namespace)
		 <|> token "default" *> token "namespace" *> defdecl)
	      <@ Stream.toList)
	     <|> (token "datatypes" *> identifier <*> token "=" *> literal
			<@ ((fn x => [x]) o Datatype))
	 ) ts

    (*
     * Parse a RELAX NG file
     *)
    fun topLevel ts =
	(manymany decl <*> (patt <|> (many content <@ Grammar))
		  <@ Top
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

(*
 * parseTop: string -> top option
 * Parse a RELAX NG file
 *)
fun parseTop s = parse s topLevel

(*
 * parsePattern: string -> pattern option
 * Parse a RELAX NG pattern
 *)
fun parsePattern s = parse s patt

end (* local *)

end (* struct Rng *)
