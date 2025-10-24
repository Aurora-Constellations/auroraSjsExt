package com.axiom.ui

import com.raquo.laminar.api.L._
import com.axiom.model.shared.dto.Encounter
import java.time.LocalDateTime
import com.axiom.shared.table.TableDerivation.given // <-- needed for CellDataConvertor given

case class EncounterRow(
  encounterId: Long,
  accountId: Long,
  doctorId: Long,
  startDate: LocalDateTime
)

object EncountersTable {
  private val table = ReactiveTable[EncounterRow]
  val selectedEncounterIdVar: Var[Option[Long]] = Var(None)


  // Dynamic signal 
  def bind(
    encountersSignal: Signal[List[Encounter]],
    selectedVar: Var[Option[Long]] = selectedEncounterIdVar
  ): Element = {
    val current = Var(List.empty[Encounter])

    div(
      // the grid
      table.render(onRowClick = Some { i =>
        val es = current.now()
        if (i >= 0 && i < es.length) selectedVar.set(Some(es(i).encounterId))
      }),

      // repopulate when data changes
      encountersSignal --> { es =>
        current.set(es)
        val rows = es.map(e => EncounterRow(e.encounterId, e.accountId, e.doctorId, e.startDate))
        table.populate(rows)
        // reset selection on list change
        selectedVar.set(None)
      }
    )
  }
}
