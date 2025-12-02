package org.aurora.sjsast

import cats.kernel.BoundedSemilattice


object catsgivens :
  given [T] :  BoundedSemilattice[Option[T]] = new BoundedSemilattice[Option[T]] {
    def empty: Option[T] = None
    def combine(x: Option[T], y: Option[T]): Option[T] = 
      if (x == empty) y
      else if (y == empty) x
      else x |+| y
  }


  given [T]:BoundedSemilattice[Set[T]] = new BoundedSemilattice[Set[T]] :
    def empty: Set[T] = Set.empty
    def combine (x: Set[T], y:Set[T]) :  Set[T] =
      x union y
  

  given [T]:BoundedSemilattice[Map[String,CIO]] = new BoundedSemilattice[Map[String,CIO]] {
    def empty: Map[String,CIO] = Map.empty
    def combine (x: Map[String,CIO], y:Map[String,CIO]) :  Map[String,CIO] =
      if(x == empty) y
       else if (y==empty) x
       else {
        val keys = x.keySet union y.keySet

        val result = keys.map{k => 
          (x.get(k),y.get(k)) match {
            case (Some(x:CIO),Some(y:CIO)) => k -> ( x |+| y )
            case (None,Some(y:CIO)) => k -> y
            case (Some(x:CIO),None) => k -> x
            case (None,None) =>  throw Exception("This should not happen")
          }
        }.toMap    
        result
      }
  }

  

  given BoundedSemilattice[PCM] =  new BoundedSemilattice[PCM] {
    def empty: PCM = PCM(Map.empty)
    def combine (x: PCM, y:PCM) :  PCM =
      if(x == empty) y
       else if (y==empty) x
       else PCM( x.cio |+| y.cio) 
  }


  given BoundedSemilattice[Issues] =  new BoundedSemilattice[Issues] {
    def empty: Issues = Issues(Set.empty,Set.empty)
    def combine (x: Issues, y:Issues) :  Issues =
      if(x == empty) y
       else if (y==empty) x
       else Issues( x.ics |+| y.ics, x.narrative |+| y.narrative) 
  }


  given BoundedSemilattice[Orders] =  new BoundedSemilattice[Orders] {
    def empty: Orders = Orders(Set.empty,Set.empty)
    def combine (x: Orders, y:Orders) :  Orders =
      if(x == empty) y
       else if (y==empty) x
       else Orders( x.ngo |+| y.ngo, x.narrative |+| y.narrative) 
  }


  given BoundedSemilattice[Clinical] =  new BoundedSemilattice[Clinical] {
    def empty: Clinical = Clinical(Set.empty,Set.empty)
    def combine (x: Clinical, y:Clinical) :  Clinical =
      if(x == empty) y
       else if (y==empty) x
       else Clinical( x.ngc |+| y.ngc, x.narrative |+| y.narrative) 
  }


  given BoundedSemilattice[CIO] =  new BoundedSemilattice[CIO] {
    def empty: CIO = Issues(Set.empty,Set.empty)
    def combine (x: CIO, y:CIO) :  CIO =
      (x,y) match {
        case (i1:Issues, i2:Issues) => i1 |+| i2
        case (o1:Orders, o2:Orders) => o1 |+| o2
        case (c1:Clinical, c2:Clinical) => c1 |+| c2
        case  _ =>  ???
      }
  }







  
end catsgivens
