package org.aurora.sjsast.scoring

import org.aurora.sjsast.{BoolValue, DoubleValue, IntValue, SingleValueUnit, StringValue}

object Parsing:
  def parseBoolean(value: SingleValueUnit): Option[Boolean] =
    value.value match
      case BoolValue(v)   => Some(v)
      case IntValue(v)    => Some(v != 0)
      case DoubleValue(v) => Some(v != 0)
      case StringValue(v) =>
        normalizeName(v) match
          case "true" | "yes" | "y" | "present" | "positive" | "1" => Some(true)
          case "false" | "no" | "n" | "absent" | "negative" | "0"  => Some(false)
          case "unknown" | "incomplete" | "not_assessed" | "not_assessable" => None
          case _ => None

  def parseInt(value: SingleValueUnit): Option[Int] =
    value.value match
      case IntValue(v)    => Some(v)
      case DoubleValue(v) => Some(v.toInt)
      case BoolValue(v)   => Some(if v then 1 else 0)
      case StringValue(v) =>
        normalizeName(v) match
          case "unknown" | "nt" | "not_testable" | "incomplete" => None
          case other => other.toIntOption

  def asText(value: SingleValueUnit): Option[String] =
    value.value match
      case StringValue(v) => Some(normalizeName(v))
      case IntValue(v)    => Some(v.toString)
      case DoubleValue(v) => Some(v.toInt.toString)
      case BoolValue(v)   => Some(if v then "true" else "false")

  def normalizeName(value: String): String =
    value
      .trim
      .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
      .toLowerCase
      .replaceAll("[^a-z0-9]+", "_")
      .stripPrefix("_")
      .stripSuffix("_")
