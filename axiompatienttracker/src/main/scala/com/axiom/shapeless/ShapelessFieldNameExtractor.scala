package com.axiom



object ShapelessFieldNameExtractor :
  import shapeless3.deriving.*
  import scala.compiletime.{erasedValue, summonInline,constValue}
  import scala.deriving.Mirror

  inline def fieldNames[T](using m: Mirror.Of[T]): List[String] =
    summonAllLabels[m.MirroredElemLabels]

  inline def summonAllLabels[T <: Tuple]: List[String] =
    inline erasedValue[T] match
      case _: (head *: tail) => constValue[head].toString :: summonAllLabels[tail]
      case _: EmptyTuple => Nil




  






