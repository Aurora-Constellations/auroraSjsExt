package com.axiom.ui.patienttracker

import scala.scalajs.js
import com.axiom.ui.tableutils.*
import com.axiom.ui.tableutils.GridT
import com.axiom.model.shared.dto.Patient
import com.axiom.ShapelessFieldNameExtractor
import com.axiom.ui.patienttracker.TypeClass.*
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.axiom.ModelFetch
import com.axiom.ModelFetch.columnHeaders
import com.axiom.ui.patienttracker.utils.PatientStatusIcons.renderStatusIcon
import com.axiom.ui.patienttracker.utils.KeyboardNavigation
import com.axiom.AxiomPatientTracker.PatientUI
import com.raquo.airstream.ownership.OneTimeOwner
import org.scalajs.dom.KeyboardEvent
import scala.concurrent.ExecutionContext.Implicits.global
import com.axiom.ui.patienttracker.utils.SearchBar
import com.axiom.ui.patienttracker.utils.DataProcessing.*
import com.axiom.shared.table.TableDerivation
import com.axiom.shared.table.TableDerivation.given
import com.axiom.shared.table.{CCRowList, ColRow, GridDataT, GridT}


type PatientList = CCRowList[Patient]

trait RenderHtml:
  def renderHtml: Element

case class CellData(text: String, element: HtmlElement)

case class PatientGridData(grid: PatientTracker, colrow: ColRow, data: CellData)
    extends GridDataT[PatientTracker, PatientUI, CellData](grid, colrow, data)
    with RenderHtml:
  def renderHtml = td(data.text)

class PatientTracker() extends GridT[PatientUI, CellData] with RenderHtml:

  given owner: Owner = new OneTimeOwner(() => ())
  val selectedCellVar: Var[Option[ColRow]] = Var(None)
  val selectedRowVar: Var[Option[Int]] = Var(None)
  val searchQueryVar: Var[String] = Var("")

  lazy val createPatientFormState = FormState()
  // Flag For create patient form
  val showCreatePatientForm = Var(false)
  def openCreatePatientModal(): Unit = showCreatePatientForm.set(true)
  def closeCreatePatientModal(): Unit = showCreatePatientForm.set(false)

  // hide these everywhere
  private val removedCols: Set[String] = Set("hcn", "dob")

  // one source of headers
  private lazy val allHeaders: List[String] =
    ShapelessFieldNameExtractor.fieldNames[PatientUI]

  private lazy val visibleHeaders: List[String] =
    allHeaders.filterNot(removedCols)

  private lazy val visibleIndex: Map[String, Int] =
    visibleHeaders.zipWithIndex.toMap

  // small helper to read a cell's text by column name
  private def textAt(colName: String)(cols: Row): String =
    visibleIndex
      .get(colName)
      .flatMap(i => cols.lift(i))
      .map(_.data.text)
      .getOrElse("")

  selectedCellVar.signal
    .map {
      case Some(sel) => Some(sel.row)
      case None      => None
    }
    .foreach(selectedRowVar.set)

  selectedRowVar.signal.foreach { rowIdxOpt =>
    scrollToSelectedRow(rowIdxOpt)
  }

  // cells for a row
  private def cellsOf(p: PatientUI) =
    CellDataConvertor.derived[PatientUI].celldata(p).toVector

  private def visibleCellsOf(p: PatientUI): List[CellData] =
    allHeaders.zip(cellsOf(p)).collect { case (h, cell) if !removedCols(h) => cell }

  // FIXED columns
  def columns(row: Int, p: PatientUI): List[CellData] = {
    (allHeaders zip cellsOf(p))
      .collect { case (name, cell) if !removedCols(name) => cell }
  }
      
  override def cctoData(row: Int, cc: PatientRow): List[CellData] = columns(row, cc)

  def scrollToSelectedRow(rowIdxOpt: Option[Int]): Unit = {
    rowIdxOpt match {
      case Some(rowIdx) =>
        Option(dom.document.getElementById(s"row-$rowIdx")).foreach { element =>
          val rect = element.getBoundingClientRect()
          val isInView = rect.top >= 0 && rect.bottom <= dom.window.innerHeight

          if (!isInView) {
            element
              .asInstanceOf[js.Dynamic]
              .scrollIntoView(
                js.Dynamic.literal(
                  behavior = "smooth",
                  block = "nearest"
                )
              )
          }
        }
      case None => // Do nothing
    }
  }

  def renderHtml: Element =
    def headerRow(s: List[String]) =
      List(tr(s.map(name => th(name, padding := "8px"))))
    div(
      cls := "table-container", // Wrapper for both search bar and table
      // Modularized SearchBar
      SearchBar(searchQueryVar, openCreatePatientModal),
      div(
        cls := "table-scroll-body",
        table(
          onKeyDown --> tableKeyboardHandler,
          thead(
            children <-- Val(visibleHeaders).map { hs =>
              List(tr(hs.map(h => th(h))))
            }
          ),
          tbody(
            // Added the filter functionality we used under the searchFilterFunction() previously
            children <-- gcdVar.signal
              .combineWithFn(searchQueryVar.signal) { (rows, q0) =>
                val q = q0.trim.toLowerCase
                if (q.isEmpty) rows
                else
                  rows.filter { rowCols =>
                    rowCols.exists(cell => cell.data.text.toLowerCase.contains(q))
                  }
              }
              .map(_.map(row(_)))
          )
        )
      ),
      PatientFormModel.create(createPatientFormState, showCreatePatientForm, () => closeCreatePatientModal())
    )

  def row(cols: Row): HtmlElement = {
    val rowIdx = cols.headOption.map(_.position.row).getOrElse(-1)
    val unitNumber = textAt("unitNumber")(cols)
    tr(
      idAttr := s"row-$rowIdx",
      backgroundColor <-- selectedRowVar.signal.map {
        case Some(row) if row == rowIdx => "#32a852" // shade of green
        case _                          => "black"
      },
      onDblClick --> { _ =>
        // Assuming the third column contains the unit number
        println(s"Row double-clicked: Fetching details for unit number: $unitNumber")
        renderPatientDetailsPage(unitNumber)
      },
      cols.map(c => tableCell(c.position))
    )
  }

  def tableCell(colRow: ColRow): HtmlElement =
    td(
      tabIndex := colRow.row * 9000 + colRow.col, // apparently I need this capture keyboard events
      onKeyDown --> keyboardHandler,
      onMouseUp.mapTo(colRow).map(Some(_)) --> selectedCellVar.writer,
      data(colRow)
        .map { cell => (cell.data.element) }
        .getOrElse("---")
    )

  /** event handler at the table later to prevent default behaviour from key actions that can cause the web page to
    * scroll
    *
    * @param e
    */
  def tableKeyboardHandler(e: KeyboardEvent) =
    e.keyCode match
      case 40 | 38 => e.preventDefault() // Prevent default scrolling behavior for up/down arrows
      case _       => ()
      // TODO Keyboard number mapping to the events
      // TODO ?SHOULD this be default behaviour on most tables?  if so put it in tableutils with a way to override

// Key press state
  val navHelper = new KeyboardNavigation(moveAndScroll)

  def keyboardHandler(e: KeyboardEvent): Unit = navHelper.keyboardHandler(e)
  def startKeyPressHandler(keyCode: Int, action: () => Unit): Unit =
    navHelper.startKeyPressHandler(keyCode, action)
  def stopKeyPressHandler(e: KeyboardEvent): Unit =
    navHelper.stopKeyPressHandler(e)

  // Add event listeners for keyup to stop the interval
  dom.window.addEventListener("keyup", (e: KeyboardEvent) => stopKeyPressHandler(e))

  // Move the selected row and scroll the page
  private def moveAndScroll(step: Int): Unit = {
    val currentRowOpt = selectedRowVar.now()
    val totalRows = gcdVar.now().size

    currentRowOpt.foreach { currentRow =>
      val newRow = (currentRow + step).max(0).min(totalRows - 1) // Ensure bounds
      if (newRow != currentRow) {
        selectedRowVar.set(Some(newRow))
        scrollToSelectedRow(Some(newRow))
      }
    }
  }
  // inside class PatientTracker
  def refreshAndKeepSearch(newPatients: List[PatientUI]): Unit = {
    val q = searchQueryVar.now() // remember current search text
    populate(newPatients) // replace underlying rows (gcdVar)
    searchQueryVar.set(q) // restore search text
  }

  // Helper function to render "View Details" and "Edit" actions on the patient tracker
  def renderActionButtons(unitNumber: String): HtmlElement =
    div(
      cls := "details-column",
      button(
        "View Details",
        marginRight := "8px",
        onClick --> { _ => renderPatientDetailsPage(unitNumber) }
      ),
      button(
        "Edit",
        onClick --> { _ => renderPatientDetailsPage(unitNumber, editable = true) }
      )
    )
