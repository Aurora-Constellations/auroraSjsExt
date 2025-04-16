package com.axiom


import shapeless3.deriving.*
import java.time.*

case class Display(text:String, color:String)

trait TableColProperties[A]:
  def element(a: A):List[Display]

object TableColProperties:
  given TableColProperties[Int] =         i => List(Display(i.toString, "red"))
  given TableColProperties[Boolean] =     b => List(Display(b.toString,"blue"))
  given TableColProperties[String] =      s =>  List(Display(identity(s),"#3361ff"))
  given TableColProperties[LocalDate] =   d => List(Display(d.toString(),"yellow"))
  given TableColProperties[LocalDateTime] = d => List(Display(d.toString(),"cyan"))


  def deriveShowProduct[A](using
  pInst: K0.ProductInstances[TableColProperties, A],
  labelling: Labelling[A]
  ): TableColProperties[A] =
    (a: A) =>
      val properties = labelling.elemLabels.zipWithIndex
        .map { (label, index) =>
          val value = pInst.project(a)(index)([t] => (st: TableColProperties[t], pt: t) => st.element(pt))
          value
        }.foldLeft(List.empty[Display])((a,b) =>a ++ b)
        properties
        
        // labelling.label match {
        //   case "Person" => Display("Person","green") //change color properties depending on the name of the case class
        //   case _ => Display("Other","white")
        // }


  def deriveShowSum[A](using
      inst: K0.CoproductInstances[TableColProperties, A]
  ): TableColProperties[A] =
    (a: A) => inst.fold(a)([a] => (st: TableColProperties[a], a: a) => st.element(a))

  inline given derived[A](using gen: K0.Generic[A]): TableColProperties[A] =
    gen.derive(deriveShowProduct, deriveShowSum)






