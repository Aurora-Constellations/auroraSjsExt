package com.axiom.ui

import com.raquo.laminar.api.L._
import com.axiom.model.shared.dto.Account
import java.time.LocalDateTime
import com.axiom.shared.table.TableDerivation.given
import org.scalajs.dom

case class AccountRow(
  accountId: Long,
  startDate: LocalDateTime,
  endDate: Option[LocalDateTime],
  status: String
)

object AccountsTable:
  private val table = ReactiveTable[AccountRow]
  val selectedAccountIdVar: Var[Option[Long]] = Var(None)

  enum Filter:
    case All, ActiveOnly, InactiveOnly

  val filterVar: Var[Filter] = Var(Filter.All)

  def showAll(): Unit        = filterVar.set(Filter.All)
  def showActive(): Unit     = filterVar.set(Filter.ActiveOnly)
  def showInactive(): Unit   = filterVar.set(Filter.InactiveOnly)

  private def toRow(a: Account) =
    AccountRow(
      a.accountId,
      a.startDate,
      a.endDate,
      if a.endDate.isEmpty then "Active" else "Closed"
    )

  def bindWithContextMenu(
    accountsSignal: Signal[List[Account]]
  )(
    onCreateEncounter: Long => Unit,
    onViewAllEncounters: Long => Unit
  ): Element =
    val current = Var(List.empty[Account])

    val filteredSignal: Signal[List[Account]] =
      accountsSignal.combineWith(filterVar.signal).map {
        case (as, Filter.All)          => as
        case (as, Filter.ActiveOnly)   => as.filter(_.endDate.isEmpty)
        case (as, Filter.InactiveOnly) => as.filter(_.endDate.nonEmpty)
      }

    div(
      table.render(
        onRowClick = Some(i =>
          selectedAccountIdVar.set(Some(current.now()(i).accountId))
        ),
        onRowContextMenu = Some { (i: Int, e: dom.MouseEvent) =>
          val rows = current.now()
          if (i >= 0 && i < rows.length) {
            val acc   = rows(i)
            val accId = acc.accountId
            val isActive = acc.endDate.isEmpty

            val items =
              if (isActive)
                List(
                  ContextMenu.Item("Create Encounter",  () => { ContextMenu.hide(); onCreateEncounter(accId) }),
                  ContextMenu.Item("View all encounters",() => { ContextMenu.hide(); onViewAllEncounters(accId) })
                )
              else
                List(
                  ContextMenu.Item("View all encounters",() => { ContextMenu.hide(); onViewAllEncounters(accId) })
                )

            ContextMenu.show(e.clientX, e.clientY, items)
          }
        }
      ),
      filteredSignal --> { as =>
        current.set(as)
        table.populate(as.map(toRow))
      }
    )

