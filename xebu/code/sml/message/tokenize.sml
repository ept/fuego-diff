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
 * Token: tokenization for RELAX NG
 *)
structure Token = struct

local

    fun isNameStartChar c =
	Char.isAlpha c orelse c = #"_"

    fun isNameChar c =
	isNameStartChar c orelse c = #"-" orelse c = #"." orelse Char.isDigit c

    (*
     * readWhile: (char -> bool) -> char list -> (string * char list)
     * Read characters from the argument list as long as they satisfy the
     * argument predicate and return the string formed from these characters
     * along with the part of the argument list that was not read
     *)
    fun readWhile f s =
	let 
	    fun part l [] = ((implode(rev l)),[])
	      | part l (s as c::cs) =
		if f c then 
		    part (c::l) cs
		else 
		    ((implode(rev l)),s)
	in 
	    part [] s
	end

    (*
     * skipComment: char list -> char list
     * Skip characters in the argument list until and including the next
     * newline and return the list of unskipped characters
     *)
    fun skipComment [] = []
      | skipComment (c::cs) =
	if c = #"\n" then cs else skipComment cs

    fun tok toks [] = rev toks
      | tok toks (s as c::cs) =
	if isNameStartChar c then (* name *)
	    let 
		val (token, ss) = readWhile isNameChar s
	    in 
		tok (token::toks) ss
	    end
	else if Char.isSpace c then (* whitespace *)
	    tok toks cs
	else if c = #"#" then (* comment *)
	    tok toks (skipComment cs)
	else if c = #"\"" then (* literal *)
	    let
		val (token, ss) = readWhile (fn c => c <> #"\"") (tl s)
	    in
		tok ("\"" ^ token::toks) (tl ss)
	    end
	else (* other *)
	    tok ((str c)::toks) cs

in

(*
 * tokenize: string -> string list
 * Tokenize the argument string into tokens defined by RELAX NG
 *)
fun tokenize s = tok [] (explode s)

end (* local *)

end (* struct Token *)
