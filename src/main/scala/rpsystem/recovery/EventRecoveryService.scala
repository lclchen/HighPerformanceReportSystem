/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.recovery

import rpsystem.domain._
import java.util.UUID
import java.util.Date
import java.util.TreeMap
import java.util.Comparator
import scala.util.control.Breaks._
import scala.collection.mutable.ListBuffer

/** The trait of EventRecoveryService */
trait IEventRecoveryService{
  /** whether the error-recovery for event is available */
  var available: Boolean = true

  /** Store the events by the CommandHandler.
    * @param evt Event.
    */
  def storeEventByCH(evt: Event)

  /** Record the last sent event by the EventBus.
    * @param evt Event
    */
  def recordLastSentEvtByEB(evt: Event)

  /** Record the Processing Event by the EventBus
    * @param evt Event
    * @param evtHdlNum UUID of the event-handler.
    */
  def recordProcessingEvtByEB(evt: Event, evtHdlNum: UUID)

  /** Get recovery events by the EventBus in last system shutdown.
    * @param isLastEvtInProcessingCmds whethe the last event is in processing commands.
    * @return ListBuffer of RecoveryEvent and its relative Eventhandler's UUID.
    */
  def getRecEventsByEB(isLastEvtInProcessingCmds:Boolean): ListBuffer[(RecoveryEvent, UUID)]

  /** Get the last event UUID string by the EventBus.
    * @return String of last event's UUID.
    */
  def getLastEventByEB: String

  /** Remove all events record by EventBus. */
  def removeAllEventsByEB

  /** Remove a processed event by the Eventhandler.
    * @param evtID UUID of the event.
    * @param evtHdlNum UUID to identity a EventHandler.
    */
  def removeProcessedEvtByEH(evtID: UUID, evtHdlNum: UUID)

  /** Check whether a event exist.
    * @param cmdID UUID of command.
    * @param acctID UUID of an account.
    * @return whether this event exists.
    */
  def isEventExist(cmdID:UUID, acctID:UUID):Boolean
}

/** The implementation of IEventRecoveryService.
  * It is resonsible of the error-recovery of events.
  * @param stateRec ProcessingEvtRecStorage.
  * @param messageRec MessageEvtRecStorage.
  */
class EventRecoveryService(stateRec: ProcessingEvtRecStorage, messageRec: MessageEvtRecStorage)
  extends IEventRecoveryService{

  override def storeEventByCH(evt: Event){
    messageRec.storeEvent(evt)
  }
  
  override def recordLastSentEvtByEB(evt: Event){    
    evt match{
      case e:RecoveryEvent =>
      case e:Event => stateRec.updateLastSentEvent(e)
    }
  }
  
  override def recordProcessingEvtByEB(evt: Event, evtHdlNum: UUID){
    evt match{
      case e:RecoveryEvent =>
      case e:Event => stateRec.storeProcessingEvent(e, new Date(), evtHdlNum)
    }
  }
  
  override def removeProcessedEvtByEH(evtID: UUID, evtHdlNum: UUID){
    stateRec.removeProcessedEvent(evtID, evtHdlNum)  
  }
  
  override def getLastEventByEB:String = {
    stateRec.getLastSendEventMessage
  }
  
  override def getRecEventsByEB(isLastEvtInProcessingCmds:Boolean): ListBuffer[(RecoveryEvent, UUID)] = {
    val recEvts = new ListBuffer[(RecoveryEvent, UUID)]()

    val lastEvtID = stateRec.getLastSendEventID
    lastEvtID match{
      case None => return recEvts
      case Some(_) =>
    }

    // its data format is[evtIDtoString + evtHdlNum, Date.toString].
    val processedEvts = stateRec.getProcessingEvent
    val allEvts = messageRec.getAllEvents

    val treeMap: TreeMap[Date, String] = new TreeMap[Date, String](new Comparator[Date] {
      override def compare(date1: Date, date2: Date): Int = {
        return date1.compareTo(date2)
      }
    })
    processedEvts.foreach(pair => treeMap.put(new Date(pair._2), pair._1))
    
    var iter = treeMap.keySet().iterator()
    var nextRedoEvtDate: Date = new Date()
    if (iter.hasNext())
      nextRedoEvtDate = iter.next()

    breakable {
      allEvts.foreach(evt => {
        // if this event is last-sent-evt
        if (evt.eventID.equals(lastEvtID)) {
          if (!isLastEvtInProcessingCmds) {
            val pair = (getRecEvent(evt), null)
            recEvts += pair
          }
          break
        } 
        else {
          // if this event is in all-processing-evts
          while ((treeMap.get(nextRedoEvtDate) != null) &&
            (evt.eventID.toString.equals(treeMap.get(nextRedoEvtDate).substring(0, 36)))) {
            val pair = (getRecEvent(evt), UUID.fromString(treeMap.get(nextRedoEvtDate).substring(36)))
            recEvts += pair
            if (iter.hasNext()) {
              nextRedoEvtDate = iter.next()
            } else {
              nextRedoEvtDate = new Date()
            }
          }
        }

      })
    }
    //There can be RecEvent in evts
    return recEvts
  }
  
  override def removeAllEventsByEB {
    stateRec.removeLastSendEvent
    stateRec.removeAllEvents
  }
  
  override def isEventExist(cmdID:UUID, acctID:UUID):Boolean = {
    messageRec.isEventExist(cmdID, acctID)
  }

  /** Private function to transform an event into a recovery-event.
    * @param evt Event.
    * @return
    */
  private def getRecEvent(evt: Event): RecoveryEvent={
    evt match{
    case e:TransferOutEvent =>
      return TransferOutRecEvent(e.eventID, e.commandID, e.committedTime, e.accountID, e.currency, e.balance,
        e.revision, e.amountOut, e.transferInAccountID, e.amountIn)

    case e:TransferInEvent =>
      return TransferInRecEvent(e.eventID, e.commandID, e.committedTime, e.accountID, e.currency, e.balance,
        e.revision, e.amountIn, e.transferOutAccountID, e.amountOut)

    case e:WithdrawEvent =>
      return WithdrawRecEvent(e.eventID, e.commandID, e.committedTime, e.accountID, e.currency, e.balance,
        e.revision, e.amountWithdrawn, e.amountOut)

    case e:DepositEvent =>
      return DepositRecEvent(e.eventID, e.commandID, e.committedTime, e.accountID, e.currency, e.balance,
        e.revision, e.amountDeposited, e.amountIn)

    case e:RegisterAccountEvent =>
      return RegisterAccountRecEvent(e.eventID, e.commandID, e.committedTime, e.accountID, e.currency,
        e.balance, e.revision, e.userName)

    case e:DeleteAccountEvent =>
      return DeleteAccountRecEvent(e.eventID, e.commandID, e.committedTime, e.accountID, e.currency,
        e.balance, e.revision)

    case e:ChangeUserNameEvent =>
      return ChangeUserNameRecEvent(e.eventID, e.commandID, e.committedTime, e.accountID, e.currency,
        e.balance, e.revision, e.newUserName)
    }
  }
}