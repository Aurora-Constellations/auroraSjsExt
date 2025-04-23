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
    c(0) = c(0).copy(text = s"*${c(0).text}*", color = "green")
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
    cols.map{c => this.tableCell(c._2)},
    td(
      cls := "details-column",
      button(
        "View Details",
        onClick --> { _ =>
          println(s"Details clicked for row: ${cols.head._3.text}")
          // TODO: Add your logic here for handling the "Details" button click
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
      case 40 => e.preventDefault()
      case 38 => e.preventDefault()
      case 37 => e.preventDefault()
      case 39 => e.preventDefault()
      case 32 => e.preventDefault()
      case _  => ()  

    

  def keyboardHandler(e:KeyboardEvent)  =
    val selectedCellOpt = selectedCellVar.now()
    def conditionalUpdate(vector:ColRow):Unit =
      selectedCellOpt.foreach {currentColRow =>
        val newColRow = currentColRow.add(vector)
        inBounds(newColRow) match
          case true => selectedCellVar.set(Some(newColRow))
          case _ => ()
      }
    e.keyCode match
      case 40 =>  //down cursor
        conditionalUpdate(ColRow(0,1))
      case 38 => //up cursor
        conditionalUpdate(ColRow(0,-1))
      case 37 => //left cursor
        conditionalUpdate(ColRow(-1,0))
      case 39 => //right cursor
        conditionalUpdate(ColRow(-1,0))
      case 9 => //tab
        // dom.window.console.log(s"tabbed ${gd.coordinate}tab tab tab ")
      case _ => ()


