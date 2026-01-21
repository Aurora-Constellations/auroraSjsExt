package org.aurora.sjsast

import org.aurora.sjsast.{GenAst => G}
import scala.scalajs.js

case class QuReference(
    refName: String,
    qu: QU = QU()
)

object QuReference:
  def fromJs(qr: G.QuReference): QuReference =
    // Extract the qu string from the array of QU objects
    val qu = QU.fromJsArray(qr.qu)
    
    // Extract the reference name from the Langium Reference
    val refName = try {
      val refObj = qr.ref
      if (!js.isUndefined(refObj.ref)) {
        // The actual referenced object
        val referencedNode = refObj.ref.asInstanceOf[js.Dynamic]
        referencedNode.name.asInstanceOf[String]
      } else {
        // Fallback to $refText if ref is not resolved
        refObj.asInstanceOf[js.Dynamic].`$refText`.asInstanceOf[String]
      }
    } catch {
      case e: Exception => 
        println(s"Error extracting reference name: ${e.getMessage}")
        ""
    }
    
    QuReference(refName = refName, qu = qu)