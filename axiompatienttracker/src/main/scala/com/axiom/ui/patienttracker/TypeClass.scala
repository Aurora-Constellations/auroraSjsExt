package com.axiom.ui.patienttracker
import shapeless3.deriving.*
import java.time.*

object TypeClass :
  //type class and givens
  trait CellDataConvertor[A]:
    def celldata(a: A):List[CellData]

  object CellDataConvertor:
    given CellDataConvertor[Int] =         i => List(CellData(i.toString,"black"))
    given CellDataConvertor[Boolean] =     b => List(CellData(b.toString,"blue"))
    given CellDataConvertor[String] =      s =>  List(CellData(identity(s),"#3361ff"))
    given CellDataConvertor[LocalDate] =   d => List(CellData(d.toString(),"yellow"))
    given CellDataConvertor[LocalDateTime] = d => List(CellData(d.toString(),"cyan"))

    def deriveShowProduct[A](using
      pInst: K0.ProductInstances[CellDataConvertor, A],
      labelling: Labelling[A]
      ): CellDataConvertor[A] =
        (a: A) =>
          val properties = labelling.elemLabels.zipWithIndex
            .map { (label, index) =>
              val value = pInst.project(a)(index)([t] => (st: CellDataConvertor[t], pt: t) => st.celldata(pt))
              value
            }.foldLeft(List.empty[CellData])((a,b) =>a ++ b)
            properties
            


    def deriveShowSum[A](using
        inst: K0.CoproductInstances[CellDataConvertor, A]
    ): CellDataConvertor[A] =
      (a: A) => inst.fold(a)([a] => (st: CellDataConvertor[a], a: a) => st.celldata(a))

    inline given derived[A](using gen: K0.Generic[A]): CellDataConvertor[A] =
      gen.derive(deriveShowProduct, deriveShowSum)

  

