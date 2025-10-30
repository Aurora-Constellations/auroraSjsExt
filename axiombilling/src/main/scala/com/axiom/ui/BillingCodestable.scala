package com.axiom.ui

import com.raquo.laminar.api.L._
import com.axiom.model.shared.dto.Billing
import java.time.LocalDateTime
import com.axiom.shared.table.TableDerivation.given

case class BillingRow(
  billingId: Long,
  encounterId: Long,
  billingCode: String,
  diagnosticCode: String,
  recordedTime: Option[LocalDateTime],
  unitCount: Int,
  notes: Option[String]
)

object BillingCodesTable:
  private val table = ReactiveTable[BillingRow]

  def bind(billingsSignal: Signal[List[Billing]]): Element =
    div(
      table.render(),
      billingsSignal --> { bs =>
        table.populate(bs.map(b => BillingRow(b.billingId, b.encounterId, b.billingCode, b.diagnosticCode, b.recordedTime, b.unitCount, b.Notes)))
      }
    )
