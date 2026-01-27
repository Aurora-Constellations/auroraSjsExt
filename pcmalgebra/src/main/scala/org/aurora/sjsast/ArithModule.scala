package org.aurora.sjsast

case class ArithModule(
  name: String, 
  statements: List[Statement]
)

sealed trait Statement
case class Definition(name: String, args: List[String], expr: Expression) extends Statement
case class Evaluation(expr: Expression) extends Statement

sealed trait Expression
case class NumberLiteral(value: Value) extends Expression
case class BinaryExpression(left: Expression, operator: String, right: Expression) extends Expression
case class FunctionCall(name: String, args: List[Expression]) extends Expression
case class VariableAccess(name: String) extends Expression // Maps to FunctionCall with no args