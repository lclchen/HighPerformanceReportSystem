package rpsystem.domain

import rpsystem.persistence.IPersistence
import rpsystem.recovery.{RecoveryEvent, IEventRecoveryService}

trait IRecordEvent {
  var isRecordEvent:Boolean = true
  val isUpdateAccount:Boolean = true
  val isUpdateStatistics:Boolean = true

  def recordEvent(evt: Event): Unit = {}
  def updateAccount(evt: Event): Unit = {}
  def updateStatistics(evt: Event): Unit = {}
}

trait IEventHandler {
  def handle(evt: Event): Unit
}

class EventHandler(persistence:IPersistence, evtRecSrv:IEventRecoveryService) extends IEventHandler with IRecordEvent {

  override def recordEvent(evt:Event): Unit = {
    persistence.saveEvent(evt)
  }

  override def handle(evt: Event):Unit = {
    evt match {
      case evt:RecoveryEvent =>
        if (evtRecSrv.available) {
          if (!persistence.isEventExist(evt.eventID) && isRecordEvent)
            recordEvent(evt)
          if (!persistence.isEventExist(evt.eventID) && isUpdateAccount)
            updateAccount(evt)
          if (!persistence.isEventExist(evt.eventID) && isUpdateStatistics)
            updateStatistics(evt)
        }

      case evt:Event =>
        if (isRecordEvent)
          recordEvent(evt)
        if (isUpdateAccount)
          updateAccount(evt)
        if (isUpdateStatistics)
          updateStatistics(evt)

      case _ =>
        logger.error("EventHandler receive unknown message")
    }
  }
}

object EventHandler {
  final val PRIORITY_LOW = 0
  final val PRIORITY_MIDDLE = 1
  final val PRIORITY_HIGH = 2
}
