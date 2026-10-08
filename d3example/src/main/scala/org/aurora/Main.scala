package org.aurora

import com.axiom.protocol.{Protocol, ToExtension, ToWebview}
import org.aurora.messaging.VsCodeBridge
import org.aurora.visual.d3.{D3Renderer, toJS}

object D3MainObject {

  def main(args: Array[String]): Unit = {

    val renderer = new D3Renderer("#d3-container", VsCodeBridge.send)

    VsCodeBridge.listen {
      case ToWebview.UpdateDiagram(tree) =>
        renderer.render(tree.toJS)

      case ToWebview.HighlightPosition(line, character) =>
        renderer.highlightAt(line, character)
    }

    // Tell the host we are listening so it can send the current diagram (it may have been drawn before this webview existed)
    VsCodeBridge.send(ToExtension.Ready(Protocol.Version))
  }
}
