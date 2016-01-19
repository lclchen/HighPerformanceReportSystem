/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.domain

import scala.collection.mutable.ListBuffer
import java.util.UUID
import java.math.BigDecimal
import scala.collection.mutable.HashSet

/** EventSubscribe is used by EventHandler to subscribe events from EventBus.
  * param id: the UUID of the EventHandler.
  */
trait EventSubscribe{val id: UUID}

/** Subscribe events according to the event-type */
case class EventSubscribe_EventType(id:UUID, typeName:String) extends EventSubscribe
/** Subscribe events according to the committed-time of events */
case class EventSubscribe_TimeDuration(id:UUID, startTime:Long, endTime:Long) extends EventSubscribe
/** Subscribe events according to the account-uuid */
case class EventSubscribe_AccoutID(id:UUID, list:HashSet[UUID]) extends EventSubscribe
/** Subscribe events according to the range of account's balance */
case class EventSubscribe_Balance(id:UUID, min:BigDecimal, max:BigDecimal) extends EventSubscribe
/** Subscribe events according to the currency of events */
case class EventSubscribe_Currency(id:UUID, typeName:String) extends EventSubscribe


/** EventSubscribe is used by EventHandler to cancel subscribe events from EventBus.
  * param id: the UUID of the EventHandler.
  */
trait EventSubscribeCancel{val id:UUID}

/** Cancel subscribe events according to the event-type, the type-name must be same to EventSubscribe. */
case class EventSubscribeCancel_EventType(id:UUID, typeName:String) extends EventSubscribeCancel
/** Cancel subscribe events according to the committed-time of events, the params' value must be same to Subscribe. */
case class EventSubscribeCancel_TimeDuration(id:UUID, startTime:Long, endTime:Long) extends EventSubscribeCancel
/** Cancle subscribe events according to the account-uuid. */
case class EventSubscribeCancel_AccoutID(id:UUID, list:ListBuffer[UUID]) extends EventSubscribeCancel
/** Cancel subscribe events according to the range of account's balance, the params' value must be same to Subscribe. */
case class EventSubscribeCancel_Balance(id:UUID, min:BigDecimal, max:BigDecimal) extends EventSubscribeCancel
/** Cancel subscribe events according to the currency of events, the params' value must be same to Subscribe. */
case class EventSubscribeCancel_Currency(id:UUID, typeName:String) extends EventSubscribeCancel

/** Cancle all subscription of a EventHandler.
  * @param id UUID of a EventHandler.
  */
case class EventSubscribeCancel_All(id:UUID) extends EventSubscribeCancel