package org.aurora.sjsast.arnold
import magnolia1._
import org.aurora.sjsast.LHSet

trait Join1[T]:
  def join(a: T, b: T): T
  def meet(a: T, b: T): T

object Join1 extends AutoDerivation[Join1]:
  val hey = 0;

  //extension method definition
  extension [T](a: T)(using jm: Join1[T])
    def |+|(b: T): T = jm.join(a, b)


  override def join[T](ctx: CaseClass[Join1, T]): Join1[T] = 
      new Join1[T] {
        def join(a: T, b: T): T =  ctx.construct { param =>
           param.typeclass.join(param.deref(a), param.deref(b))
        }

        def meet(a: T, b: T): T = ctx.construct { param =>
           param.typeclass.meet(param.deref(a), param.deref(b))
        }
    }

  override def split[T](ctx: SealedTrait[Join1, T]): Join1[T] = 
    new Join1[T] {
      def join(a: T, b:T): T = ctx.choose(a) { sub => 
        if (sub.cast.isDefinedAt(b)) then
          sub.typeclass.join(sub.value, sub.cast(b))
          else 
            a}

      def meet (a: T, b:T): T = ctx.choose(a) { sub => 
        if (sub.cast.isDefinedAt(b)) then
          sub.typeclass.join(sub.value, sub.cast(b))
          else 
            a}
    }
  
  

trait NamedTrait:
  val name: String

// TODO finish join and meet implementations
given Join1[Int] with
  def join(a: Int,b: Int): Int =  
  if (a == b) then 2*a
  else if (a == 0) then b
  else if (b == 0) then a
  else a*b

  def meet(a: Int, b: Int): Int =
    if (a == b) then a else 0
  
given Join1[String] with
  def join(a: String, b: String): String = 
  if (a == b) then a 
  else if (a.isEmpty) then b 
  else if (b.isEmpty) then a 
  else s"$a$b"

  def meet(a: String, b: String): String = 
    if (a == b) then a else ""

given [T <: NamedTrait]:Join1[LHSet[T]]  = ???  //TODO FINISH THIS
