package com.axiom.ui

import com.raquo.laminar.api.L._
import com.axiom.shared.table._
import com.axiom.shared.table.TypeClass.CellDataConvertor
import com.axiom.shared.table.TableDerivation.given
import shapeless3.deriving.Labelling   
import org.scalajs.dom
import com.axiom.ui.ContextMenu

final class ReactiveTable[T <: Product](using conv: CellDataConvertor[T],lab: Labelling[T]) extends GridT[T, CellData]:

  // headers  from shapeless Labelling
  private lazy val headers: List[String] = lab.elemLabels.toList

  override def cctoData(idx: Int, cc: T): List[CellData] =
    conv.celldataList(cc).toList

  
  def render(
    onRowClick: Option[Int => Unit] = None,
    onRowContextMenu: Option[(Int, dom.MouseEvent) => Unit]= None
    ): Element =
    div(
      cls := "table-container",
      table(
        cls := "my-table",
        thead(tr(headers.map(h => th(h)))),
        tbody(
          children <-- showGcdVar.signal.map { rows =>
            rows.toList.zipWithIndex.map { case (row, i) =>
              tr(
                onClick.mapTo(i) --> { idx => onRowClick.foreach(_(idx)) },
                onContextMenu.preventDefault --> {e => onRowContextMenu.foreach(_(i, e))},
                row.map(c => td(c.data.element))
              )
            }
          }
        )
      )
    )
