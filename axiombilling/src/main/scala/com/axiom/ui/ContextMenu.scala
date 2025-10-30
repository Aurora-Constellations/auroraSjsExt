package com.axiom.ui

import com.raquo.laminar.api.L._

object ContextMenu:

    final case class Item(label: String, onClick: () => Unit)
    final case class State(x: Double, y: Double, items: List[Item])

    private val stateVar: Var[Option[State]] = Var(None)

    def show(x: Double, y:Double , items: List[Item]): Unit =
        stateVar.set(Some(State(x,y,items)))

    def hide(): Unit = stateVar.set(None)

    def view: Element =
        div(
      display <-- stateVar.signal.map(_.fold("none")(_ => "block")),
      cls := "ctx-overlay",
      onClick --> (_ => hide()),
      child.maybe <-- stateVar.signal.map {
        case Some(State(x, y, items)) =>
          Some(
            div(
              cls := "ctx-menu",
              position := "fixed",
              left := s"${x}px",
              top := s"${y}px",
              zIndex := 10000,
              items.map(it =>
                div(
                  cls := "ctx-item",
                  it.label,
                  onClick --> { _ => hide(); it.onClick() }
                )
              )
            )
          )
        case None => None
      }
    )