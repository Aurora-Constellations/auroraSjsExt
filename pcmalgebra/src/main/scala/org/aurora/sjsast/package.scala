package org.aurora.sjsast

import scala.collection.mutable.LinkedHashSet
import scala.collection.mutable.LinkedHashMap

// Alias for LinkedHashSet
type LHSet[T] = LinkedHashSet[T]

// Helper for creating sets
def LHSet[T](elems: T*): LHSet[T] = LinkedHashSet(elems*)

// Alias for LinkedHashMap
type LHMap[K, V] = LinkedHashMap[K, V]

