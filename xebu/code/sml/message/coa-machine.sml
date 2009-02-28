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
 * Coa: the definition and building of COA machines
 *)
structure Coa = struct

(*
 * The type for XAS events, the Any is for cases where the event type
 * does not matter
 *)
datatype event = StartDocument | EndDocument
	       | StartElement of Rng.uri * string
	       | Attrib of Rng.uri * string * string
	       | EndElement of Rng.uri * string
	       | Content of string
	       | TypedContent of Rng.uri * string
	       | NamespacePrefix of Rng.uri * Rng.prefix
	       | Any

datatype eoaEdgeKind = Del | Out

datatype doaEdgeKind = Read | Peek | Promise | Null

(*
 * The data for an edge is described in COA machine documentation.
 * In addition to that, an edge contains its initial and terminal
 * state numbers.
 *)
type eoaEdgeValue = { event: event,
		      kind: eoaEdgeKind }
type eoaEdge = int * eoaEdgeValue * int

type doaEdgeValue = { event: event,
		      kind: doaEdgeKind,
		      pushed: event list,
		      queued: event list }
type doaEdge = int * doaEdgeValue * int

(*
 * Each machine consists of the start state number followed by its
 * list of edges.
 *)
type eoa = int * eoaEdge list
type doa = int * doaEdge list

exception OutEx of string

local 

    open Rng
    open Transform

    fun eventToString StartDocument = "SD()"
      | eventToString EndDocument = "ED()"
      | eventToString (StartElement(u,n)) = "SE(" ^ u ^ " " ^ n ^ ")"
      | eventToString (Attrib(u,n,v)) = "A(" ^ u ^ " " ^ n ^ " " ^ v ^ ")"
      | eventToString (EndElement(u,n)) = "EE(" ^ u ^ " " ^ n ^ ")"
      | eventToString (Content(t)) = "C(" ^ t ^ ")"
      | eventToString (TypedContent(u,n)) = "TC(" ^ u ^ " " ^ n ^ ")"
      | eventToString (NamespacePrefix(u,p)) = "NP(" ^ u ^ " " ^ p ^ ")"
      | eventToString Any = "AN()"

    fun delPair _ [] = []
      | delPair x ((y,z)::xs) =
	if x = y orelse x = z then delPair x xs
	else (y,z)::(delPair x xs)
    fun delUnknownPair k u l =
	if k <> u then delPair u l else l
    fun delEdge _ [] = []
      | delEdge x ((a as (y,_,z))::xs) =
	if x = y orelse x = z then delEdge x xs
	else a::(delEdge x xs)
    fun delUnknownEdge k u l =
	if k <> u then delEdge u l else l

    type nodePair = int * int

    (*
     * The result of converting a pattern into an EOA is a possible
     * pair of node pairs: the first pair is used when it is
     * known that this pattern happens and the second pair when this
     * is not known.  The first node in each pair is the entry
     * node and the second is the exit node.  The first list collects
     * node pairs that should be identified with each other.  The
     * second list collects all edges of the machine
     *)
    type eoaPattResult = (nodePair * nodePair) * nodePair list
			 * eoaEdge list

    datatype eoaResult = EoaValue of string
		       | EoaExPair of eoaPattResult
		       | EoaOpen of eoaPattResult

    fun stripEoaResult (EoaValue _) = (NONE,[],[])
      | stripEoaResult (EoaExPair(x,ps,es)) = (SOME x,ps,es)
      | stripEoaResult (EoaOpen(x,ps,es)) = (SOME x,ps,es)

    fun addEoaResult NONE = EoaValue ""
      | addEoaResult (SOME x) = EoaExPair x

    fun eoaPatt node =
	let 
	    fun nextId () = !node before node := !node + 1
	    fun toKind (EoaValue _) = Del
	      | toKind (EoaExPair _) = Del
	      | toKind (EoaOpen _) = Out
	    fun fromKind Del = EoaExPair
	      | fromKind Out = EoaOpen
	    fun strip (EoaValue _) =
		let
		    val id = nextId()
		in
		    (((id,id),(id,id)),[],[])
		end
	      | strip (EoaExPair(x,ps,es)) = (x,ps,es)
	      | strip (EoaOpen(x,ps,es)) = (x,ps,es)
	    fun element (n,e) =
		let
		    val (((lke,lkx),(lue,lux)),ps,es) = strip e
		    val ke = nextId()
		    val kx = nextId()
		    val ps' = delUnknownPair lke lue ps
		    val ps'' = delUnknownPair lkx lux ps'
		    val es' = delUnknownEdge lke lue es
		    val es'' = delUnknownEdge lkx lux es'
		in
		    case n of
			Name(Cname(Uri u,l)) =>
			let
			    val kind = toKind e
			    val ue = nextId()
			    val ux = nextId()
			in
			    EoaExPair(((ke,kx),(ue,ux)),ps'',
				      [(ke,{event=StartElement(u,l),kind=Del},
					lke),
				       (lkx,{event=EndElement(u,l),kind=kind},
					kx),
				       (ue,{event=StartElement(u,l),kind=Out},
					lke),
				       (lkx,{event=EndElement(u,l),kind=Out},
					ux)]
				      @ es'')
			end
		      | _ =>
			EoaExPair(((ke,kx),(ke,kx)),ps'',
				  [(ke,{event=StartElement("*","*"),kind=Out},
				    lke),
				   (lkx,{event=EndElement("*","*"),kind=Out},
				    kx)]
				  @ es'')
		end
	    fun attribute (Name(Cname(Uri s,l)),x as EoaValue v) =
		if v <> "" then
		    let
			val id = nextId()
		    in
			EoaExPair(((id,id),(id,id)),[],[(id,{event=Attrib(s,l,v),kind=Del},id)])
		    end
		else x
	      | attribute (_,x) = x
	    fun group [] = raise (OutEx "Empty Group found")
	      | group [e] = e
	      | group (e::es) =
		let
		    fun isOpen (EoaOpen _) = true
		      | isOpen _ = false
		    val res = group es
		    val (((lke,lkx),(lue,lux)),lps,les) = strip res
		    val (((ke,kx),(ue,ux)),ps,es) = strip e
		    val lre = if isOpen e then lue else lke
		    val con = fromKind (toKind res)
		    val ps' = delUnknownPair kx ux ps
		    val lps' = delUnknownPair lre lue lps
		    val es' = delUnknownEdge kx ux es
		    val les' = delUnknownEdge lre lue les
		in
		    con(((ke,lkx),(ue,lux)),(kx,lre)::(ps' @ lps'),es' @ les')
		end
	    fun choice [] = raise (OutEx "Empty Choice found")
	      | choice [e] = e
	      | choice (e::es) =
		let
		    val res = choice es
		    val (((lke,lkx),(lue,lux)),lps,les) = strip res
		    val (((ke,kx),(ue,ux)),ps,es) = strip e
		    fun con Del Del = Del
		      | con _ _ = Out
		    val cons = fromKind (con (toKind e) (toKind res))
		in
		    cons(((ue,ux),(ue,ux)),
			 (lue,ue)::(lux,ux)::(ps @ lps),
			 es @ les)
		end
	    fun interleave _ = EoaValue ""
	    fun question (e as EoaValue _) = raise (OutEx "Value given for ?")
	      | question e =
		let
		    val (((ke,kx),(ue,ux)),ps,es) = strip e
		    val con = fromKind (toKind e)
		    val ps' = delUnknownPair ue ke ps
		    val ps'' = delUnknownPair ux kx ps'
		    val es' = delUnknownEdge ue ke es
		    val es'' = delUnknownEdge ux kx es'
		in
		    con(((ue,ux),(ue,ux)),(ue,ux)::ps'',es'')
		end
	    fun star e =
		let
		    val (((ke,kx),(ue,ux)),ps,es) = strip e
		in
		    EoaOpen(((ke,kx),(ue,ux)),(ke,kx)::(ue,ux)::ps,es)
		end
	    fun data (DataName(Cname(Prefix p,l))) =
		(case p of
		     "*" => EoaValue ""
		   | _ =>
		     let
			 val id = nextId()
		     in
			 EoaExPair(((id,id),(id,id)),[],[(id,{event=Attrib("http://www.w3.org/2001/XMLSchema-instance","type",p ^ ":" ^ l),kind=Del},id)])
		     end)
	      | data (DataValue s) = EoaValue s
	      | data _ = EoaValue ""
	in 
	    foldPatt (updateElement element
		      (updateAttribute attribute
		       (updateGroup group
			(updateChoice choice
			 (updateInterleave interleave
			  (updateQuestion question
			   (updateStar star
			    (updateData (legacyData data)
					(idBuilder (EoaValue "") group)
			     ))))))))
	end

    (*
     * The result of converting a pattern into a DOA consists of half-edge
     * pairs since new events may need to be pushed or queued into the
     * edges.
     *)
    type doaHalfEdge = doaEdgeValue * int

    type doaHalfEdgePair = doaHalfEdge * doaHalfEdge

    type doaPattResult = (doaHalfEdgePair * doaHalfEdgePair)
			 * doaEdge list

    datatype doaResult = DoaValue of string
		       | DoaExPair of doaPattResult
		       | DoaOpen of doaPattResult
		       | DoaQuestion of doaPattResult

    fun stripDoaResult (DoaValue _) = (NONE,[])
      | stripDoaResult (DoaExPair(x,es)) = (SOME x,es)
      | stripDoaResult (DoaOpen(x,es)) = (SOME x,es)
      | stripDoaResult (DoaQuestion(x,es)) = (SOME x,es)

    fun addDoaResult (NONE,_) = DoaValue ""
      | addDoaResult (SOME x,es) = DoaExPair(x,es)

    fun makeExitEdge(x,(ed,e)) = (e,ed,x)
    fun makeEntryEdge(e,(ed,x)) = (e,ed,x)

    fun enqueue({event,kind,queued,pushed},id) q =
	({event=event,kind=kind,queued=queued @ [q],pushed=pushed},id)
    fun unqueue({event,kind,queued,pushed},id) q =
	({event=event,kind=kind,queued=q::queued,pushed=pushed},id)
    fun push({event,kind,queued,pushed},id) p =
	({event=event,kind=kind,queued=queued,pushed=p::pushed},id)
    fun event({event=Any,kind=Null,queued,pushed},id) ev =
	({event=ev,kind=Read,queued=queued,pushed=pushed},id)
      | event ({event=ev,...},_) _ =
	raise (OutEx ("Tried to override non-empty event "
		      ^ (eventToString ev)))
    fun toqueue({event,kind,queued,pushed},id)
      = ({event=event,kind=kind,queued=pushed @ queued,pushed=[]},id)
    fun topush({event,kind,queued,pushed},id)
      = ({event=event,kind=kind,queued=[],pushed=pushed @ queued},id)

    fun doaPatt node ds =
	let 
	    fun nextId () = !node before node := !node + 1
	    fun emptyHalfEdge id = ({event=Any,kind=Null,queued=[],pushed=[]},
				    id)
	    fun peekHalfEdge id = ({event=Any,kind=Peek,queued=[],pushed=[]},
				   id)
	    fun strip (DoaValue _) =
		let
		    val id = nextId()
		    val e = emptyHalfEdge id
		in
		    (((e,e),(e,e)),[])
		end
	      | strip (DoaExPair(x,es)) = (x,es)
	      | strip (DoaOpen(x,es)) = (x,es)
	      | strip (DoaQuestion(x,es)) = (x,es)
	    fun toCon (DoaValue _) = DoaExPair
	      | toCon (DoaExPair _) = DoaExPair
	      | toCon (DoaOpen _) = DoaOpen
	      | toCon (DoaQuestion _) = DoaQuestion
	    fun isNull ({kind=Null,event=Any,queued,pushed},_) = true
	      | isNull _ = false
	    fun isEmpty ({kind=Null,event=Any,queued=[],pushed=[]},_) = true
	      | isEmpty _ = false
	    fun makeReadEntryEdge ed e =
		if isNull ed then (event (toqueue ed) e,[])
		else
		    let
			val id = nextId()
		    in
			(({event=e,kind=Read,queued=[],pushed=[]},id),
			 [makeEntryEdge(id,ed)])
		    end
	    fun makeReadExitEdge ed e =
		if isNull ed then (event (topush ed) e,[])
		else
		    let
			val id = nextId()
		    in
			(({event=e,kind=Read,queued=[],pushed=[]},id),
			 [makeExitEdge(id,ed)])
		    end
	    fun element (n,e) =
		let
		    val (((ke,kx),(ue,ux)),es) = strip e
		    val (se,ee,known) =
			let
			    val (u,l,known) =
				case n of
				    Name(Cname(Uri u,l)) => (u,l,true)
				  | _ => ("*","*",false)
			in
			    (StartElement(u,l),EndElement(u,l),known)
			end
		    val isClosed =
			case e of
			    DoaOpen _ => false
			  | _ => true
		    val (nue,nues) = makeReadEntryEdge ke se
		    val (nux,nuxs) = makeReadExitEdge kx ee
		    val nke =
			if known then
			    if isNull ke then unqueue ke se
			    else push ke se
			else nue
		    val nkx =
			if known andalso isClosed then enqueue kx ee
			else nux
		in
		    DoaExPair(((nke,nkx),(nue,nux)),nues @ nuxs @ es)
		end
	    fun attribute (Name(Cname(Uri u,l)),x as DoaValue v) =
		if v <> "" then
		    let
			val id = nextId()
			val e = ({event=Any,kind=Null,queued=[Attrib(u,l,v)],pushed=[]},id)
			val x = emptyHalfEdge id
		    in
			DoaExPair(((e,x),(e,x)),[])
		    end
		else x
	      | attribute (_,x) = DoaValue ""
	    fun group [] = raise (OutEx "Empty Group found")
	      | group [e] = e
	      | group ((DoaValue _)::es) = group es
	      | group (e::es) =
		let
		    fun isQuestion (DoaQuestion _) = true
		      | isQuestion _ = false
		    fun isOpen (DoaOpen _) = true
		      | isOpen _ = false
		    fun merge x e =
			let
			    val xv = #1(x)
			    val ev = #1(e)
			in
			    if isNull x then
				if isNull e then
				    [(#2(x),
				      {event=Any,kind=Peek,queued=[],
				       pushed= #pushed(ev) @ #queued(ev)
					       @ #pushed(xv) @ #queued(xv)},
				      #2(e))]
				else
				    [(#2(x),{event= #event(ev),kind= #kind(ev),
					     queued= #queued(ev),
					     pushed= #pushed(xv) @ #queued(xv)
						     @ #queued(ev)},#2(e))]
			    else
				if isNull e then
				    [(#2(x),{event= #event(xv),kind= #kind(xv),
					     queued= #queued(xv) @ #pushed(ev)
						     @ #queued(ev),
					     pushed= #pushed(xv)},#2(e))]
				else
				    let
					val id = nextId()
				    in
					[makeExitEdge(id,x),
					 makeEntryEdge(id,e)]
				    end
			end
		    val res = group es
		    val (((lke,lkx),(lue,lux)),les) = strip res
		    val (((ke,kx),(ue,ux)),es) = strip e
		    val lre = if isOpen e then lue else lke
		    val con = toCon res
		in
		    if es = [] then
			let
			    val lev = #1(lke)
			    val ev = #1(ke)
			    val nke = ({event= #event(lev),kind= #kind(lev),
					queued= #queued(ev) @ #queued(lev),
					pushed= #pushed(ev) @ #pushed(lev)},
				       #2(lke))
			    val lxv = #1(lkx)
			    val xv = #1(kx)
			    val nkx = ({event= #event(lxv),kind= #kind(lxv),
					queued= #queued(xv) @ #queued(lxv),
					pushed= #pushed(xv) @ #pushed(lxv)},
				       #2(lkx))
			in
			    con(((nke,nkx),(lue,lux)),es @ les)
			end
		    else if isQuestion e then
			if isQuestion res then
			    let
				val id = nextId()
				val ne = peekHalfEdge id
				val nx = peekHalfEdge id
			    in
				DoaExPair(((ne,nx),(ne,nx)),
					  makeEntryEdge(id,ue)::
					  makeExitEdge(id,ux)::
					  makeEntryEdge(id,lue)::
					  makeExitEdge(id,lux)::
					  (es @ les))
			    end
			else
			    let
				val id = nextId()
				val qe = makeEntryEdge(id,ue)
				val qx = makeExitEdge(id,ux)
				val nx = makeEntryEdge(id,lke)
				val ne = peekHalfEdge id
			    in
				con(((ne,lkx),(ne,lux)),qe::qx::nx::(es @ les))
			    end
		    else if isQuestion res then
			let
			    val id = nextId()
			    val qe = makeEntryEdge(id,lue)
			    val qx = makeExitEdge(id,lux)
			    val ne = makeExitEdge(id,kx)
			    val x = peekHalfEdge id
			    val ncon = toCon e
			in
			    ncon(((ke,x),(ue,x)),qe::qx::ne::(es @ les))
			end
		    else
			con(((ke,lkx),(ue,lux)),(merge kx lre) @ es @ les)
		end
	    fun choice [] = raise (OutEx "Empty Choice found")
	      | choice [e] = e
	      | choice es =
		let
		    fun toEntryEdge id (((_,_),(ue,_)),_) =
			makeEntryEdge(id,ue)
		    fun toExitEdge id (((_,_),(_,ux)),_) =
			makeExitEdge(id,ux)
		    fun mapToCons [] = DoaExPair
		      | mapToCons ((DoaOpen _)::_) = DoaOpen
		      | mapToCons (_::es) = mapToCons es
		    fun extractEdges (_,es) = es
		    val eid = nextId()
		    val xid = nextId()
		    val ees = map (toEntryEdge eid) (map strip es)
		    val xes = map (toExitEdge xid) (map strip es)
		    val ee = emptyHalfEdge eid
		    val xe = emptyHalfEdge xid
		    val con = mapToCons es
		in
		    con(((ee,xe),(ee,xe)),
			ees @ xes @ List.concat (map extractEdges
						     (map strip es)))
		end
	    fun interleave _ = DoaValue ""
	    fun question (e as DoaValue _) = raise (OutEx "Value given for ?")
	      | question e =
		let
		    val (((_,_),(ue,ux)),es) = strip e
		in
		    DoaQuestion(((ue,ux),(ue,ux)),es)
		end
	    fun star e =
		let
		    val (((ke,kx),(ue,ux)),es) = strip e
		    val id = nextId()
		    val edge = emptyHalfEdge id
		in
		    DoaOpen(((edge,edge),(edge,edge)),makeEntryEdge(id,ke)::
						      makeExitEdge(id,kx)::es)
		end
	    fun data (DataName(Cname(Prefix p,l))) =
		(case p of
		     "*" =>
		     let
			 val id = nextId()
			 val e = ({event=Content("*"),kind=Peek,
				   queued=[],pushed=[]},id)
			 val x = ({event=Content("*"),kind=Read,
				   queued=[],pushed=[]},id)
		     in
			 DoaExPair(((e,x),(e,x)),[])
		     end
		   | _ =>
		     let
			 val id = nextId()
			 val u = valOf (Util.assocl ds p)
			 val e = ({event=TypedContent("*","*"),kind=Promise,
				   queued=[],pushed=[Attrib("http://www.w3.org/2001/XMLSchema-instance","type",p ^ ":" ^ l)]},id)
			 val x = ({event=TypedContent(u,l),kind=Read,
				   queued=[],pushed=[]},id)
		     in
			 DoaExPair(((e,x),(e,x)),[])
		     end)
	      | data (DataValue s) = DoaValue s
	      | data _ = DoaValue ""
	in 
	    foldPatt (updateElement element
		      (updateAttribute attribute
		       (updateGroup group
			(updateChoice choice
			 (updateInterleave interleave
			  (updateQuestion question
			   (updateStar star
			    (updateData (legacyData data)
					(idBuilder (DoaValue "") group)
			     ))))))))
	end

    fun depends [] l = l
      | depends ((d as Define(s,m,p))::ds) l = depends ds ((d,identPatt p)::l)
      | depends _ _ = raise (OutEx "Non-Define found in Grammar")

    fun toposort ds =
	let
	    fun split [] es fs n = (es,fs,n)
	      | split (d as (x,[])::ds) es fs n = split ds (x::es) fs (n+1)
	      | split (d::ds) es fs n = split ds es (d::fs) n
	    fun delete (Define(s,_,_)) (d,l) =
		(d,List.filter (fn x => x <> s) l)
	      | delete _ _ = raise (OutEx "Non-Define found when sorting")
	    fun loop [] l = l
	      | loop ds l =
		let
		    val (es,fs,n) = split ds [] [] 0
		    val fs' =
			let
			    fun del [] l = l
			      | del (d::ds) l = del ds (map (delete d) l)
			in
			    del es fs
			end
		in
		    if n > 0 then
			loop fs' (l @ es)
		    else raise (OutEx "Recursive definition found")
		end
	in
	    loop (depends ds []) []
	end

    fun reduce ds =
	let 
	    fun id s =
		case Util.assocl ds s
		 of NONE => raise (OutEx ("Identifier " ^ s ^ " not defined"))
		  | SOME e => reduce ds e
	in 
	    foldPatt (updateId id idTransform)
	end

    fun revedge (e,ed,x) = (x,ed,e)
    fun reach n es =
	let
	    fun matchEdge n (e,_,_) = n = e
	    fun fetch n =
		map (#3) (List.filter (matchEdge n) es)
	    fun reach' [] rs = rs
	      | reach' (n::ns) rs =
		let
		    val f = List.filter (fn x => not (Util.exists x rs)) (fetch n)
		in
		    reach' (ns @ f) (n::rs)
		end
	in
	    reach' [n] []
	end
    fun isReach ls (e,_,x) = Util.exists e ls andalso Util.exists x ls

in

(*
 * buildEoa: (string * string) list -> pattern -> eoa
 * Build an EOA from the pattern using the namespace and datatype
 * mapping provided as an argument
 *)
fun buildEoa ds p: eoa =
    let 
	val node = ref 0
	val ps =
	    case p of
		Grammar l => (toposort (map toDef l))
	      | _ => raise (OutEx "Non-Grammar form encountered")
	fun toPair (Define(s,m,p)) = (s,p)
	  | toPair _ = raise (OutEx "Non-Define encountered building EOA")
	val ps' = map toPair (map toDef ps)
	val red = 
	    case Util.assocl ps' "start"
	     of NONE => raise (OutEx "No definition for start found")
	      | SOME p => reduce ps' p
	val es = eoaPatt node red
	fun full (n,p) = n <> "" andalso p <> ""
	val edges =
	    map (fn (p,u) =>
		    (!node,{event=NamespacePrefix(u,p),kind=Del},!node))
		(List.filter full ds)
    in 
	case stripEoaResult es
	 of (NONE,_,_) => (!node,edges)
	  | (SOME((e1,x1),(e2,x2)),ps,es) =>
	    let
		val ps' = if e1 <> !node then (e1,!node)::ps else ps
		val ps'' = if x1 <> !node then (x1,!node)::ps' else ps'
		val ps''' = delUnknownPair e1 e2 (delUnknownPair x1 x2 ps'')
		val es' = delUnknownEdge e1 e2 (delUnknownEdge x1 x2 es)
		fun buildUnf [] l = l
		  | buildUnf (p::ps) l = buildUnf ps (Unf.add p l)
		val unf = buildUnf ps''' []
		fun mapEdge l (x,e,y) = (Unf.find x l,e,Unf.find y l)
		val gr = map (mapEdge unf) (edges @ es')
		val rg = map revedge gr
		val es'' = List.filter
			       (isReach (Util.intersect (map (#1) gr)
							(map (#1) rg)))
			       gr
	    in
		(Unf.find (!node) unf,es'')
	    end
    end

(*
 * buildDoa: (string * string) list -> pattern -> doa
 * Build a DOA from the pattern using the namespace and datatype
 * mapping provided as an argument
 *)
fun buildDoa ds p: doa =
    let 
	val node = ref 0
	fun nextId () = !node before node := !node + 1
	val ps =
	    case p of
		Grammar l => (toposort (map toDef l))
	      | _ => raise (OutEx "Non-Grammar form encountered")
	fun toPair (Define(s,m,p)) = (s,p)
	  | toPair _ = raise (OutEx "Non-Define encountered building DOA")
	val ps' = map toPair (map toDef ps)
	val red = 
	    case Util.assocl ps' "start"
	     of NONE => raise (OutEx "No definition for start found")
	      | SOME p => reduce ps' p
	val es = doaPatt node ds red
	fun full (n,p) = n <> "" andalso p <> ""
	val queued = map (fn (p,u) => NamespacePrefix(u,p))
			 (List.filter full ds)
	val current = nextId()
	val entry = (!node,{event=StartDocument,kind=Read,queued=queued,pushed=[]},current)
	val exit = (current,{event=EndDocument,kind=Read,queued=[],pushed=[]},!node)
    in 
	case stripDoaResult es
	 of (NONE,_) => (!node,[entry,exit])
	  | (SOME((e1,x1),(e2,x2)),es) =>
	    let
		val e =
		    case e1 of
			({kind=Null,event=e,queued=q,pushed=p},id) =>
			[(!node, {event=StartDocument,kind=Read,
				  queued=queued @ p @ q, pushed=[]}, id)]
		      | _ => [entry,makeEntryEdge(current,toqueue e1)]
		val x =
		    case x1 of
			({kind=Null,event=e,queued=q,pushed=p},id) =>
			[(id, {event=EndDocument,kind=Read,
			       queued=[], pushed=p @ q}, !node)]
		      | _ => [exit,makeExitEdge(current,topush x1)]
		val gr = e @ x @ es
		val rg = map revedge gr
		val es' = List.filter
			      (isReach (Util.intersect (map (#1) gr)
						       (map (#1) rg)))
			      gr
	    in
		(!node, es')
	    end
    end

end (* local *)

end (* struct Coa *)
