package org.aurora.sjsast.arnoldinterpreter


object SimpleInterpreter :

  // The interpreter function that evaluates an expression
  def evaluate(expr: Expr): Value = expr match {
    case Number(value) => IntValue(value)

    // Handle arithmetic operations
    case Add(left, right) => (evaluate(left), evaluate(right)) match {
        case (IntValue(l), IntValue(r)) => IntValue(l + r)
        case _ => throw new IllegalArgumentException("Cannot add non-integer values")
    }
    case Subtract(left, right) => (evaluate(left), evaluate(right)) match {
        case (IntValue(l), IntValue(r)) => IntValue(l - r)
        case _ => throw new IllegalArgumentException("Cannot subtract non-integer values")
    }
    case Multiply(left, right) => (evaluate(left), evaluate(right)) match {
        case (IntValue(l), IntValue(r)) => IntValue(l * r)
        case _ => throw new IllegalArgumentException("Cannot multiply non-integer values")
    }
    case Divide(left, right) => (evaluate(left), evaluate(right)) match {
        case (IntValue(l), IntValue(r)) =>
        if (r == 0) throw new ArithmeticException("Division by zero")
        IntValue(l / r)
        case _ => throw new IllegalArgumentException("Cannot divide non-integer values")
    }

    // Handle inequality operations
    case GreaterThan(left, right) => (evaluate(left), evaluate(right)) match {
        case (IntValue(l), IntValue(r)) => BoolValue(l > r)
        case _ => throw new IllegalArgumentException("Cannot compare non-integer values")
    }
    case LessThan(left, right) => (evaluate(left), evaluate(right)) match {
        case (IntValue(l), IntValue(r)) => BoolValue(l < r)
        case _ => throw new IllegalArgumentException("Cannot compare non-integer values")
    }
    case EqualTo(left, right) => (evaluate(left), evaluate(right)) match {
        case (IntValue(l), IntValue(r)) => BoolValue(l == r)
        case (BoolValue(l), BoolValue(r)) => BoolValue(l == r)
        case _ => throw new IllegalArgumentException("Cannot compare dissimilar types")
    }
  }
// TODO move this to tests
// def main(args: Array[String]): Unit 
//   // Example 1: Simple addition: 5 + 3
//   val expr1 = Add(Number(5), Number(3))
//   println(s"Expression: 5 + 3 = ${evaluate(expr1)}") // Expected: IntValue(8)

//   // Example 2: More complex expression: (5 + 3) * 2
//   val expr2 = Multiply(Add(Number(5), Number(3)), Number(2))
//   println(s"Expression: (5 + 3) * 2 = ${evaluate(expr2)}") // Expected: IntValue(16)

//   // Example 3: Greater than: 10 > 5
//   val expr3 = GreaterThan(Number(10), Number(5))
//   println(s"Expression: 10 > 5 = ${evaluate(expr3)}") // Expected: BoolValue(true)

//   // Example 4: Less than: 10 < 5
//   val expr4 = LessThan(Number(10), Number(5))
//   println(s"Expression: 10 < 5 = ${evaluate(expr4)}") // Expected: BoolValue(false)

//   // Example 5: Equality of expressions: (2 + 3) == 5
//   val expr5 = EqualTo(Add(Number(2), Number(3)), Number(5))
//   println(s"Expression: (2 + 3) == 5 = ${evaluate(expr5)}") // Expected: BoolValue(true)

//   // Example 6: Type error
//   try {
//       val expr6 = Add(Number(10), GreaterThan(Number(5), Number(3)))
//       println(s"Expression: 10 + (5 > 3) = ${evaluate(expr6)}")
//   } catch {
//       case e: IllegalArgumentException => println(s"Caught expected error: ${e.getMessage}")
//   }
