package com.axiom.ui

import com.raquo.laminar.api.L._
import com.axiom.model.shared.dto.Encounter
import java.time.LocalDateTime
import com.axiom.shared.table.TableDerivation.given

case class EncounterRow(
  encounterId: Long,
  accountId: Long,
  doctorId: Long,
  startDate: LocalDateTime
)

object EncountersTable:
  private val table = ReactiveTable[EncounterRow]
  val selectedEncounterIdVar: Var[Option[Long]] = Var(None)

  def bind(encountersSignal: Signal[List[Encounter]]): Element =
    val current = Var(List.empty[Encounter])
    div(
      table.render(onRowClick = Some(i => selectedEncounterIdVar.set(Some(current.now()(i).encounterId)))),
      encountersSignal --> { es =>
        current.set(es)
        table.populate(es.map(e => EncounterRow(e.encounterId, e.accountId, e.doctorId, e.startDate)))
        selectedEncounterIdVar.set(None)
      }
    )
