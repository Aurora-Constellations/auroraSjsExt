package com.axiom

import com.raquo.laminar.api.L
import java.lang.reflect.Field


object ShapelessCaseEnum :
  import shapeless3.deriving.*
  import com.raquo.laminar.api.L.{*, given}
  import java.time._
  import org.scalajs.dom
  

  sealed trait DataType[+T] :
    val value:T
    def valueString(): String


  case class DateT(value:LocalDate) extends DataType[LocalDate] :
    def valueString(): String = value.toString
  // object DateT:
  //   def unapply(value: LocalDate): Option[LocalDate] = Some(value)  

  case class DateTimeT(value: LocalDateTime) extends DataType[LocalDateTime] :
    def valueString(): String = value.toString  
  // object DateTimeT:
  //   def unapply(value: LocalDateTime): Option[LocalDateTime] = Some(value)  

  case class BooleanT(value:Boolean) extends DataType[Boolean] :
    def valueString(): String = value.toString     
  // object BooleanT:
    def unapply(value: Boolean): Option[Boolean] = Some(value)

  case class StringT(value:String) extends DataType[String]  :
    def valueString(): String = value.toString
  // object StringT:
  //   def unapply(value: String): Option[String] = Some(value)

  case class IntT(value:Int) extends DataType[Int] :
    def valueString(): String = value.toString
  // object IntT:
  //   def unapply(value: Int): Option[Int] = Some(value)
  case class OptionT[A](value: Option[A]) extends DataType[Option[A]] :
    def valueString():String = value.toString()



  trait TraitMapper[A] :
    def map(a:A) : List[DataType[A]]


  trait EnumMapper[A]:
    def map(a: A): List[FieldType]



  enum FieldType :
    case DateT(value: LocalDate) 
    case DateTimeT(value: LocalDateTime) 
    case DateTimeOpt(value: Option[LocalDateTime])
    case LocalDateTimeOpt(value: Option[LocalDateTime])
    case BooleanT(value: Boolean)
    case StringT(value: String)
    case StringOpt(value: Option[String])
    case IntT(value: Int)

  /**
    * 
    */

  object TraitMapper :
    given TraitMapper[LocalDate] = DateT(_) +: Nil
    given TraitMapper[LocalDateTime] = DateTimeT(_) +: Nil
    given TraitMapper[Boolean] = BooleanT(_) +: Nil
    given TraitMapper[String] = StringT(_) +: Nil
    given TraitMapper[Int] =  IntT(_) +: Nil
    
   // Implicit instance for Option
    given [A](using ev: TraitMapper[A]): TraitMapper[Option[A]] with
      def map(a: Option[A]): List[DataType[Option[A]]] = a match
        case Some(value) => ev.map(value).map(x => OptionT(a))
        case None => List(OptionT(None))

      // def deriveTraitProduct[A](using pInst: K0.ProductInstances[TraitMapper, A], labelling: Labelling[A]  ): TraitMapper[A] =
      // (a: A) =>
      //   labelling.elemLabels.zipWithIndex
      //     .map { (label, index) =>
      //       val value = pInst.project(a)(index)([t] => (st: TraitMapper[t], pt: t) => st.map(pt))
      //       value
      //     }.foldLeft(List.empty[DataType[A]])(_ ++ _)
      // def deriveTraitSum[A](using inst: K0.CoproductInstances[TraitMapper, A] ): TraitMapper[A] =
      //   (a: A) => inst.fold(a)([a] => (st: TraitMapper[a], a: a) => st.map(a))

      // inline given derived[A](using gen: K0.Generic[A]): TraitMapper[A] =
      //   gen.derive(deriveTraitProduct, deriveTraitSum)


      // def traitCoProduct [T] (v:T)(using  x: TraitMapper[T]) = x.map(v)  



  object EnumMapper:
    given EnumMapper[Int] = FieldType.IntT(_) +: Nil
    given EnumMapper[Boolean] = FieldType.BooleanT(_) +: Nil
    given EnumMapper[LocalDate] = FieldType.DateT(_) +: Nil
    given EnumMapper[LocalDateTime] = FieldType.DateTimeT(_) +: Nil
    given EnumMapper[String] = FieldType.StringT(_) +: Nil
     // Generic given for Option types
    given [A](using ev: EnumMapper[A]): EnumMapper[Option[A]] = 
      (opt: Option[A]) => opt match
        case Some(value) => ev.map(value)
        case None => Nil

    


    def deriveEnumProduct[A](using pInst: K0.ProductInstances[EnumMapper, A], labelling: Labelling[A]  ): EnumMapper[A] =
      (a: A) =>
        labelling.elemLabels.zipWithIndex
          .map { (label, index) =>
            val value = pInst.project(a)(index)([t] => (st: EnumMapper[t], pt: t) => st.map(pt))
            value
          }.foldLeft(List.empty[FieldType])(_ ++ _)
    def deriveEnumSum[A](using inst: K0.CoproductInstances[EnumMapper, A] ): EnumMapper[A] =
      (a: A) => inst.fold(a)([a] => (st: EnumMapper[a], a: a) => st.map(a))

    inline given derived[A](using gen: K0.Generic[A]): EnumMapper[A] =
      gen.derive(deriveEnumProduct, deriveEnumSum)


    def enumCoProduct [T] (v:T)(using  x: EnumMapper[T]) = x.map(v)  
  







