package org.aurora.sjsast

import scala.collection.mutable.LinkedHashMap

case class PCM(
    name: String = "",
    // Now holds ModulePCM
    modules: LinkedHashMap[String, ModulePCM] = LinkedHashMap.empty
)