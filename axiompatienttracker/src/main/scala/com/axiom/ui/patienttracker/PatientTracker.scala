package com.axiom.ui.patienttracker

import scala.scalajs.js
import com.axiom.ui.tableutils.*
import com.axiom.model.shared.dto.Patient
import scala.collection.mutable
import com.axiom.ShapelessFieldNameExtractor
import java.time.*
import com.axiom.ui.patienttracker.TypeClass.*
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import com.axiom.ModelFetch
import com.raquo.laminar.api.L
import com.axiom.ModelFetch.headers
import com.raquo.airstream.ownership.OneTimeOwner
import org.scalajs.dom.KeyboardEvent
import io.bullet.borer.derivation.key

type PatientList = CCRowList[Patient]


trait RenderHtml :
  def renderHtml:Element

case class CellData(text:String,color:String) 

//TODO[populate] PatientGridData
case class PatientGridData(grid: PatientTracker,colrow:ColRow, data:CellData) 
    extends GridDataT[PatientTracker,Patient,CellData](grid,colrow,data) with RenderHtml :
  def renderHtml = td(data.text,backgroundColor:=data.color)




class PatientTracker() extends GridT [Patient,CellData] with RenderHtml:

  given owner:Owner = new OneTimeOwner(()=>())
  val selectedCellVar:Var[Option[ColRow]] = Var(None)
  val selectedRowVar:Var[Option[Int]] = Var(None)
  val searchQueryVar: Var[String] = Var("")
  val numColumnsToShow = 10

  selectedCellVar.signal.map {
    case Some(sel) => Some(sel.row)
    case None => None
  }.foreach(selectedRowVar.set)
  
  selectedRowVar.signal.foreach { rowIdxOpt => 
    scrollToSelectedRow(rowIdxOpt)
  }
  
  val colHeadersVar:Var[List[String]] = Var(ShapelessFieldNameExtractor.fieldNames[Patient].slice(1, numColumnsToShow))

  def columns(row:Int,p:Patient) =  
    val c = mutable.IndexedSeq(CellDataConvertor.derived[Patient].celldata(p)*).slice(1 ,numColumnsToShow)
    c(0) = c(0).copy(text = s"${c(0).text}", color = "green")
    c.toList

  override def cctoData(row:Int,cc:Patient):List[CellData] = columns(row,cc)

  // Rudimentary serach filter function, could be made column/data agnostic to be able to use for all columns of the patient data.
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
        // Add a listener to print the input value
        onMountCallback { _ =>
          searchQueryVar.signal.foreach { query =>
            searchFilterFunction() // Example usage of the filter function
          }
        }
      ),
      table(
        onKeyDown --> tableKeyboardHandler,//prevents default scrolling behaviour from various key strokes
        thead(
          children <-- colHeadersVar.signal.map{headerRow(_) }
        ),
        tbody(
          children <-- showGcdVar.signal.map{ 
            (rowList:GCD) => rowList.map(tup => row(tup))
          }
        )
      )
    )
  
  def row(cols:Row)  = 
    tr(
      idAttr := s"row-${cols.head._2.row}",
    backgroundColor <-- selectedRowVar.signal.map{ selRow => 
        selRow match
        case Some(row) if row == cols.head._2.row => "#32a852" //shade of green
          case _ => "black"
      },
      onDblClick --> { _ =>
        val unitNumber = cols.head._3.text // Assuming the first column contains the unit number
        println(s"Row double-clicked: Fetching details for unit number: $unitNumber")
        renderPatientDetailsPage(unitNumber)
      },
      cols.map{c => this.tableCell(c._2)},
      td(
        cls := "details-column",
        button(
          "View Details",
          onClick --> { _ =>
            println(s"Details clicked for row: ${cols.head._3.text}")
            val unitNumber = cols.head._3.text // Assuming the first column contains the unit number
            renderPatientDetailsPage(unitNumber)
          }
        )
      )
    )

  def tableCell(colRow:ColRow) : HtmlElement  =
    td(
      tabIndex := colRow.row*9000 + colRow.col, //apparently I need this capture keyboard events
      onKeyDown --> keyboardHandler,
      onMouseUp.mapTo(colRow).map(Some(_)) --> selectedCellVar.writer,
      data(colRow).map{ gcdTuple =>
        if (gcdTuple._2.col == 3 ){ //Index 3 is for gender, starting index is 0
          if (gcdTuple._3.text == "M" || gcdTuple._3.text == "Male") {
            img(
              src := "https://img.icons8.com/color/48/male.png",
              alt := "Male"
            )
          } else {
            img(
              src := "https://img.icons8.com/color/48/female.png",
              alt := "Female"
            )
          }
        }
        else{
          span(s"${gcdTuple._3.text}") // Wrap the string in a span
        }
      }.getOrElse("---") // Ensure fallback is also wrapped in a span
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


