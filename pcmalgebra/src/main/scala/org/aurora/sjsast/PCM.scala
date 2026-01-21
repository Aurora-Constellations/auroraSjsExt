package org.aurora.sjsast

import scala.collection.mutable.LinkedHashMap

case class PCM(
    name: String = "",
    cio: LinkedHashMap[String, CIO] = LinkedHashMap.empty
)