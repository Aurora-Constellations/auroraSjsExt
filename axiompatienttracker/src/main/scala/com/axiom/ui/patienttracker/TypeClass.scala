package com.axiom.ui.patienttracker

import shapeless3.deriving.*
import java.time.*
import com.raquo.laminar.api.L.{*, given}
import com.axiom.ui.patienttracker.utils.{PatientStatus, PatientStatusIcons}

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

  // type class and givens
  trait CellDataConvertor[A]:
    def celldata(a: A): List[CellData]

  def f[A](a: A)(using b: RenderCellData[A]): HtmlElement =
    b.celldata(a)

  object CellDataConvertor:
    given CellDataConvertor[Long] = l => List(CellData(l.toString, f(l)))
    given CellDataConvertor[Int] = i => List(CellData(i.toString, f(i)))
    given CellDataConvertor[Boolean] = b => List(CellData(b.toString, f(b)))
    given CellDataConvertor[String] = s => List(CellData(identity(s), f(s)))
    given CellDataConvertor[LocalDate] = d => List(CellData(d.toString(), f(d)))
    given CellDataConvertor[LocalDateTime] = d => List(CellData(d.toString(), f(d)))
    given CellDataConvertor[HtmlElement] = el => List(CellData(text = "", element = el))
    def deriveShowProduct[A](using
        pInst: K0.ProductInstances[CellDataConvertor, A],
        labelling: Labelling[A]
    ): CellDataConvertor[A] =
      (a: A) =>
        val properties: List[CellData] =
          labelling.elemLabels.zipWithIndex.toList // <— ensure List here
            .flatMap { (label, index) =>
              val base: List[CellData] =
                pInst.project(a)(index)([t] => (st: CellDataConvertor[t], pt: t) => st.celldata(pt))

              label match
                case "flag" | "status" =>
                  base.map { cd =>
                    cd.copy(element =
                      PatientStatusIcons.renderStatusIcon(
                        PatientStatus.fromString(cd.text)
                      )
                    )
                  }
                case _ =>
                  base
            }
        properties

    def deriveShowSum[A](using
        inst: K0.CoproductInstances[CellDataConvertor, A]
    ): CellDataConvertor[A] =
      (a: A) => inst.fold(a)([a] => (st: CellDataConvertor[a], a: a) => st.celldata(a))

    inline given derived[A](using gen: K0.Generic[A]): CellDataConvertor[A] =
      gen.derive(deriveShowProduct, deriveShowSum)

    given optionCellDataConvertor[T](using ev: CellDataConvertor[T]): CellDataConvertor[Option[T]] =
      case Some(value) => ev.celldata(value)
      case None        => List(CellData("", div("None"))) // Or choose a suitable fallback
