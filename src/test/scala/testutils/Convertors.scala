package docere.testutils



/**
  * FileReader object to read files from the file system and creates a string dsl for platorm independent paths
  */
import typings.std.Map as TsMap
import scala.collection.mutable
import typings.langium.libUtilsStreamMod.TreeStream

object Convertors:

  extension [K,V] (m:TsMap[K, V])
    def toScalaMap: Map[K, V] = 
      var scalaMutMap = mutable.Map[K, V]()
      m.forEach{(v, k, _) => scalaMutMap += (k -> v)}
      scalaMutMap.toMap

  extension [T] (s:TreeStream[T])
    def toScalaList: List[T] = 
      var list = mutable.ListBuffer[T]()
      s.forEach{ (v, _) => list = list += v}
      list.toList
      
  