package com.axiom.ui


import com.raquo.laminar.api.L._
import com.axiom.model.shared.dto.Account
import java.time.LocalDateTime
import com.axiom.shared.table.TableDerivation.derived

case class AccountRow(
  accountId: Long,
  startDate: LocalDateTime,
  endDate: Option[LocalDateTime],
  status: String
)

object AccountsTable:
  private val table = ReactiveTable[AccountRow]()

  def bind(accountsSignal: Signal[List[Account]]): Element =
    div(
      table.render(),
      accountsSignal --> { as =>
        table.populate(as.map(a =>
          AccountRow(a.accountId, a.startDate, a.endDate, if a.endDate.isEmpty then "Active" else "Closed")
        ))
      }
    )
