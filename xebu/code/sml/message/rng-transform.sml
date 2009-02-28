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
 * Transform: an implementation of the RELAX NG simplification rules
 *)
structure Transform = struct

local

    open Rng

in

(*
 * This is the major type of this structure.  The idea of the folder
 * is that patterns in RELAX NG files map into the type 'a and contents
 * map into the type 'b.  This is the most general such folder (if you
 * want to get fancy, it's called a catamorphism), and more specific
 * folders are defined below.
 *)
type ('a, 'b) folder = { element: nameclass * 'a -> 'a,
			 attribute: nameclass * 'a -> 'a, group: 'a list -> 'a,
			 interleave: 'a list -> 'a, choice: 'a list -> 'a,
			 question: 'a -> 'a, star: 'a -> 'a, plus: 'a -> 'a,
			 id: string -> 'a, parent: string -> 'a, empty: 'a,
			 text: 'a, mixed: 'a -> 'a, data: data -> 'a,
			 plist: 'a -> 'a, notAllowed: 'a,
			 external: uri -> 'a, grammar: 'b list -> 'a,
			 start: method * 'a -> 'b,
			 define: string * method * 'a -> 'b,
			 divv: 'b list -> 'b, incl: uri * 'b list -> 'b }

(*
 * This is the type for transformers that transform a RELAX NG tree
 * into another RELAX NG tree.  This is normally the type to use for
 * simplification.
 *)
type transformer = (pattern, content) folder

(*
 * The identity transformer.  The idea is that most transformers can be
 * specified by just a few components, so the identity transformer is
 * provided as a base for these and update functions for each individual
 * folder component are then used to build the desired transformer on
 * top of this.  In fact, the update functions work for general folders
 * so it's possible to use them to construct other kinds of folders.
 *)
val idTransform = { element=Element, attribute=Attribute, group=Group,
		    interleave=Interleave, choice=Choice, question=Question,
		    star=Star, plus=Plus, id=Id, parent=Parent, empty=Empty,
		    text=Text, mixed=Mixed, data=Data, plist=Plist,
		    notAllowed=NotAllowed,
		    external=External, grammar=Grammar, start=Start,
		    define=Define, divv=Divv, incl=Incl }: transformer

fun updateElement e (f:('a, 'b) folder) =
    { element= e, attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateAttribute a (f:('a, 'b) folder) =
    { element= #element(f), attribute= a, group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateGroup g (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= g,
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateInterleave i (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= i, choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateChoice c (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= c, question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateQuestion q (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= q,
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateStar s (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= s, plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updatePlus p (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= p, id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateId i (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= i, parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateMixed m (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = m, data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateData d (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= d, plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateExternal e (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= e, grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateGrammar g (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= g,
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateStart s (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= s, define= #define(f), divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateDefine d (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= d, divv= #divv(f),
      incl= #incl(f) }: ('a, 'b) folder

fun updateDivv d (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= d,
      incl= #incl(f) }: ('a, 'b) folder

fun updateIncl i (f:('a, 'b) folder) =
    { element= #element(f), attribute= #attribute(f), group= #group(f),
      interleave= #interleave(f), choice= #choice(f), question= #question(f),
      star= #star(f), plus= #plus(f), id= #id(f), parent= #parent(f),
      empty= #empty(f), text= #text(f),
      mixed = #mixed(f), data= #data(f), plist= #plist(f),
      notAllowed= #notAllowed(f), external= #external(f), grammar= #grammar(f),
      start= #start(f), define= #define(f), divv= #divv(f),
      incl= i }: ('a, 'b) folder

(*
 * A function to use for updating an old-style data function to obey
 * the new-style type data definition
 *)
fun legacyData data (ParamName(n,_)) = data (DataName n)
  | legacyData data (NameValue(_,s)) = data (DataValue s)
  | legacyData data d = data d

(*
 * foldPatt: ('a, 'b) folder -> pattern -> 'a
 * foldCont: ('a, 'b) folder -> content -> 'b
 * A pair of mutually recursive functions that take a general folder
 * and fold the provided RELAX NG element according to that
 *)
fun foldPatt (f:('a, 'b) folder) (Element(n,p)) = #element(f)(n,foldPatt f p)
  | foldPatt f (Attribute(n,p)) = #attribute(f)(n,foldPatt f p)
  | foldPatt f (Group l) = #group(f)(map (foldPatt f) l)
  | foldPatt f (Interleave l) = #interleave(f)(map (foldPatt f) l)
  | foldPatt f (Choice l) = #choice(f)(map (foldPatt f) l)
  | foldPatt f (Question p) = #question(f)(foldPatt f p)
  | foldPatt f (Star p) = #star(f)(foldPatt f p)
  | foldPatt f (Plus p) = #plus(f)(foldPatt f p)
  | foldPatt f (Id s) = #id(f)(s)
  | foldPatt f (Parent s) = #parent(f)(s)
  | foldPatt f Empty = #empty(f)
  | foldPatt f Text = #text(f)
  | foldPatt f (Mixed p) = #mixed(f)(foldPatt f p)
  | foldPatt f (Data n) = #data(f)(n)
  | foldPatt f (Plist p) = #plist(f)(foldPatt f p)
  | foldPatt f NotAllowed = #notAllowed(f)
  | foldPatt f (External u) = #external(f)(u)
  | foldPatt f (Grammar l) = #grammar(f)(map (foldCont f) l)
and foldCont f (Start(m,p)) = #start(f)(m,foldPatt f p)
  | foldCont f (Define(s,m,p)) = #define(f)(s,m,foldPatt f p)
  | foldCont f (Divv l) = #divv(f)(map (foldCont f) l)
  | foldCont f (Incl(u,l)) = #incl(f)(u,map (foldCont f) l)

(*
 * The identity transformer for patterns
 *)
val idPatt = foldPatt idTransform

(*
 * A specific folder when pattern and content both map to the same type
 *)
type 'a builder = ('a, 'a) folder

(*
 * idBuilder: 'a -> ('a list -> 'a) -> 'a builder
 * Construct a builder out of a base value and a list constructor.  This
 * function is for the cases where a value is built recursively.  It needs
 * to know what value to give for the non-recursive nodes and how to
 * construct a value from a list of values.
 *)
fun idBuilder base build: 'a builder =
    { element= #2, attribute= #2, group=build, interleave=build,
      choice=build, question=Util.id, star=Util.id, plus=Util.id,
      id=Util.const base, parent=Util.const base, empty= base, text= base,
      mixed=Util.id, data=Util.const base, plist=Util.id, notAllowed=base,
      external=Util.const base, grammar=build, start= #2, define= #3,
      divv=build, incl=build o #2 }

(*
 * Type for builders that build lists
 *)
type 'a retriever = 'a list builder

(*
 * idRetriever: unit -> 'a retriever
 * Construct a builder where the base case is the empty list and the
 * way to construct from a list is to concatenate the lists.  This is
 * a function due to restrictions in polymorphic values.
 *)
fun idRetriever () = idBuilder nil List.concat

(*
 * replaceDecl: top -> pattern
 * Apply all namespace and datatype declarations in the argument top
 * file to its pattern and return the resulting pattern
 *)
fun replaceDecl t =
    let
	fun declReplace (nss,dts,p) =
	    let
		fun replaceCname (name as Cname(Prefix p,n)) f =
		    (case f p
		      of NONE => if p = "" then Cname(Uri "",n) else name
		       | SOME u => Cname(Uri u,n))
		  | replaceCname name _ = name
		fun replaceName (Name(c)) f = Name(replaceCname c f)
		  | replaceName (Except(n,m)) f = Except(replaceCname n f,
							 replaceName m f)
		  | replaceName (List(l)) f =
		    List(map (fn x => replaceName x f) l)
		fun elem (n,p) = Element(replaceName n (Util.assocl nss), p)
		fun attr (n,p) = Attribute(replaceName n (Util.assocl nss), p)
		fun data (DataName(c as Cname(Uri u,n))) =
		    Data(DataName(case Util.assocr dts u of
				      NONE => c
				    | SOME p => Cname(Prefix p,n)))
		  | data c = Data(c)
	    in 
		foldPatt (updateElement elem
			  (updateAttribute attr
			   (updateData data idTransform
			    ))) p
	    end
	fun declSplit (Top(ds,p)) =
	    let
		fun dnSplit [] nss dts = (nss,dts)
		  | dnSplit (Namespace(p,u)::ds) nss dts
		    = dnSplit ds ((p,u)::nss) dts
		  | dnSplit (Datatype(p,u)::ds) nss dts
		    = dnSplit ds nss ((p,u)::dts)
		val (nss,dts) = dnSplit ds [] []
	    in
		(nss,dts,p)
	    end
    in
	(declReplace o declSplit) t
    end

(*
 * extPatt: pattern -> pattern
 * Expand all include and external statements in the argument pattern
 *)
val rec extPatt = fn x =>
    let 
	fun parse s =
	    let val t = parseTop (TextIO.inputAll (TextIO.openIn s))
	    in 
		case t
		 of SOME g => (extPatt o replaceDecl) g
		  | NONE => Empty
	    end
	fun incl (Incl(u,cs)) =
	    let val p = parse u
	    in 
		case p
		 of Grammar(l) => l @ cs
		  | p => Start(Assign,p) :: cs
	    end
	  | incl x = [x]
	fun external u = parse u
	fun grammar l =	Grammar(List.concat (map incl l))
	fun divv l = Divv(List.concat (map incl l))
    in 
	foldPatt (updateExternal external
		  (updateGrammar grammar
		   (updateDivv divv idTransform
		    ))) x
    end

(*
 * removePatt: pattern -> pattern
 * Reduce the grouping and suffix patterns according to rules, remove
 * all mixed patterns by transforming to equivalent interleave patterns,
 * and remove all div contents by collapsing them
 *)
val removePatt =
    let
	fun incl (u,l) = Incl(u, reduce l)
	and reduce [] = []
	  | reduce (Divv(l)::cs) = reduce(l @ cs)
	  | reduce (c::cs) = (removeCont c)::(reduce cs)
	and removeCont c = foldCont (updateIncl incl idTransform) c
	fun group [p] = p
	  | group l = Group(l)
	fun interleave [p] = p
	  | interleave l = Interleave(l)
	fun choice [p] = p
	  | choice l = Choice(l)
	fun plus p = Group([p, Star p])
	fun mixed p = Interleave([Text, p])
	fun grammar l = Grammar(reduce l)
    in
	foldPatt (updateGroup group
		  (updateInterleave interleave
		   (updateChoice choice
		    (updatePlus plus
		     (updateMixed mixed
		      (updateGrammar grammar idTransform
		       ))))))
    end

(*
 * liftNaPatt: pattern -> pattern
 * Lift notAllowed nodes to higher levels in the hierarchy as specified
 *)
val liftNaPatt =
    let
	fun attribute(n,NotAllowed) = NotAllowed
	  | attribute(n,p) = Attribute(n,p)
	fun group l = if Util.exists NotAllowed l then NotAllowed else Group(l)
	fun interleave l = if Util.exists NotAllowed l then NotAllowed
			   else Interleave(l)
	fun choice l = if Util.forAll NotAllowed l then NotAllowed
		       else Choice(Util.delete NotAllowed l)
	fun plus NotAllowed = NotAllowed
	  | plus p = Plus(p)
    in
	foldPatt (updateAttribute attribute
		  (updateGroup group
		   (updateInterleave interleave
		    (updateChoice choice
		     (updatePlus plus idTransform
		      )))))
    end

(*
 * liftEmptyPatt: pattern -> pattern
 * Lift Empty nodes to higher levels in the hierarchy as specified
 *)
val liftEmptyPatt =
    let
	fun group l = if Util.forAll Empty l then Empty
		      else Group(Util.delete Empty l)
	fun interleave l = if Util.forAll Empty l then Empty
			   else Interleave(Util.delete Empty l)
	fun plus Empty = Empty
	  | plus p = Plus(p)
    in
	foldPatt (updateGroup group
		  (updateInterleave interleave
		   (updatePlus plus idTransform
		    )))
    end

(*
 * toDef: content -> content
 * Transform a Start into a Define with the name start, leave others as is
 *)
fun toDef (Start(m,p)) = Define("start",m,p)
  | toDef x = x

(*
 * fromDef: content -> content
 * Transform a Define of the name start into Start, leave others as is
 *)
fun fromDef (d as Define(s,m,p)) =
    if s = "start" then Start(m,p) else d
  | fromDef x = x

(*
 * combinePatt: pattern -> pattern
 * Combine all define nodes defining the same name together using the
 * assignment operator specified
 *)
val combinePatt = 
    let 
	fun grammar l =
	    let 
		(*
		 * Check whether the list has a Define using method m
		 *)
		fun exists m [] = false
		  | exists m (Define(s,m',p)::cs) = m = m' orelse exists m cs
		  | exists m (c::cs) = exists m cs
		(*
		 * Combine a Define with the same method as the other
		 * argument into a list of the other method's list
		 * with the Define's pattern, overriding the other
		 * argument completely if the methods do not match
		 *)
		fun combine (Define(s,m,p),(m',ps)) =
		    if m = m' then (m,p::ps)
		    else (m,[p])
		  | combine (_,x) = x
		(*
		 * Reduce a list of Defines into a single Define,
		 * combining the assignments into the correct type
		 * of pattern
		 *)
		fun reduce [] = Define("start",Assign,Empty)
		  | reduce ((c as Define(s,Assign,p))::cs) =
		    if exists Assign cs then reduce cs
		    else let val (m,ps) = foldr combine (Assign,[]) cs
			in 
			     case m
			      of Or => Define(s,Assign,Choice(p::ps))
			       | And => Define(s,Assign,Interleave(p::ps))
			       | Assign => c
			end
		  | reduce (c::cs) = reduce cs
		(*
		 * Insert a Define statement into a list of lists where
		 * each element list has Defines for only one defined
		 * name
		 *)
		fun insert (c as Define(_,_,_)) [] = [[c]]
		  | insert (c as Define(s,_,_)) (l::ls) =
		    (case hd l
		      of Define(s',_,_) =>
			 if s = s' then ((c::l)::ls)
			 else l::(insert c ls)
		       | x => (l::ls))
		  | insert c l = l
		(*
		 * Split a list of Defines into a list of lists such
		 * that each element list only has Defines for a single
		 * defined name
		 *)
		fun split [] l = l
		  | split (c::cs) [] = split cs [[c]]
		  | split (c::cs) ([]::ss) = split cs ([c]::ss)
		  | split (c::cs) ss = split cs (insert c ss)
	    in 
		Grammar(map fromDef
			    (map reduce
				 (map rev (split (map toDef l) []))))
	    end
    in 
	foldPatt (updateGrammar grammar idTransform)
    end

(*
 * addGrammar: pattern -> pattern
 * If argument is not a Grammar pattern, make one containing a Start
 * definition as the argument pattern
 *)
fun addGrammar (p as Grammar _) = p
  | addGrammar p = Grammar([Start(Assign,p)])

(*
 * simplify: top -> pattern
 * Apply all relevant simplification rules of the RELAX NG specification
 *)
val simplify = addGrammar o combinePatt o liftNaPatt o liftEmptyPatt
	       o removePatt o extPatt o replaceDecl

(*
 * declPatt: pattern -> decl list
 * Fetch a list of all declarations in the pattern, going into files
 * included with include or external statements
 *)
val rec declPatt = fn x =>
    let 
	fun parse s =
	    case parseTop (TextIO.inputAll (TextIO.openIn s))
	     of SOME (Top(ds,p)) => ds @ (declPatt p)
	      | NONE => []
	fun external u = parse u
	fun incl (u,_) = parse u
    in 
	foldPatt (updateExternal external
		  (updateIncl incl (idRetriever())
		   )) x
    end

(*
 * namePatt: pattern -> cname list
 * Fetch all names defined in the pattern for elements or attributes
 *)
val namePatt =
    let
	fun element(n as Name(c),ns) = c::ns
	  | element(_,ns) = ns
	fun attribute(n as Name(c),ns) = c::ns
	  | attribute(_,ns) = ns
    in
	foldPatt (updateElement element
		  (updateAttribute attribute (idRetriever())
		   ))
    end

(*
 * valuePatt: (string * string) list -> pattern -> (cname * string) list
 * Fetch all datatype names returning them as xs:type style attribute
 * values according to the given prefix mapping
 *)
fun valuePatt ds =
    let
	fun element(_,[v]) =
	    (case v of
		 (Cname(Prefix p,s),v') =>
		 if p = "*" andalso s = "*" then [] else [v]
	       | _ => [v])
	  | element(_,vs) = vs
	fun attribute (Name(c as Cname(Uri u,s)),vs) =
	    map (fn (_,v) => (c,v))
	    (List.filter (fn x =>
			     case x of
				 (Cname(Prefix p,s'),_) =>
				 if p = "*" andalso s' = "*"
				 then true else false
			       | _ => false) vs)
	  | attribute (_,vs) = vs
	fun data (DataName(Cname(Uri u,s))) =
	    (case Util.assocr ds u of
		 SOME l => [(Cname((Uri "http://www.w3.org/2001/XMLSchema-instance"),"type"),l ^ ":" ^ s)]
	       | NONE => [])
	  | data (DataValue s) = [(Cname(Prefix("*"),"*"),s)]
	  | data _ = []
    in
	foldPatt (updateElement element
		  (updateAttribute attribute
		   (updateData (legacyData data) (idRetriever())
		    )))
    end

(*
 * identPatt: pattern -> string list
 * Fetch all names used as identifiers in the argument pattern
 *)
val identPatt =
    let
	fun id s = [s]
    in
	foldPatt (updateId id (idRetriever()))
    end

(*
 * defPatt: pattern -> string list
 * Fetch all names defined with either Start or Define constructs
 *)
val defPatt =
    let
	fun start(Rng.Assign,ds) = "start"::ds
	  | start(_,ds) = ds
	fun define(s,Rng.Assign,ds) = s::ds
	  | define(_,_,ds) = ds
    in
	foldPatt (updateStart start
		  (updateDefine define (idRetriever())
		   ))
    end

(*
 * checkPatt: string list -> pattern -> string list
 * Fetch all undefined names used in the pattern; defined names are
 * provided as the argument list
 *)
fun checkPatt ds =
    let
	fun id s =
	    if Util.exists s ds then [] else [s]
	fun start(Rng.Assign,l) = l
	  | start(_,l) =
	    if Util.exists "start" ds then l else "start"::l
	fun define(_,Rng.Assign,l) = l
	  | define(s,_,l) =
	    if Util.exists s ds then l else s::l
    in
	foldPatt (updateId id
		  (updateStart start
		   (updateDefine define (idRetriever())
		    )))
    end

end (* local open Rng *)

end (* struct Transform *)
