package com.axiom.ui.patienttracker

import scala.scalajs.js
import com.axiom.ui.tableutils.*
import com.axiom.ui.tableutils.GridT
import com.axiom.model.shared.dto.Patient
import scala.collection.mutable
import com.axiom.ShapelessFieldNameExtractor
import java.time.*
import com.axiom.ui.patienttracker.TypeClass.*
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.axiom.ModelFetch
import com.raquo.laminar.api.L
import com.axiom.ModelFetch.columnHeaders
import com.axiom.ui.patienttracker.PatientStatusIcons.renderStatusIcon 
import com.axiom.ui.patienttracker.KeyboardNavHelper

import com.raquo.airstream.ownership.OneTimeOwner
import org.scalajs.dom.KeyboardEvent
import io.bullet.borer.derivation.key
import scala.concurrent.ExecutionContext.Implicits.global
import com.axiom.ui.patienttracker.SearchBar

type PatientList = CCRowList[Patient]

trait RenderHtml :
  def renderHtml:Element

case class CellData(text:String,color:String) 

case class PatientGridData(grid: PatientTracker,colrow:ColRow, data:CellData) 
    extends GridDataT[PatientTracker,Patient,CellData](grid,colrow,data) with RenderHtml :
  def renderHtml = td(data.text,backgroundColor:=data.color)


class PatientTracker() extends GridT [Patient,CellData] with RenderHtml:

  given owner:Owner = new OneTimeOwner(()=>())
  val selectedCellVar:Var[Option[ColRow]] = Var(None)
  val selectedRowVar:Var[Option[Int]] = Var(None)
  val searchQueryVar: Var[String] = Var("")
  val numColumnsToShow = 10
  searchQueryVar.signal.foreach { _ =>
      searchFilterFunction()
    }
  
  // Flag For create patient form
  val showCreatePatientForm = Var(false)
  def openCreatePatientModal(): Unit = showCreatePatientForm.set(true)
  def closeCreatePatientModal(): Unit = showCreatePatientForm.set(false)

  val colsToRemove = Set("hcn", "dob") // Use Set for faster lookup
  val colHeadersVar: Var[List[String]] = {
    val headers = ShapelessFieldNameExtractor.fieldNames[Patient].slice(1, numColumnsToShow)
    val newHeaders = "STATUS" :: headers.toList
    Var(newHeaders.filterNot(name => colsToRemove.contains(name)))
  }
  

  selectedCellVar.signal.map {
      case Some(sel) => Some(sel.row)
    case None => None
  }.foreach(selectedRowVar.set)

  selectedRowVar.signal.foreach { rowIdxOpt =>
    scrollToSelectedRow(rowIdxOpt)
  }
  

  //Create Patient Form input variables for dynamic patient creation
  case class FormState(
  firstNameVar: Var[String] = Var(""),
  lastNameVar: Var[String] = Var(""),
  unitNumberVar: Var[String] = Var(""),
  accountNumberVar: Var[String] = Var(""),
  sexVar: Var[String] = Var(""),
  dobVar: Var[String] = Var(""),
  admitDateVar: Var[String] = Var(""),
  floorVar: Var[String] = Var(""),
  roomVar: Var[String] = Var(""),
  bedVar: Var[String] = Var(""),
  hospVar: Var[String] = Var(""),
  auroraFileVar: Var[String] = Var("")
)
  lazy val createPatientFormState = FormState()

  def getSpecificCellData(columnName: String, p: Patient): CellData = {
    // Get original headers and cell data
    val headers = ShapelessFieldNameExtractor.fieldNames[Patient]
    val cellData = mutable.IndexedSeq(CellDataConvertor.derived[Patient].celldata(p)*)
    val columnIndexOpt = headers.indexOf(columnName) match
      case -1 => None
      case i  => Some(i)

    // Get the cell data for the specific column
    val specificCellData = columnIndexOpt
      .filter(_ < cellData.length) // Ensure the index is within bounds
      .map(cellData(_))
      .getOrElse(CellData("", "")) // Default CellData if column not found or out of bounds
    specificCellData
  }

  def columns(row: Int, p: Patient) =
    // Get original headers and cell data
    val headers = ShapelessFieldNameExtractor.fieldNames[Patient]
    val cellData = mutable.IndexedSeq(CellDataConvertor.derived[Patient].celldata(p)*).slice(1, numColumnsToShow)

    // Get the cell data for the specific column
    val statusCellData = getSpecificCellData("flag", p)
    val newCellData = List(statusCellData) ++ cellData.toList
    val zipped = headers.zip(newCellData)

    // Filter out the column with name "hcn"
    val filtered = zipped.filterNot { case (name, _) => colsToRemove.contains(name) }
    val filteredCellData = filtered.map(_._2)
    filteredCellData

  override def cctoData(row:Int,cc:Patient):List[CellData] = columns(row,cc)

  // Rudimentary search filter function, could be made column/data agnostic to be able to use for all columns of the patient data.
   def searchFilterFunction(): Unit = {
    val query = searchQueryVar.now().toLowerCase.trim
    println(s"Searching for: $query")
    // Filter rows where any cell in the row contains the query
    val filteredPatients = gcdVar.now().filter { row =>
      row.exists { case (_, _, data) =>
        val cellData = data.asInstanceOf[CellData]
        cellData.text.toLowerCase.contains(query)
      }
    }
    showGcdVar.set(filteredPatients.filter(_.nonEmpty))
  }

  def scrollToSelectedRow(rowIdxOpt: Option[Int]): Unit = {
    rowIdxOpt match {
      case Some(rowIdx) =>
        Option(dom.document.getElementById(s"row-$rowIdx")).foreach { element =>
          val rect = element.getBoundingClientRect()
          val isInView = rect.top >= 0 && rect.bottom <= dom.window.innerHeight

          if (!isInView) {
            element.asInstanceOf[js.Dynamic].scrollIntoView(
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
  
  def renderHtml: L.Element =
    def headerRow(s:List[String]) = 
      List(tr(
          (s :+ "Details").map (s => { // Add Details column header
            th(s, padding := "8px")
          })
        )
      )

    div(
       cls := "table-container", // Wrapper for both search bar and table
      // Modularized SearchBar
      SearchBar(searchQueryVar, openCreatePatientModal),
      div(
        cls := "table-scroll-body",
        table(
          onKeyDown --> tableKeyboardHandler,
          thead(
            children <-- colHeadersVar.signal.map { headerRow(_) }
          ),
          tbody(
            children <-- showGcdVar.signal.map { rowList =>
              rowList.map(tup => row(tup))
            }
          )
        )
      ),
      PatientActions.createPatientForm(createPatientFormState, showCreatePatientForm, () => closeCreatePatientModal())
    )

  def row(cols: Row): HtmlElement = {
    val showConfirm = Var(false)
    val rowIdx = cols.head._2.row //Extracted Once for consistennt row ID reference
    val unitNumber = cols(1)._3.text //Making it globally available

    tr(
      idAttr := s"row-$rowIdx",
      backgroundColor <-- selectedRowVar.signal.map {
        case Some(row) if row == rowIdx => "#32a852" //shade of green
        case _ => "black"
      },
      onDblClick --> { _ => 
        // Assuming the second column contains the unit number
        println(s"Row double-clicked: Fetching details for unit number: $unitNumber")
        renderPatientDetailsPage(unitNumber) 
      },
      cols.map(c => tableCell(c._2)),
      PatientActions.renderActionButtons(unitNumber) //Helper Function to render the View Details and Edit Buttons
    )
  }

  def tableCell(colRow: ColRow): HtmlElement =
    td(
      tabIndex := colRow.row * 9000 + colRow.col, // apparently I need this capture keyboard events
      onKeyDown --> keyboardHandler,
      onMouseUp.mapTo(colRow).map(Some(_)) --> selectedCellVar.writer,
      data(colRow)
        .map { gcdTuple =>
           if (colRow.col == 0)
              renderStatusIcon(PatientStatus.fromString(gcdTuple._3.text)) // Helper Function to render the status Icons
           else
            span(gcdTuple._3.text)
        }
      .getOrElse("---")
  )
          
  /**
    * event handler at the table later to prevent default behaviour from key actions
    * that can cause the web page to scroll
    *
    * @param e
    */
  def tableKeyboardHandler(e:KeyboardEvent)  =
    e.keyCode match
      case 40 | 38 => e.preventDefault() // Prevent default scrolling behavior for up/down arrows
      case _  => ()  


// Key press state
  val navHelper = new KeyboardNavHelper(moveAndScroll)

  def keyboardHandler(e: KeyboardEvent): 
    Unit = navHelper.keyboardHandler(e)
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

