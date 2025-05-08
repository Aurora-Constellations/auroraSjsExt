package docere.sjsast

import cats.kernel.BoundedSemilattice
import cats.syntax.semigroup._ // for |+|


object catsgivens :
  given [T] :  BoundedSemilattice[Option[T]] = new BoundedSemilattice[Option[T]] {
    def empty: Option[T] = None
    def combine(x: Option[T], y: Option[T]): Option[T] = 
      if (x == empty) y
      else if (y == empty) x
      else x |+| y
  }

  //TODO NOT SURE ABOUT LIST as there can be duplicate elements
  given [T] :  BoundedSemilattice[List[T]] = new BoundedSemilattice[List[T]] {
    def empty: List[T] = List.empty
    def combine (x: List[T], y:List[T]) :  List[T] =
      if(x == empty) y
       else if (y==empty) x
       else x ++ y
  }  

  given [T <: SjsNode] : BoundedSemilattice[Set[T]] = new BoundedSemilattice[Set[T]] {
    def empty: Set[T] = Set.empty
    def combine (x: Set[T], y:Set[T]) :  Set[T] =
      if(x == empty) y
       else if (y==empty) x
       else (x.asMap |+| y.asMap).map{(k,v) => v.asInstanceOf[T]}.toSet
  }     


  given [T <: SjsNode] : BoundedSemilattice[Map[String,T]] = new BoundedSemilattice[Map[String,T]] {
    def empty: Map[String,T] = Map.empty
    def combine (x: Map[String,T], y:Map[String,T]) :  Map[String,T] =
      if(x == empty) y
        else if (y==empty) x
        else {
          val keys = x.keySet union y.keySet

          val result = keys.map{k => 
            (x.get(k),y.get(k)) match {
              case (Some(x),Some(y)) => k -> x.merge(y).asInstanceOf[T]
              case (Some(x),None) =>    k -> x
              case (None,Some(y)) =>    k -> y
              case (None,None)    =>    ???//k -> SjsAst.InvalidSjsNode().merge[T](SjsAst.InvalidSjsNode())
            }
          }.toMap    

          
          result
      }
  }









  
end catsgivens
