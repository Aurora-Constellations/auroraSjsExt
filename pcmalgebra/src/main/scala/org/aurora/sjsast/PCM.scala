package org.aurora.sjsast

import scala.collection.mutable.LinkedHashMap

case class PCM(
    cio: LinkedHashMap[String, CIO] = LinkedHashMap.empty
)