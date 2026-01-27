package org.aurora.sjsast

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ArithInterpreterTest extends AnyFunSuite with Matchers {

    test("Basic Arithmetic: Addition and Multiplication") {
        val interpreter = new ArithInterpreter()
        val statements = List(
            Evaluation(
                BinaryExpression(
                    NumberLiteral(DoubleValue(10)), 
                    "+", 
                    BinaryExpression(NumberLiteral(DoubleValue(5)), "*", NumberLiteral(DoubleValue(2)))
                )
            )
        )
        // 10 + (5 * 2) = 20
        val result = interpreter.runModule(statements)
        info(s"Result of 10 + (5 * 2) = ${result.head.asDouble}")
        result shouldBe List(DoubleValue(20.0))
    }

    test("Power and Modulo operators") {
        val interpreter = new ArithInterpreter()
        val statements = List(
            // 2 ^ 3 = 8
            Evaluation(BinaryExpression(NumberLiteral(DoubleValue(2)), "^", NumberLiteral(DoubleValue(3)))),
            // 10 % 3 = 1
            Evaluation(BinaryExpression(NumberLiteral(DoubleValue(10)), "%", NumberLiteral(DoubleValue(3))))
        )
        val result = interpreter.runModule(statements)
        info(s"Result 2 ^ 3 = ${result(0).asDouble}, 10 % 3 = ${result(1).asDouble}")
        result shouldBe List(DoubleValue(8.0), DoubleValue(1.0))
    }

    test("Function Definition and Call") {
        val interpreter = new ArithInterpreter()
        val statements = List(
            // def square(x): x * x;
            Definition("square", List("x"), BinaryExpression(VariableAccess("x"), "*", VariableAccess("x"))),
            // square(4)
            Evaluation(FunctionCall("square", List(NumberLiteral(DoubleValue(4)))))
        )
        val result = interpreter.runModule(statements) 
        info(s"Result of square(4) = ${result.head.asDouble}")
        result shouldBe List(DoubleValue(16.0))
    }

    test("Nested Function Calls with Multiple Arguments") {
        val interpreter = new ArithInterpreter()
        val statements = List(
            // def add(a, b): a + b;
            Definition("add", List("a", "b"), BinaryExpression(VariableAccess("a"), "+", BinaryExpression(VariableAccess("b"), "+", NumberLiteral(DoubleValue(0))))),
            // def doubleAdd(x, y): add(x, y) * 2;
            Definition("doubleAdd", List("x", "y"), 
                BinaryExpression(FunctionCall("add", List(VariableAccess("x"), VariableAccess("y"))), "*", NumberLiteral(DoubleValue(2)))
            ),
            // doubleAdd(3, 5) -> (3 + 5) * 2 = 16
            Evaluation(FunctionCall("doubleAdd", List(NumberLiteral(DoubleValue(3)), NumberLiteral(DoubleValue(5)))))
        )
        val result = interpreter.runModule(statements)
        info(s"Result of doubleAdd(3, 5) = ${result.head.asDouble}")
        result shouldBe List(DoubleValue(16.0))
    }

    test("Mixing IntValue and DoubleValue via asDouble") {
        val interpreter = new ArithInterpreter()
        val statements = List(
            // Evaluation of IntValue(5) + DoubleValue(2.5) = 7.5
            Evaluation(BinaryExpression(NumberLiteral(IntValue(5)), "+", NumberLiteral(DoubleValue(2.5))))
        )
        val result = interpreter.runModule(statements)
        info(s"Result of 5 + 2.5 = ${result.head.asDouble}")
        result shouldBe List(DoubleValue(7.5))
    }

    test("Error handling: Undefined reference") {
        val interpreter = new ArithInterpreter()
        val statements = List(
            Evaluation(VariableAccess("unknown_var"))
        )
        intercept[Exception] {
            interpreter.runModule(statements)
        }.getMessage should include ("Variable unknown_var not found")
    }
}