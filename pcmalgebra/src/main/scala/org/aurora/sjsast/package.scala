package org.aurora.sjsast

import scala.collection.mutable.{LinkedHashSet, LinkedHashMap}

// 1. Define the type alias
type LHSet[T] = LinkedHashSet[T]

// 2. Create a companion object for the alias
object LHSet:
  // This "exports" all members of LinkedHashSet into LHSet's scope
  export LinkedHashSet.{apply, from, empty, newBuilder}

type LHMap[K, V] = LinkedHashMap[K, V]

object LHMap:
  export LinkedHashMap.{apply, from, empty, newBuilder}