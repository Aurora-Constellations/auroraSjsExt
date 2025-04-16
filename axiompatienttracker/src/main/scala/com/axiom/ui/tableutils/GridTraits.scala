package com.axiom.ui.tableutils

import com.raquo.laminar.api.L.{*, given}
import collection.mutable.ListBuffer
import scala.collection.mutable


type CCRowList[CC] = List[CC]  //list of case classes
type CellDataGrid[D] = List[List[D]] //list of cell data
type CellDataIndexedGrid[D] = mutable.IndexedSeq[mutable.IndexedSeq[D]]  //indexed list of cell data

trait GridT[CC,D]:

  //GCD is short for the tuple (Grid,ColRow, Data)
  type GCD = mutable.IndexedSeq[mutable.IndexedSeq[(GridT[CC,D],ColRow,D)]]
  type GCDTuple = (GridT[CC,D],ColRow,D)
  type Row =mutable.IndexedSeq[GCDTuple]

  //abstract methods
  def cctoData(row:Int,cc:CC):List[D] //abstract method to convert case class to List of data for rendering on table
  
  def populate(ccList:CCRowList[CC]):Unit =
    val grid :CellDataGrid[D] = ccList.zipWithIndex .map((cc,index) => cctoData(index,cc)) //convert case class to list of data
    val indexedgrid : CellDataIndexedGrid[D] = grid.map(_.to(mutable.IndexedSeq)).to(mutable.IndexedSeq) //convert list of list to indexed list of indexed list
    val newgcd = indexedgrid.zipWithIndex.map{(rowList,d) => rowList.zipWithIndex.map{(data,c) => (this,ColRow(c,d),data)}}
    gcdVar.set(newgcd)
    showGcdVar.set(newgcd)
    

  val gcdVar : Var[GCD] = Var(mutable.IndexedSeq.empty[mutable.IndexedSeq[(GridT[CC,D],ColRow,D)]].empty)
  val showGcdVar: Var[GCD] = Var(mutable.IndexedSeq.empty[mutable.IndexedSeq[(GridT[CC, D], ColRow, D)]].empty)

  // lazy val gcd :GCD = indexedrid.zipWithIndex.map{(rowList,d) => rowList.zipWithIndex.map{(data,c) => (this,ColRow(c,d),data)}}

  def colRange = 
    val x = gcdVar.now()
    x.size match {
      case 0 => (0 until 0)
      case _ => (0 until x.head.size)
    }
  def rowRange = (0 until gcdVar.now().size)
  
  def inBounds(c:ColRow): Boolean = 
    colRange.contains(c.col) && rowRange.contains(c.row)

  def update(c:ColRow, data:GCDTuple):Unit =
    if(inBounds(c))   gcdVar.now()(c.row)(c.col) = data 

  def data(c:ColRow):Option[GCDTuple] = 
    if(inBounds(c))
      Some(gcdVar.now()(c.row)(c.col))   else   None
  
  def data(x:Int,y:Int):Option[GCDTuple] = 
    data(ColRow(x,y))

end GridT
  
//TODO this is being used to extend into a case class.  It would be useful to programmatically convert this to tuple and back since above algorithm
//uses TUPLE (GCDTuple) to store data in the grid. 
trait GridDataT[G <: GridT[CC,D],CC,D](grid:G,colrow:ColRow,data:D) 
  



