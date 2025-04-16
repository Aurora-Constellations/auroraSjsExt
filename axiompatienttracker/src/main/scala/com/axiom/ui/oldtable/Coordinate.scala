package org.axiom.ui.oldtable.utils

case class Coordinate(x:Int,y:Int) :
  def addX(x:Int):Coordinate =
    this.copy(x = this.x+x)
  def addY(y:Int):Coordinate =
    this.copy(y= this.y+ y)
