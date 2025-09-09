package com.axiom.ui.tableutils

import com.raquo.laminar.api.L.{*, given}
import scala.collection.mutable.IndexedSeq

type CCRowList[CC] = List[CC] //list of case classes
type CellDataGrid[D] = List[List[D]] //list of cell data
type CellDataIndexedGrid[D] = IndexedSeq[IndexedSeq[D]] //indexed list of cell data

trait GridT[CC, D]:

  // GCD is short for the tuple (Grid,ColRow, Data)
  // Replaced the tuple (GridT[CC, D], ColRow, D) with case class
  case class GridCell[G, D](grid: G, position: ColRow, data: D)
  type GCD =
    IndexedSeq[IndexedSeq[GridCell[GridT[CC, D], D]]] // updated the GCD type to use GridCell case class
  type Row = IndexedSeq[GridCell[GridT[CC, D], D]]

  // abstract methods
  def cctoData(
      row: Int,
      cc: CC
  ): List[D] // abstract method to convert case class to List of data for rendering on table

  def populate(ccList: CCRowList[CC]): Unit =
    val grid: CellDataGrid[D] =
      ccList.zipWithIndex.map((cc, index) => cctoData(index, cc)) // convert case class to list of data
    val indexedgrid: CellDataIndexedGrid[D] =
      grid.map(_.to(IndexedSeq)).to(IndexedSeq) // convert list of list to indexed list of indexed list
    val newgcd = indexedgrid.zipWithIndex.map { (rowList, d) =>
      rowList.zipWithIndex.map { (data, c) => GridCell(this, ColRow(c, d), data) }
    } // Updated to use the case class GridCell
    gcdVar.set(newgcd)
    showGcdVar.set(newgcd)

// Filter results in laminar

  val gcdVar: Var[GCD] = Var(
    IndexedSeq.empty[IndexedSeq[GridCell[GridT[CC, D], D]]]
  ) // Updated to use the case class GridCell
  val showGcdVar: Var[GCD] = Var(
    IndexedSeq.empty[IndexedSeq[GridCell[GridT[CC, D], D]]]
  ) // Updated to use the case class GridCell

  def colRange =
    val x = gcdVar.now()
    x.size match {
      case 0 => (0 until 0)
      case _ => (0 until x.head.size)
    }
  def rowRange = (0 until gcdVar.now().size)

  def inBounds(c: ColRow): Boolean =
    colRange.contains(c.col) && rowRange.contains(c.row)

  def update(c: ColRow, cell: GridCell[GridT[CC, D], D]): Unit =
    if (inBounds(c))
      gcdVar.now()(c.row)(c.col) =
        cell // changed from "data" => "cell" as now we are storing GridCell and not just a piece of data

  def data(c: ColRow): Option[GridCell[GridT[CC, D], D]] =
    if (inBounds(c))
      Some(gcdVar.now()(c.row)(c.col))
    else None

  def data(x: Int, y: Int): Option[GridCell[GridT[CC, D], D]] =
    data(ColRow(x, y))

end GridT

//TODO this is being used to extend into a case class.  It would be useful to programmatically convert this to tuple and back since above algorithm
//uses TUPLE (GCDTuple) to store data in the grid.
trait GridDataT[G <: GridT[CC, D], CC, D](grid: G, colrow: ColRow, data: D)
