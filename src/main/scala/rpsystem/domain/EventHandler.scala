/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.domain

import java.util.UUID
import rpsystem.persistence.IPersistence
import rpsystem.recovery.{RecoveryEvent, IEventRecoveryService}

/** The trait to record event */
trait IRecordEvent {
  /** whether it is necessary to record event */
  var isRecordEvent:Boolean = true
  /** whether it is necessary to update account snapshot or entity. */
  val isUpdateAccount:Boolean = true
  /** whether is is necessary to update some statistics. */
  val isUpdateStatistics:Boolean = true

  def recordEvent(evt: Event): Unit = {}
  def updateAccount(evt: Event): Unit = {}
  def updateStatistics(evt: Event): Unit = {}
}

/** The trait of EventHandler */
trait IEventHandler {
  def handle(evt: Event): Unit
}

/** The implementation of IEventHandler.
  * It is responsible for handling events sent from the EventBus.
  * @param persistence IPersistence to record events or update information.
  * @param evtRecSrv IEventRecoveryServier for error-recovery.
  * @param uuid UUID to identity the EventHandler.
  */
class EventHandler(persistence:IPersistence, evtRecSrv:IEventRecoveryService, val uuid:UUID)
  extends IEventHandler with IRecordEvent {

  override def recordEvent(evt:Event): Unit = {
    persistence.saveEvent(evt)
  }

  override def handle(evt: Event):Unit = {
    evt match {
      // handle recovery-event.
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
        if (evtRecSrv.available)
          evtRecSrv.removeProcessedEvtByEH(evt.eventID, uuid)

      case _ =>
        logger.error("EventHandler receive unknown message")
    }
  }
}

object EventHandler {
  // the priority of EventHandler, EventHandlerActor with high priority should be ensure to be online.
  final val PRIORITY_LOW = 0
  final val PRIORITY_MIDDLE = 1
  final val PRIORITY_HIGH = 2
}
