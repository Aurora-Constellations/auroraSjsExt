package org.aurora.sjsast

case class PCM(
    cio: LHMap[String, CIO] = LHMap()
)

object PCM :      
    private def cioFromModuleOrElse(p: GenAst.PCM): LHMap[String, CIO] =
        LHMap(
            p.module.map(_.elements).getOrElse(p.elements)
            .toList
            .map { x =>
                x.$type match {
                case "Issues" =>
                    "Issues" -> (Issues(x.asInstanceOf[GenAst.Issues]): CIO)

                case "Orders" =>
                    "Orders" -> (Orders(x.asInstanceOf[GenAst.Orders]): CIO)

                case "Clinical" =>
                    "Clinical" -> (Clinical(x.asInstanceOf[GenAst.Clinical]): CIO)
                }
            }*
        )

    def apply(p:GenAst.PCM) :PCM = 
        val cio = cioFromModuleOrElse(p)
        PCM(cio)