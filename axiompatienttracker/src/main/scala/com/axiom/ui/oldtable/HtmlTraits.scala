package org.axiom.ui.oldtable.utils

import org.axiom.ui.oldtable.{Grid,GridData,Header}
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

trait HtmlAble[T]:
  extension (t:T) 
    def htmlElement:HtmlElement


given HtmlAble[Header] with 
  extension(h:Header)
    def htmlElement: HtmlElement = th(h.header,
      backgroundColor <-- h.selected.signal.map( _ match
          case true => "orange"
          case false => "purple"
      )
    )

given HtmlAble[GridData] with
  extension(gd: GridData) 
    def htmlElement: HtmlElement = cellTextInput(gd)
      


given HtmlAble[Grid] with
  extension(g: Grid) 
    def htmlElement: HtmlElement = 
      def row(y:Int) = g.xRange.map(x => td(g.data(x,y).map( x => x.inputHtmlElement  ).getOrElse(div("erXror")))) 
      def rows = g.yRange.map(y => tr(row(y)))
      def headers = g.varHeaders.now().map{_.htmlElement }

      table(
          thead(headers*),
          tbody(rows*)
      )
