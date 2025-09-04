package com.axiom.ui.patienttracker
import shapeless3.deriving.*
import java.time.*
import org.scalajs.dom.{HTMLHtmlElement}
import com.raquo.laminar.api.L.{*, given}
import org.w3c.dom.Text

object TypeClass :
  

  trait RenderCellData[A]:

    def celldata(a:A): HtmlElement

  object RenderCellData:
    given RenderCellData[String] = text =>  div(  text  )
    given RenderCellData[Int] = numeric =>  div(  s"$numeric"  )
    given RenderCellData[LocalDateTime] = datetime =>  div(  s"$datetime"  )
    given RenderCellData[LocalDate] = date =>  div(  s"$date"  )
    given RenderCellData[Long] = ltext =>  div(  ltext  )
    given RenderCellData[Boolean] = bnumeric =>  div(  s"$bnumeric"  )

  // type class and givens
  trait CellDataConvertor[A]:
    def celldata(a: A):List[CellData]

  def f[A](a:A)(using b:RenderCellData[A])  :HtmlElement =
    b.celldata(a)


  object CellDataConvertor:
    given CellDataConvertor[Long] =         l => List(CellData(l.toString,"green", f(l)))
    given CellDataConvertor[Int] =         i => List(CellData(i.toString,"black",f(i)))
    given CellDataConvertor[Boolean] =     b => List(CellData(b.toString,"blue",f(b)))
    given CellDataConvertor[String] =      s =>  List(CellData(identity(s),"#3361ff",f(s)))
    given CellDataConvertor[LocalDate] =   d => List(CellData(d.toString(),"yellow",f(d)))
    given CellDataConvertor[LocalDateTime] = d => List(CellData(d.toString(),"cyan",f(d)))

    def deriveShowProduct[A](using
      pInst: K0.ProductInstances[CellDataConvertor, A],
      labelling: Labelling[A]
      ): CellDataConvertor[A] =
        (a: A) =>
          val properties = labelling.elemLabels.zipWithIndex
            .map { (label, index) =>
              val value = pInst.project(a)(index)([t] => (st: CellDataConvertor[t], pt: t) => st.celldata(pt))
              value
            }.foldLeft(List.empty[CellData])((a,b) =>a ++ b)
            properties
            


    def deriveShowSum[A](using
        inst: K0.CoproductInstances[CellDataConvertor, A]
    ): CellDataConvertor[A] =
      (a: A) => inst.fold(a)([a] => (st: CellDataConvertor[a], a: a) => st.celldata(a))

    inline given derived[A](using gen: K0.Generic[A]): CellDataConvertor[A] =
      gen.derive(deriveShowProduct, deriveShowSum)

    given optionCellDataConvertor[T](using ev: CellDataConvertor[T]): CellDataConvertor[Option[T]] =
      case Some(value) => ev.celldata(value)
      case None        => List(CellData("", "gray",div("None"))) // Or choose a suitable fallback
      
  
  //   trait TableCellRenderer[A]:

  //     def render(a: A, pos: ColRow): HtmlElement

  // // Rendering using contextual abstraction
  // object TableCellRenderer:

  //   // main renderer for cell (status in col 0, text elsewhere)
  //   given TableCellRenderer[Cell] with
  //     def render(cell: Cell, pos: ColRow): HtmlElement =
  //       if pos.col == 0 then
  //         div(
  //           cls := "status-column",
  //           renderStatusIcon(utils.PatientStatus.fromString(cell.data.text))
  //         )
  //       else
  //         span(cell.data.text)

  //   // Option support
  //   given [A](using r: TableCellRenderer[A]): TableCellRenderer[Option[A]] with
  //     def render(opt: Option[A], pos: ColRow): HtmlElement =
  //       opt match
  //         case Some(a) => r.render(a, pos)
  //         case None    => span("—")

  
  // def renderCell[A](a: A, pos: ColRow)(using r: TableCellRenderer[A]): HtmlElement =
  //   r.render(a, pos)
  //     def deriveShowProduct[A](using
  //       pInst: K0.ProductInstances[CellDataConvertor, A],
  //       labelling: Labelling[A]
  //       ): CellDataConvertor[A] = 
  //         (a: A) =>
  //           val properties = labelling.elemLabels.zipWithIndex
  //             .map { (label, index) =>
  //               val value = pInst.project(a)(index)([t] => (st: CellDataConvertor[t], pt: t) => st.celldata(pt))
  //               value
  //             }.foldLeft(List.empty[CellData])((a,b) =>a ++ b)
  //             properties
