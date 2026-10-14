package org.aurora.visual.d3

import scala.scalajs.js

// The webview's VS Code API. acquireVsCodeApi() may only be called once per page, so everything that talks to the
// extension (renderers, filter banner) shares this instance. Absent when the page runs in a plain browser (e.g. the
// Vite dev server).
object VsCodeApi:
    lazy val api: Option[js.Dynamic] =
        if (js.typeOf(js.Dynamic.global.acquireVsCodeApi) == "function")
            Some(js.Dynamic.global.acquireVsCodeApi())
        else None
