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

structure OrlEx = struct

local

    open Orl

in

fun clnames (Top(pk,_,_,ds)) =
    let
	val pkpre = if size(pk) > 0 then pk ^ "." else ""
	fun gather [] l = l
	  | gather (Namespace _::ds) l = gather ds l
	  | gather (Type(n,_)::ds) l = gather ds ((n,pkpre ^ (Util.cap n))::l)
    in
	gather ds []
    end

fun nsnames (Top(_,_,_,ds)) =
    let
	fun gather _ [] l = l
	  | gather _ (Namespace(_,u)::ds) l = gather u ds l
	  | gather u (Type(n,_)::ds) l = gather u ds ((n,u)::l)
    in
	gather "http://www.hiit.fi/fuego/fc/xas" ds []
    end

fun decsplit l =
    let
	fun decsplit' [] lp = lp
	  | decsplit' ((d as Namespace x)::ds) (l1,l2) =
	    decsplit' ds (x::l1,l2)
	  | decsplit' ((d as Type x)::ds) (l1,l2) = decsplit' ds (l1,x::l2)
    in
	decsplit' l ([],[])
    end

end (* local *)

end (* struct OrlEx *)
