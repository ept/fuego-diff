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
 * Unf: type and functions for a Union-find data structure
 *)
structure Unf = struct

fun find x l =
    if x < length l then
	let
	    val y = List.nth(l,x)
	in
	    if x <> y then find y l else x
	end
    else x

fun add (x,y) l =
    let
	val x' = find x l
	val y' = find y l
	fun update 0 l x [] = [x]
	  | update 0 l x (z::zs) = x::zs
	  | update n l x [] = l::(update (n-1) (l+1) x [])
	  | update n l x (z::zs) = z::(update (n-1) (l+1) x zs)
    in
	if x' < y'
	then update y' 0 x' l
	else if x' > y'
	then update x' 0 y' l
	else l
    end

end (* struct Unf *)
