package org.axiom.ui.oldtable.utils
import org.axiom.ui.oldtable.{Grid,GridData, Data}
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom
import org.axiom.ui.oldtable.utils.given
// import typings.std.stdStrings.text

enum EditorToggleState(colorString:String):
  case UnSelected extends EditorToggleState("green")
  case StateTwo extends EditorToggleState("blue")
  case StateFocused extends EditorToggleState("red")
  case RowSelected extends EditorToggleState("#9F5EB2")
  //Note: you have to surface properties of the enum to the outside
  lazy val color = colorString

import EditorToggleState._  


/**
  * creates interactive edit input that behaves like a cell within a grid of cells (like a spreadsheet)
  *
  * @param gd
  * @return
  */  
def cellTextInput(gd:GridData): HtmlElement = 
  input(
  // backgroundColor <-- gd.toggleState.signal.map(_.color),
  typ := "text",
  onInput.mapToValue --> gd.varDataWriter,
  //TODO ?DELETE THIS -> onInput.mapToValue.map(x => gd.varData.now()).map(data => (data.date.toDateTimeString, data.s).toString()) --> gd.g.summaryText,
  onKeyDown --> (e => 
    def htmlInputFocus(c:Coordinate) =
      gd.g.data(c).foreach{_.inputHtmlElement.ref.focus()  }
    e.keyCode match
    case 40 =>  //down cursor
      // dom.window.console.log(s"down ${gd.coordinate}down*down*down* ")
      htmlInputFocus( gd.coordinate.addY(1) )
    case 38 => //up cursor
      htmlInputFocus( gd.coordinate.addY(-1) )
    case 37 => //left cursor
      htmlInputFocus( gd.coordinate.addX(-1) )
    case 39 => //right cursor
      htmlInputFocus( gd.coordinate.addX(+1) )
    case 9 => //tab
      dom.window.console.log(s"tabbed ${gd.coordinate}tab tab tab ")

    case _ => ()
    ),
    onFocus --> (e => 
      gd.g.focusedCoodinate.update(_ => Some(gd.coordinate) )
      selectedRow(gd).foreach(d => d.toggleState.update(_ => RowSelected))
      gd.toggleState.update(_ => StateFocused)
      // gd.g.varHeaders.now()(gd.coordinate.x).selected.update(_ => true)
      gd.g.headersSignal.map (list => list(gd.coordinate.x).selected.update(_ => true))
      gd.g.summaryText.set(gd.varData.now().toString())
    ),
    onBlur --> (e => //focus out
      gd.toggleState.update(_ => UnSelected)
      selectedRow(gd).foreach(d => d.toggleState.update(_ => UnSelected))
      gd.g.headersSignal.map (list => list(gd.coordinate.x).selected.update(_ => false))
      // gd.g.varHeaders(gd.coordinate.x).selected.update(_ => false)
    ) ,
  )


/**
  * determines row coordinate of selected cell and generates list of GridData 
  * that exists along tha row.
  *
  * @param gd
  * @return List[GridData]
  */  
def selectedRow(gd:GridData) : List[GridData]=
    gd.g.xRange.toList
      .filter(_ != gd.x)
      .map{x =>gd.g.data(x,gd.y) }
      .filter(d => d.isDefined)
      .map(_.get)
