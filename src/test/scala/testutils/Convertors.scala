package docere.testutils



/**
  * FileReader object to read files from the file system and creates a string dsl for platorm independent paths
  */
import scala.collection.mutable
import typings.langium.libUtilsStreamMod.TreeStream

object Convertors:

  extension [T] (s:TreeStream[T])
    def toScalaList: List[T] = 
      var list = mutable.ListBuffer[T]()
      s.forEach{ (v, _) => list = list += v}
      list.toList
      
  
