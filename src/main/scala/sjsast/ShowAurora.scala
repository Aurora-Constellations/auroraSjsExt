package docere.sjsast
import cats.Show
import cats.syntax.show._

object ShowAurora:
  val newline = "\n"

  given Show[PCM] = Show.show { (p: PCM) =>
    val childrenShow = p.cio
      .get("Orders")
      .map { _.asInstanceOf[Orders] }
      .map { _.show }
      .getOrElse("")
    s"$newline$childrenShow"
  }

  given Show[Issues] = Show.show { (i: Issues) =>
    val result = i.ics.map { (ic: IssueCoordinate) => ic.show }.mkString(newline)
    s"Issues:$newline$result"
  }
  given Show[IssueCoordinate] = Show.show { (ic: IssueCoordinate) =>
    val result = ic.refs.map { _.name }.mkString(",")
    val name = ic.name
    s"$name($result)"
  }

  given Show[Orders] = Show.show { (o: Orders) =>
    val result = o.ngo.map { (ngo: NGO) => ngo.show }.mkString(newline)
    val name = o.name
    s"$name:$newline$result"
  }

  given Show[OrderCoordinate] = Show.show { (rc: OrderCoordinate) =>
    val result = rc.refs.map { _.name }.mkString(",")
    val name = rc.name
    s"$name($result)"
  }

  given Show[NGO] = Show.show { (ng: NGO) =>
    val result = ng.orderCoordinates.map { (oc: OrderCoordinate) => oc.show }.mkString(newline)
    val name = ng.name
    s"$name$newline$result"
  }
