package org.aurora.sjsast

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
        val ordercoord =
            LHSet(
                ngo.orders.toList
                .filter(_.$type == "OrderCoordinate")
                .map { x =>
                    OrderCoordinate(x.asInstanceOf[GenAst.OrderCoordinate])
                }*
            )
        val qurefs = LHSet(ngo.qurc.toList.map(QuReferences(_))*)
        val qu = LHSet(ngo.qu.toList.map(QU(_))*)
        NGO(name, narratives, ordercoord, qurefs, qu)