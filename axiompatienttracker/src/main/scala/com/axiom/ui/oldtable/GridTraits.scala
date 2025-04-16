package org.axiom.ui.oldtable.utils

import collection.mutable.ListBuffer
import org.scalajs.dom.Coordinates

trait GridT[D](cols:Int,rows:Int) :
  lazy val grid:ListBuffer[ListBuffer[Option[D]]]
  val xRange = (0 until cols)
  val yRange = (0 until rows)
  val linearizedleftRightCoordinates = for(
    y <- yRange;
    x <- xRange
    ) yield(Coordinate(x,y))
  
  def inBounds(c:Coordinate): Boolean = 
    xRange.contains(c.x) && yRange.contains(c.y)

  def update(c:Coordinate, data:Option[D]):Unit =
    if(inBounds(c))   grid(c.y)(c.x) = data

  def data(c:Coordinate):Option[D] = 
    if(inBounds(c))
      grid(c.y)(c.x)   else   None
  
  def data(x:Int,y:Int):Option[D] = 
    data(Coordinate(x,y))


end GridT
  

trait GridDataT[G,D](grid:G,data:D) :
  def coordinate:Coordinate



import collection.mutable.ListBuffer  
/**
  * allocates 2 dimensional ListBuffers[D]  as specified by [T]
  */
trait LLBufferDimensionT[D] extends TwoDimLBufferT[D]:
  def dim(x:Int,y:Int) =
    def row = (0 until x)
      .foldLeft ( emptyRow ) { 
      (lb,_) => lb.addOne(None)
    }

    (0 until y)
      .foldLeft(emptyBuffer){
        (lb,y) => lb.addOne(row)
    }


trait TwoDimLBufferT [D] :   
  type RowType = ListBuffer[Option[D]]
  def emptyRow:RowType
  def emptyBuffer:ListBuffer[RowType]

    




