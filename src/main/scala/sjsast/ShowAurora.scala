package docere.sjsast
import cats.Show
import cats.syntax.show._ 

object ShowAurora:
  val newline = "\n"

  given Show[PCM] =  Show.show{
      (p: PCM) => 
        val childrenShow = p.cio.get("Orders").map{ _.asInstanceOf[Orders] }
          .map{_.show}.getOrElse("")
        s"$newline$childrenShow"  
  }     


  given Show[Issues] =  Show.show{
      (i: Issues) => 
        val result = i.ics.map{(ic:IssueCoordinate) => ic.name}.mkString(newline)
        s"$result"
    }


  given Show[Orders] = Show.show{
      (o: Orders) => 
        val result = o.ngo.map{(ngo:NGO) => ngo.show}.mkString(newline)
        val name = o.name
        s"$name:$newline$result"
    }
  
  given Show[OrderCoordinate] = Show.show{
      (rc: OrderCoordinate) => 
        val quref = rc.refs.show
        val narratives = rc.narratives.mkString(" ")
        val name = rc.name
        s"$name($quref) $narratives"
      }
      
  given Show[NGO] = Show.show{
      (ng: NGO) => 
        val result = ng.orderCoordinates.map{(oc:OrderCoordinate) => oc.show}.mkString(newline)
        val name = ng.name
        s"$name$newline$result"
    }

  given Show[QuReference]  = Show.show{
    (ref:QuReference) =>
      val qu = ref.qu
      val name = ref.name
      s"$qu$name"
  }

  given Show[QuReferences] = Show.show{
    (quRef:QuReferences) =>
      val result = quRef.refs.map{(ref:QuReference) => ref.show}.mkString(",")
      s"$result"
  }
