package com.axiom.shared.table

import shapeless3.deriving.*
import com.axiom.shared.table.TypeClass.CellDataConvertor
import com.axiom.shared.table.TypeClass
import com.raquo.laminar.api.L.HtmlElement
import com.axiom.shared.table.CellData

object TableDerivation:

  // Product derivation (case classes)
  def deriveShowProduct[A](using
      pInst: K0.ProductInstances[CellDataConvertor, A],
      labelling: Labelling[A]
  ): CellDataConvertor[A] =
    (a: A) =>
      labelling.elemLabels.zipWithIndex.toList.flatMap { (_, index) =>
        pInst.project(a)(index)(
          [t] => (st: CellDataConvertor[t], pt: t) => st.celldataList(pt)
        )
      }

  // Sum derivation (enums / sealed traits)
  def deriveShowSum[A](using
      inst: K0.CoproductInstances[CellDataConvertor, A]
  ): CellDataConvertor[A] =
    (a: A) =>
      inst.fold(a)(
        [t] => (st: CellDataConvertor[t], av: t) => st.celldataList(av)
      )

  // The actual generic "derived" given
  inline given derived[A](using gen: K0.Generic[A]): CellDataConvertor[A] =
    gen.derive(deriveShowProduct, deriveShowSum)


