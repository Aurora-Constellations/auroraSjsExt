package org.aurora.sjsast

import scala.scalajs.js
import org.aurora.sjsast.rewriteReferences._

/**
 * PCM implementation at module level.
 */
case class ModulePCM(module: Module) extends SjsNode:
  override val name: String = module.name

  def toPCM: PCM = PCM(module.cio)

  def toPCM(aliasName: String): PCM =
    // Extract the original issue name from the module's Issues section
    val originalNameOpt = module.cio.get("Issues")
      .map(_.asInstanceOf[Issues])
      .flatMap(_.ics.headOption.map(_.name))
    
    originalNameOpt match {
      case None =>
        // If no issue found, just return the PCM as-is
        toPCM
      case Some(originalName) =>
        // Now rewrite all references from originalName to aliasName
        val aliasedCIO: Map[String, CIO] = module.cio.map { case (key, value) =>
          val rewritten: CIO = value match {
            case orders: Orders => rewriteOrdersReferences(orders, originalName, aliasName)
            case clinical: Clinical => rewriteClinicalReferences(clinical, originalName, aliasName)
            case issues: Issues => issues // Don't rewrite Issues
          }
          key -> rewritten
        }
        PCM(aliasedCIO)
    }

  override def merge(p: SjsNode): SjsNode =
    throw new UnsupportedOperationException("Merging ModulePCM instances is not supported.")


object ModulePCM: 
  def apply(p: GenAst.PCM): ModulePCM =   //TODO: what about Either[ErrorString,ModulePCM]
    val moduleAst = p.module.toOption.getOrElse {
      throw new IllegalArgumentException("PCM does not contain a module declaration.")
    }

    ModulePCM(Module(moduleAst))
