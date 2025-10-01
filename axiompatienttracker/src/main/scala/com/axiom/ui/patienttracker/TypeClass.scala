package com.axiom.ui.patienttracker

import shapeless3.deriving.*
import java.time.*
import com.raquo.laminar.api.L.{*, given}
import com.axiom.ui.patienttracker.utils.{Status, StatusIcons}
import org.scalajs.dom
import com.axiom.ui.patienttracker.TypeClass.CellDataConvertor
// import com.axiom.ui.patienttracker.tableutils.TableDerivation.given

object TypeClass:

  trait RenderCellData[A]:
    def celldata(a: A): HtmlElement

  object RenderCellData:
    given RenderCellData[String] = text => div(text)
    given RenderCellData[Int] = numeric => div(s"$numeric")
    given RenderCellData[LocalDateTime] = datetime => div(s"$datetime")
    given RenderCellData[LocalDate] = date => div(s"$date")
    given RenderCellData[Long] = ltext => div(ltext)
    given RenderCellData[Boolean] = bnumeric => div(s"$bnumeric")
    given RenderCellData[HtmlElement] = el => el
   
  def htmlElement[A](a: A)(using b: RenderCellData[A]): HtmlElement =
    b.celldata(a)

  // type class and givens
  trait CellDataConvertor[A]:
    def celldataList(a: A): List[CellData]

  object CellDataConvertor:
    given CellDataConvertor[Long] = l => List(CellData(l.toString, htmlElement(l)))
    given CellDataConvertor[Int] = i => List(CellData(i.toString, htmlElement(i)))
    given CellDataConvertor[Boolean] = b => List(CellData(b.toString, htmlElement(b)))
    given CellDataConvertor[String] = s => List(CellData(identity(s), htmlElement(s)))
    given CellDataConvertor[LocalDate] = d => List(CellData(d.toString(), htmlElement(d)))
    given CellDataConvertor[LocalDateTime] = d => List(CellData(d.toString(), htmlElement(d)))
    given CellDataConvertor[HtmlElement] = el => List(CellData(text = "", element = el))  
    given CellDataConvertor[Status] = s => List(CellData(text    = Status.toCode(s),element = StatusIcons.render(s)))


  // Option support (generic, but not the shapeless derivation itself)
    given optionCellDataConvertor[T](using ev: CellDataConvertor[T]): CellDataConvertor[Option[T]] =
      case Some(value) => ev.celldataList(value)
      case None        => List(CellData("", div("None")))
    // def deriveShowProduct[A](using
    //       pInst: K0.ProductInstances[CellDataConvertor, A],
    //       labelling: Labelling[A]
    //       ): CellDataConvertor[A] =
    //         (a: A) =>
    //           labelling.elemLabels.zipWithIndex.toList
    //             .flatMap { (_, index) =>
    //               pInst.project(a)(index)(
    //                 [t] => (st: CellDataConvertor[t], pt: t) => st.celldataList(pt)
    //               )
    //             }

    // def deriveShowSum[A](using
    //       inst: K0.CoproductInstances[CellDataConvertor, A]
    //       ): CellDataConvertor[A] =
    //         (a: A) =>
    //           inst.fold(a)(
    //             [a] => (st: CellDataConvertor[a], av: a) => st.celldataList(av)
    //           )

      
    // inline given derived[A](using gen: K0.Generic[A]): CellDataConvertor[A] =
    //     gen.derive(deriveShowProduct, deriveShowSum)

    //   // Option support (fallback UI for None)
    // given optionCellDataConvertor[T](using ev: CellDataConvertor[T]): CellDataConvertor[Option[T]] =
    //     case Some(value) => ev.celldataList(value)
    //     case None        => List(CellData("", div("None")))
