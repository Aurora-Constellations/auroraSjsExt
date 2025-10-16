package com.axiom.ui.tableutils

case class ColRow(col:Int,row:Int) :
  def addX(col:Int):ColRow =
    this.copy(col = this.col+col)
  def addY(row:Int):ColRow =
    this.copy(row= this.row+ row)
  def add(vector:ColRow)  =
    this.copy(vector.col+col,vector.row+row)
