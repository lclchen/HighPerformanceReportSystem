package rpsystem.domain

import scala.collection.mutable.ListBuffer
import java.util.UUID
import java.math.BigDecimal
import scala.collection.mutable.HashSet

//add new subscribe content
trait EventSubscribe{val id:UUID}

case class EventSubscribe_EventType(id:UUID, typeName:String) extends EventSubscribe
case class EventSubscribe_TimeDuration(id:UUID, startTime:Long, endTime:Long) extends EventSubscribe
case class EventSubscribe_AccoutID(id:UUID, list:HashSet[UUID]) extends EventSubscribe
case class EventSubscribe_Balance(id:UUID, min:BigDecimal, max:BigDecimal) extends EventSubscribe
case class EventSubscribe_Currency(id:UUID, typeName:String) extends EventSubscribe


//remove subscribe content
trait EventSubscribeCancel{val id:UUID}

case class EventSubscribeCancel_EventType(id:UUID, typeName:String) extends EventSubscribeCancel	//must same to add_cmd
case class EventSubscribeCancel_TimeDuration(id:UUID, startTime:Long, endTime:Long) extends EventSubscribeCancel //must same ..
case class EventSubscribeCancel_AccoutID(id:UUID, list:ListBuffer[UUID]) extends EventSubscribeCancel
case class EventSubscribeCancel_Balance(id:UUID, min:BigDecimal, max:BigDecimal) extends EventSubscribeCancel  //must same ..
case class EventSubscribeCancel_Currency(id:UUID, typeName:String) extends EventSubscribeCancel  //must same ..

case class EventSubscribeCancel_All(id:UUID) extends EventSubscribeCancel