package com.axiom.ui

import com.raquo.laminar.api.L._
import com.axiom.model.shared.dto.Account
import java.time.LocalDateTime
import com.axiom.shared.table.TableDerivation.given

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

  def bind(accountsSignal: Signal[List[Account]]): Element =
    val current = Var(List.empty[Account])
     // derive filtered signal
    val filteredSignal: Signal[List[Account]] =
      accountsSignal.combineWith(filterVar.signal).map {
        case (as, Filter.All)          => as
        case (as, Filter.ActiveOnly)   => as.filter(_.endDate.isEmpty)
        case (as, Filter.InactiveOnly) => as.filter(_.endDate.nonEmpty)
      }
    div(
      table.render(onRowClick = Some(i => selectedAccountIdVar.set(Some(current.now()(i).accountId)))),
      filteredSignal --> { as =>
        current.set(as)
        table.populate(as.map(a => AccountRow(a.accountId, a.startDate, a.endDate, if a.endDate.isEmpty then "Active" else "Closed")))
        selectedAccountIdVar.set(None)
      }
    )
