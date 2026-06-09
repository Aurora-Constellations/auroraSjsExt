package org.aurora.sjsast

case class SingleValueUnit(
    value: Value,
    unit: String,
    negative: Boolean = false
) extends AstNode

object SingleValueUnit:
  def apply(ast: GenAst.SingleValueUnit): SingleValueUnit =
    val isNegative = ast.negative.toOption.contains("(-)")
    
    // Map the polymorphic 'value'
    val irValue = Value(ast.value)
    
    // Map the 'unit'. If it's the literal '_', it becomes a string.
    val irUnit = ast.unit.toString

    SingleValueUnit(
      value = irValue,
      unit = irUnit,
      negative = isNegative
    )
