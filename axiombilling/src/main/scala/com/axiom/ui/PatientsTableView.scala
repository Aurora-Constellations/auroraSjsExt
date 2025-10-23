package com.axiom.ui

import scala.scalajs.js
import com.axiom.shared.table._ 
import com.axiom.shared.table.GridT
import scala.concurrent.ExecutionContext.Implicits.global
import com.axiom.shared.table.TableDerivation
import com.axiom.shared.table.TableDerivation.given
import com.axiom.shared.table.{CCRowList, ColRow, GridDataT, GridT}
import com.axiom.model.shared.dto.{Patient, Billing, Account, DiagnosticCodes, Doctor, BillingCode, Encounter}
// import org.w3c.dom.Element
import BillingUIRenderer.BPatientRow
import com.raquo.laminar.api.L._


type PatientList = CCRowList[Patient]

trait RenderHtml:
  def renderHtml: Element


final class BillingTracker extends GridT[BPatientRow, CellData] with RenderHtml:

  private lazy val allHeaders: List[String] =
    ShapelessFieldNameExtractor.fieldNames[BPatientRow]

  private def visibleCells(r: BPatientRow): List[String] = allHeaders 

  private def cellsOf(r: BPatientRow): Vector[CellData] =
    TableDerivation.derived[BPatientRow].celldataList(r).toVector
  override def cctoData(rowIdx: Int, row: BPatientRow): List[CellData] =
    cellsOf(row).toList

  def renderHtml: Element =
    div(
  cls := "table-container",
  table(
    cls := "my-table",
    thead(
      tr(allHeaders.map(h => th(h)))
    ),
    tbody(
      children <-- showGcdVar.signal.map { rows =>
        rows.toList.map(row =>
          tr(
            row.map(c =>
              td(c.data.element)
            )
          )
        )
      }
    )
  )
)
