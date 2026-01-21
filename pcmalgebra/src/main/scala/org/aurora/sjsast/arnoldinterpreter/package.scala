package org.aurora.sjsast.arnoldinterpreter

// 1. Define the Abstract Syntax Tree (AST) for our simple language
sealed trait Expr

// Represents a literal integer number
case class Number(value: Int) extends Expr

// Represents an addition operation
case class Add(left: Expr, right: Expr) extends Expr

// Represents a subtraction operation
case class Subtract(left: Expr, right: Expr) extends Expr

// Represents a multiplication operation
case class Multiply(left: Expr, right: Expr) extends Expr

// Represents a division operation
case class Divide(left: Expr, right: Expr) extends Expr

// Represents a greater than operation
case class GreaterThan(left: Expr, right: Expr) extends Expr

// Represents a less than operation
case class LessThan(left: Expr, right: Expr) extends Expr

// Represents an equality check
case class EqualTo(left: Expr, right: Expr) extends Expr


// 2. Define the possible output values of our interpreter
sealed trait Value
case class IntValue(value: Int) extends Value
case class BoolValue(value: Boolean) extends Value

