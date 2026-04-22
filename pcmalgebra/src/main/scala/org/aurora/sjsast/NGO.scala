package org.aurora.sjsast

import scala.scalajs.js

case class NGO(
    name: String,
    narratives: LHSet[NL_STATEMENT] = LHSet(),
    ordercoord: LHSet[OrderCoordinate] = LHSet(), // Assuming simplified structure, ignoring MutuallyExclusive for now
    qurefs: LHSet[QuReferences] = LHSet(),
    qu: LHSet[QU] = LHSet()
)

object NGO:
    def apply(ngo: GenAst.NGO): NGO = 
        val name = ngo.name
        val narratives = LHSet(ngo.narrative.toList.map(NL_STATEMENT(_))*)
        // Ignoring MutuallyExclusive wraps. 
        // We filter for objects directly typed as OrderCoordinate.
        val ordercoord =
            LHSet(
                ngo.orders.toList
                .filter(_.asInstanceOf[js.Dynamic].`$type`.asInstanceOf[String] == "OrderCoordinate")
                .map { x =>
                    OrderCoordinate(x.asInstanceOf[GenAst.OrderCoordinate])
                }*
            )
        val qurefs = LHSet(ngo.qurc.toList.map(QuReferences(_))*)
        val qu = LHSet(ngo.qu.toList.map(QU(_))*)
        NGO(name, narratives, ordercoord, qurefs, qu)