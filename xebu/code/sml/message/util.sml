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
 * Util: a collection of general-purpose functions
 *)
structure Util = struct 

(*
 * cap: string -> string
 * Capitalize the first letter of the argument string
 *)
fun cap s =
    case explode s of
	[] => ""
      | (c::cs) => implode (Char.toUpper c :: cs)

(*
 * assocl: (''a * 'b) list -> ''a -> 'b option
 * Find first appearance in list of a pair with left member given and
 * return right member, NONE if no pair is found
 *)
fun assocl [] s = NONE
  | assocl ((k,v)::ls) s =
    if s = k then SOME v
    else assocl ls s

(*
 * assocr: ('b * ''a) list -> ''a -> 'b option
 * Find first appearance in list of a pair with right member given and
 * return left member, NONE if no pair is found
 *)
fun assocr [] s = NONE
  | assocr ((v,k)::ls) s =
    if s = k then SOME v
    else assocr ls s

(*
 * const: 'a -> 'b -> 'a
 * The K combinator, with argument x returns a function that discards
 * its argument and returns x
 *)
fun const x y = x

(*
 * id: 'a -> 'a
 * The I combinator, returns its argument
 *)
fun id x = x

(*
 * exists: ''a -> ''a list -> bool
 * Check whether first argument is equal to some element of the list
 *)
fun exists x [] = false
  | exists x (y::ys) = x = y orelse exists x ys

(*
 * forAll: ''a -> ''a list -> bool
 * Check whether first argument is equal to all elements of the list
 *)
fun forAll x [] = true
  | forAll x (y::ys) = x = y andalso forAll x ys

(*
 * delete: ''a -> ''a list -> ''a list
 * Return the argument list with all occurrences of the first argument
 * deleted
 *)
fun delete x [] = []
  | delete x (y::ys) = if x = y then delete x ys else y::(delete x ys)

(*
 * uniquify: ''a list -> ''a list
 * Return the argument list with all duplicate elements reduced to
 * their first appearance
 *)
fun uniquify [] = []
  | uniquify (x::xs) = x::(delete x (uniquify xs))

(*
 * intersect: ''a list -> ''a list -> ''a list
 * Compute the intersection of two lists
 *)
fun intersect xs ys = List.filter (fn x => exists x ys) xs

end (* struct Util *)
