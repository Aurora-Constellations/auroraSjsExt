package org.aurora.sjsast

import scala.annotation.targetName


case class Module(name:String,cio:Map[String,CIO])

object Module :      
  def apply(m:GenAst.Module): Module =
    new Module(m.name, cioFromModuleElements(m))

    
  //converts string based type representations from typescript to Scala based types
  private def cioFromModuleElements (m:GenAst.Module):Map[String,CIO] = 
    m.elements
    .toList
      .map(x => x.$type -> x)
      .map{(t,o) =>
        t match {
          case "Issues" => t -> Issues.apply(o.asInstanceOf[GenAst.Issues])
          case "Orders" => t -> Orders.apply(o.asInstanceOf[GenAst.Orders])
          case "Clinical" => t -> Clinical.apply(o.asInstanceOf[GenAst.Clinical])
        }

      }.toMap


 
