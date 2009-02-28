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
 * Stream: type and functions for stream (i.e. lazy list) processing
 *)
structure Stream = struct

(*
 * The type is made explicitly visible here.  The caller is always
 * responsible for creating the function for the tail.  This is not
 * truly lazy, since that would require references; see Paulson's
 * _ML for the Working Programmer_ for a better implementation of
 * streams.
 *)
datatype 'a stream = Nil | Cons of 'a * (unit -> 'a stream)

exception Null;

(*
 * empty: unit -> 'a stream
 * Return an empty stream
 *)
fun empty () = Nil

(*
 * single: 'a -> 'a stream
 * Return a stream consisting only of the argument as an element
 *)
fun single x = Cons(x,empty)

(*
 * head: 'a stream -> 'a
 * Return the first element of the argument stream
 *)
fun head Nil = raise Null
  | head (Cons(a,_)) = a

(*
 * tail: 'a stream -> 'a stream
 * Return the tail (i.e. without the first element) of the argument stream
 *)
fun tail Nil = raise Null
  | tail (Cons(_,f)) = f()

(*
 * toList: 'a stream -> 'a list
 * Convert the argument stream into a list
 *)
fun toList Nil = []
  | toList (Cons(a,f)) = a::(toList (f()))

(*
 * fromList: 'a list -> 'a stream
 * Convert the argument list into a stream
 *)
fun fromList [] = Nil
  | fromList (x::xs) = Cons(x,fn () => fromList xs)

(*
 * cons: 'a * 'a stream -> 'a stream
 * Prepend an element onto an existing stream
 *)
fun cons (a,b) = Cons(a, fn () => b)

(*
 * map: ('a -> 'b) -> 'a stream -> 'b stream
 * Map each element of the argument stream with the argument function
 *)
fun map f Nil = Nil
  | map f (Cons(a,g)) = Cons(f a, fn () => (map f (g ())))

(*
 * filter: ('a -> bool) -> 'a stream -> 'a stream
 * Return a stream consisting only of those elements of the argument
 * stream matching the argument predicate
 *)
fun filter f Nil = Nil
  | filter f (Cons(a,g)) =
    if f a then 
	Cons(a, fn () => (filter f (g())))
    else 
	filter f (g())

(*
 * append: 'a stream * 'a stream -> 'a stream
 * Concatenate the two argument streams
 *)
fun append (Nil, b) = b
  | append (a, Nil) = a
  | append (Cons(a,f), b) =
    let
	fun tack f () =
	    case (f ())
	     of Nil => b
	      | Cons(c,h) => Cons(c,tack h)
    in
	Cons(a, tack f)
    end

(*
 * concat: 'a stream stream -> stream
 * Take a stream of streams and concatenate them together
 *)
fun concat Nil = Nil
  | concat (Cons(Nil,s)) = concat (s())
  | concat (Cons(Cons(a,f),s)) =
    let
	fun tack f () =
	    case (f ())
	     of Nil => concat (s())
	      | Cons(b,g) => Cons(b,tack g)
    in
	Cons(a, tack f)
    end

end (* struct Stream *)
