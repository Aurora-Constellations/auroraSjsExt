package com.axiom.ui.extensionutil

import scala.collection.mutable
import com.raquo.laminar.api.L.{*, given}
import zio.json.*
import com.raquo.airstream.ownership.OneTimeOwner

object SingletonExtension:
	val eventBus: EventBus[MessageArg] = EventBus[MessageArg]()
	val webviewMessageRouter: MessageRouter = MessageRouter(eventBus)
	val extensionMessageRouter: MessageRouter = MessageRouter(eventBus)

sealed trait MessageArg extends Product with Serializable:
	// This method is used to derive a name for the message based on its class Name.
	private def derivedMessageName(a: Any) = a.getClass().getSimpleName().stripPrefix("$")
	def name: String = derivedMessageName(this)
	val arg: Any

object MessageArg:
	given JsonCodec[MessageArg] = DeriveJsonCodec.gen[MessageArg]

sealed trait MessageTypedArg[T] extends MessageArg:
	override val arg: T

//all messages are declared from here on
//being under sealed trait allows us to use pattern matching on the type and check for exhaustivity
case class MessageStringArg(arg: String) extends MessageTypedArg[String]
object MessageStringArg :
	def apply(): MessageStringArg = new MessageStringArg("")

case class MessageIntArg(arg: Int) extends MessageTypedArg[Int]

trait MessageHandler:
	protected def handleAnyArg(arg: Any): Unit
	def handleMessageArg(msg: MessageArg): Unit =
		handleAnyArg(msg.arg)

case class MessageRouter(val eb: EventBus[MessageArg]):
	given owner:Owner = new OneTimeOwner(()=>())

	private val msgHandlerMap: mutable.Map[String, MessageHandler] = mutable.Map.empty
	// Stream MessageArgs allows listeners to subscribe to all messages
	private val eventStream = eb.events
	// Streams messages as JSON strings
	private val jsonEventStream = eventStream.map(_.toJson)

	eventStream.foreach(
		msg => dispatchMessage(msg)
	)

	def registerHandler(msg: MessageArg, handler: MessageHandler): Unit =
		msgHandlerMap += (msg.name -> handler)

	def postMessage(msg: MessageArg): Unit =
		eb.emit(msg)

	def dispatchMessage(msg: MessageArg): Unit =
		msgHandlerMap.get(msg.name).foreach { 
			_.handleMessageArg(msg)
		}
