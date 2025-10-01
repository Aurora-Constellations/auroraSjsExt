package com.axiom.ui.patienttracker.tableutils

import shapeless3.deriving.*
import com.axiom.ui.patienttracker.TypeClass.CellDataConvertor
import com.axiom.ui.patienttracker.TypeClass
import com.raquo.laminar.api.L.HtmlElement
import com.axiom.ui.patienttracker.CellData

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


// package com.axiom.ui.patienttracker.tableutils

// import shapeless3.deriving.*        // Labelling, Generic, etc.
// import shapeless3.deriving.K0
// import com.raquo.laminar.api.L.{*, given}

// import com.axiom.ui.patienttracker.TypeClass.CellDataConvertor
// import com.axiom.ui.patienttracker.CellData  
// import com.axiom.ui.patienttracker.utils.{Status, StatusIcons} 

// object TableDerivation:

//   def deriveShowProduct[A](using
//       pInst: K0.ProductInstances[CellDataConvertor, A],
//       labelling: Labelling[A]
//       ): CellDataConvertor[A] =
//         (a: A) =>
//           labelling.elemLabels.zipWithIndex.toList
//             .flatMap { (_, index) =>
//               pInst.project(a)(index)(
//                 [t] => (st: CellDataConvertor[t], pt: t) => st.celldataList(pt)
//               )
//             }

//   def deriveShowSum[A](using
//       inst: K0.CoproductInstances[CellDataConvertor, A]
//       ): CellDataConvertor[A] =
//         (a: A) =>
//           inst.fold(a)(
//             [a] => (st: CellDataConvertor[a], av: a) => st.celldataList(av)
//           )

  
//   inline given derived[A](using gen: K0.Generic[A]): CellDataConvertor[A] =
//     gen.derive(deriveShowProduct, deriveShowSum)

//   // Option support (fallback UI for None)
//   given optionCellDataConvertor[T](using ev: CellDataConvertor[T]): CellDataConvertor[Option[T]] =
//     case Some(value) => ev.celldataList(value)
//     case None        => List(CellData("", div("None")))