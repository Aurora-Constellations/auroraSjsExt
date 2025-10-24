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

  def bind(accountsSignal: Signal[List[Account]]): Element =
    val current = Var(List.empty[Account])
    div(
      table.render(onRowClick = Some(i => selectedAccountIdVar.set(Some(current.now()(i).accountId)))),
      accountsSignal --> { as =>
        current.set(as)
        table.populate(as.map(a => AccountRow(a.accountId, a.startDate, a.endDate, if a.endDate.isEmpty then "Active" else "Closed")))
        selectedAccountIdVar.set(None)
      }
    )
