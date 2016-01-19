/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.domain

import rpsystem.recovery._
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._
import java.util.UUID

/** The trait for EventBus */
trait IEventBus {

  // below is methods for Error-Recovery.
  /** Record the last recevied event. */
  def recordLastReceivedEvt(evt: Event)

  /** Record all sent events. */
  def recordAllSentEvt(evt: Event, num: UUID)

  /** Get missing or unfinished events for error-recovery */
  def getRecoveryEvents(): ListBuffer[RecoveryEvent]

  /** Remove and clear records for error-recovery */
  def removeRecInfo
}

/** The implementation of EventBus.
  * @param cmdRecSrv ICommandRecoveryService for error-recovery.
  * @param evtRecSrv IEventRecoveryService for error-recovery.
  */
class EventBus(cmdRecSrv: ICommandRecoveryService, evtRecSrv: IEventRecoveryService) extends IEventBus {
  /** The broadcast mode or strategy of EventBus.
    * The default mode is broadcasting without packing.
    */
  var mode: EventBus.BROADCAST_MODE = new EventBus.BROADCAST_MODE_NOPACK()

  def setMode(newMode: EventBus.BROADCAST_MODE) {
    mode = newMode
  }

  /** Check whether the package is full and need to sends the package of events to EventHandlers.
    * @param evt New event.
    * @param pack the package of events.
    * @return ListBuffer[Event] and need to be sent. Empty if the package is no need to send.
    */
  def isPackageFull(evt: Event, pack: ListBuffer[Event]): ListBuffer[Event] = {
    var sentEvts: ListBuffer[Event] = null
    mode match {
      case m: EventBus.BROADCAST_MODE_NOPACK =>
        pack += evt
        sentEvts = pack.clone
        pack.clear

      case m: EventBus.BROADCAST_MODE_PACK_NUM =>
        pack += evt
        if (pack.length >= m.packNum) {
          sentEvts = pack.clone
          pack.clear
        }

      case m: EventBus.BROADCAST_MODE_PACK_TIMECYCLE =>
        pack += evt
        if (System.currentTimeMillis - pack(0).committedTime.getTime >= m.packDuration) {
          sentEvts = pack.clone
          pack.clear
        }
    }
    return sentEvts
  }

  override def recordLastReceivedEvt(evt: Event) {
    if (evtRecSrv.available)
      evtRecSrv.recordLastSentEvtByEB(evt)
    if (cmdRecSrv.available)
      cmdRecSrv.removeProcessedCmdByEB(evt.commandID, evt.accountID)
  }

  override def recordAllSentEvt(evt: Event, num: UUID) {
    if (evtRecSrv.available)
      evtRecSrv.recordProcessingEvtByEB(evt, num)
  }

  override def getRecoveryEvents(): ListBuffer[RecoveryEvent] = {
    if (!evtRecSrv.available && !cmdRecSrv.available)
      return new ListBuffer[RecoveryEvent]()

    val message = evtRecSrv.getLastEventByEB
    var allEvts: ListBuffer[(RecoveryEvent, UUID)] = null
    if (message.length != 108) {
      allEvts = evtRecSrv.getRecEventsByEB(false)
    } else {
      allEvts = evtRecSrv.getRecEventsByEB(cmdRecSrv.isCmdExistByEB(UUID.fromString(message.substring(36, 72)),
        UUID.fromString(message.substring(72))))
    }

    var lastEventID: String = ""
    val evts: ListBuffer[RecoveryEvent] = new ListBuffer[RecoveryEvent]()
    breakable {
      allEvts.foreach(evt => {
        if (!evt._1.eventID.toString.equals(lastEventID)) {
          lastEventID = evt._1.eventID.toString
          evts += evt._1
        }
      })
    }
    return evts
  }

  override def removeRecInfo {
    if (evtRecSrv.available)
      evtRecSrv.removeAllEventsByEB
  }
}

object EventBus {

  /** The broadcast mode or strategy of EventBus */
  trait BROADCAST_MODE

  /** The default broadcast mode, broadcast event each time EventBus receive. */
  case class BROADCAST_MODE_NOPACK() extends BROADCAST_MODE

  /** The broadcast mode, broadcast events after a certain number of events.
    * @param packNum the maximum number of events to broadcast.
    */
  case class BROADCAST_MODE_PACK_NUM(packNum: Int) extends BROADCAST_MODE

  /** The broadcast mode to broadcast events after a time duration.
    * @param packDuration the maximum time duration to broadcast.
    */
  case class BROADCAST_MODE_PACK_TIMECYCLE(packDuration: Long) extends BROADCAST_MODE

}