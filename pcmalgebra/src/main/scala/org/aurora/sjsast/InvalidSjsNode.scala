package org.aurora.sjsast

case class InvalidSjsNode(
    message: String,
    source: String = ""
)