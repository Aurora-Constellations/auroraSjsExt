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


import com.raquo.airstream.ownership.OneTimeOwner
import org.scalajs.dom.KeyboardEvent
import io.bullet.borer.derivation.key
import scala.concurrent.ExecutionContext.Implicits.global


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
      cls := "table-container",  // Wrapper for both search bar and table
      div(
        cls := "search-bar",
        // First span with the "Patient list" title
        span(cls := "patient-title", "Patient list"),  // First title span
        // Search bar
        label("Search: "),
        marginBottom := "10px",
        input(
          typ := "text",
          placeholder := "Search patients here...",
          inContext { thisNode =>
            onInput.mapTo(thisNode.ref.value) --> searchQueryVar
          }


        ),
        button(
            "Create Patient",
            cls := "create-patient-button",
            onClick --> { _ => 
            println("Create Patient button clicked")
            openCreatePatientModal() }, 
        ),
              // Add a listener to print the input value
              onMountCallback { _ =>
                searchQueryVar.signal.foreach { query =>
                  searchFilterFunction()  // Example usage of the filter function
                }
              }
      
            ),
            div(
              cls := "table-scroll-body",
              table(
                onKeyDown --> tableKeyboardHandler,
                thead(
                children <-- colHeadersVar.signal.map{headerRow(_) }
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


  def row(cols:Row)  = {

    val showConfirm = Var(false)
    tr(
      
      idAttr := s"row-${cols.head._2.row}",
      
    backgroundColor <-- selectedRowVar.signal.map{ selRow => 
        selRow match
        case Some(row) if row == cols.head._2.row => "#32a852" //shade of green
          case _ => "black"
      },
      onDblClick --> { _ =>
        val unitNumber = cols(1)._3.text
         // Assuming the second column contains the unit number
        println(s"Row double-clicked: Fetching details for unit number: $unitNumber")
        renderPatientDetailsPage(unitNumber)
      },
      cols.map { c => this.tableCell(c._2) },
      
      td(
        cls := "details-column",
        button(
          "View Details",
          marginRight := "8px",
          onClick --> { _ =>
            println(s"Details clicked for row: ${cols(1)._3.text}")
            val unitNumber = cols(1)._3.text // Assuming the second column contains the unit number
            renderPatientDetailsPage(unitNumber)
          }
        ),
        button(
          "Edit",
           onClick --> { _ =>
              val unitNumber = cols(1)._3.text
              renderPatientDetailsPage(unitNumber, editable = true)
            }
          )
        )
        )
  }

  def tableCell(colRow: ColRow): HtmlElement =
    td(
      tabIndex := colRow.row * 9000 + colRow.col, // apparently I need this capture keyboard events
      onKeyDown --> keyboardHandler,
      onMouseUp.mapTo(colRow).map(Some(_)) --> selectedCellVar.writer,
      data(colRow)
        .map { gcdTuple =>
          if (colRow.col == 0) {
            if (gcdTuple._3.text == "0") then
              img(
                src := "https://img.icons8.com/material-rounded/24/40C057/checked-checkbox.png",
                alt := "Stable",
                width := "20px",
                height := "20px"
              )
            else if (gcdTuple._3.text == "1") then // Urgency
              img(
                src := "https://img.icons8.com/fluency-systems-filled/48/FA5252/leave.png",
                alt := "Urgency",
                width := "20px",
                height := "20px"
              )
            else if (gcdTuple._3.text == "2") then // Draft
              img(
                src := "https://img.icons8.com/ios-filled/50/FAB005/create-new.png",
                alt := "Draft",
                width := "20px",
                height := "20px"
              )
            else if (gcdTuple._3.text == "12") then // Urgency + Draft
              div(
                cls := "status-column",
                img(
                  src := "https://img.icons8.com/fluency-systems-filled/48/FA5252/leave.png",
                  alt := "Urgency",
                  width := "20px",
                  height := "20px"
                ),
                img(
                  src := "https://img.icons8.com/ios-filled/50/FAB005/create-new.png",
                  alt := "Draft",
                  width := "20px",
                  height := "20px"
                )
              )
            else
              img(
                src := "https://img.icons8.com/material-rounded/24/40C057/checked-checkbox.png",
                alt := "Stable",
                width := "20px",
                height := "20px"
              )
          } else
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

    

  // Variables to track long key press
  private var keyPressInterval: Option[js.timers.SetIntervalHandle] = None
  private var activeKey: Option[Int] = None // Track the currently pressed key

  def keyboardHandler(e: KeyboardEvent): Unit = {
    val selectedCellOpt = selectedCellVar.now()

    def conditionalUpdate(vector: ColRow): Unit =
      selectedCellOpt.foreach { currentColRow =>
        val newColRow = currentColRow.add(vector)
        if (inBounds(newColRow)) {
          selectedCellVar.set(Some(newColRow))
          scrollToSelectedRow(Some(newColRow.row)) // Ensure scrolling happens
        }
      }

    e.keyCode match {
      case 40 => // Down arrow
        startKeyPressHandler(e.keyCode, () => moveAndScroll(1)) // Move down
      case 38 => // Up arrow
        startKeyPressHandler(e.keyCode, () => moveAndScroll(-1)) // Move up
      case _ =>
    }
  }

  // Start handling long key press
  private def startKeyPressHandler(keyCode: Int, action: () => Unit): Unit = {
    // If the key is already active, do nothing
    if (activeKey.contains(keyCode)) return

    // Mark the key as active
    activeKey = Some(keyCode)

    // Execute the action immediately
    action()

    // Clear any existing interval
    keyPressInterval.foreach(js.timers.clearInterval)

    // Start a new interval for continuous execution
    keyPressInterval = Some(js.timers.setInterval(100)(action()))
  }

  // Stop handling long key press
  def stopKeyPressHandler(e: KeyboardEvent): Unit = {
    // Only stop if the released key matches the active key
    if (activeKey.contains(e.keyCode)) {
      keyPressInterval.foreach(js.timers.clearInterval)
      keyPressInterval = None
      activeKey = None
    }
  }

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

