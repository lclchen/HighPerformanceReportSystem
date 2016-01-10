package rpsystem.domain

import rpsystem.recovery._
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._
import java.util.UUID

trait IEventBus {
  
  //Error-Recovery
  def recordLastReceivedEvt(evt:Event)
  def recordAllSentEvt(evt:Event, num:UUID)
  def getRecoveryEvents():ListBuffer[RecoveryEvent]
  def removeRecInfo
}

class EventBus(cmdRecSrv:ICommandRecoveryService, evtRecSrv:IEventRecoveryService) extends IEventBus{
   var mode:EventBus.BROADCAST_MODE = new EventBus.BROADCAST_MODE_NOPACK()
   
   def setMode(newMode:EventBus.BROADCAST_MODE){
     mode = newMode
   }
   
   def isPackageFull(evt:Event, pack:ListBuffer[Event]):ListBuffer[Event]={
     var sentEvts:ListBuffer[Event] = null
     mode match{
       case m: EventBus.BROADCAST_MODE_NOPACK =>
         pack += evt
         sentEvts = pack.clone
         pack.clear
       case m: EventBus.BROADCAST_MODE_PACK_NUM =>
         pack += evt
         if(pack.length >= m.packNum){
           sentEvts = pack.clone
           pack.clear
         }
       case m: EventBus.BROADCAST_MODE_PACK_TIMECYCLE =>
         pack += evt
         if(System.currentTimeMillis - pack(0).committedTime.getTime >= m.packDuration){
           sentEvts = pack.clone
           pack.clear
         }
     }
     return sentEvts
   }
   
   override def recordLastReceivedEvt(evt:Event){
     if(evtRecSrv.available)
       evtRecSrv.recordLastSentEvtByEB(evt)
     if(cmdRecSrv.available)
       cmdRecSrv.removeProcessedCmdByEB(evt.commandID, evt.accountID)
   }
   
   override def recordAllSentEvt(evt:Event, num:UUID){
     if(evtRecSrv.available)
       evtRecSrv.recordProcessingEvtByEB(evt, num)
   }

  override def getRecoveryEvents(): ListBuffer[RecoveryEvent] = {
    if(!evtRecSrv.available && !cmdRecSrv.available)
      return new ListBuffer[RecoveryEvent]()
      
    val message = evtRecSrv.getLastEventByEB
    var allEvts: ListBuffer[(RecoveryEvent, UUID)] = null
    if (message.length != 108) {
      allEvts = evtRecSrv.getRecEventsByEB(false)
    } else {
      allEvts = evtRecSrv.getRecEventsByEB(cmdRecSrv.isCmdExistByEB(UUID.fromString(message.substring(36, 72)), UUID.fromString(message.substring(72))))
    }
    
    var lastEventID: String = ""
    val evts: ListBuffer[RecoveryEvent] = new ListBuffer[RecoveryEvent]()
    breakable {
      allEvts.foreach(evt => {
        if (! evt._1.eventID.toString.equals(lastEventID)){
          lastEventID = evt._1.eventID.toString
          evts += evt._1
        }
      })
    }
    return evts
  }
   
   override def removeRecInfo{
     if(evtRecSrv.available)
    	 evtRecSrv.removeAllEventsByEB
   }
}

object EventBus {
  trait BROADCAST_MODE
  
  case class BROADCAST_MODE_NOPACK() extends BROADCAST_MODE
  case class BROADCAST_MODE_PACK_NUM(packNum:Int) extends BROADCAST_MODE
  case class BROADCAST_MODE_PACK_TIMECYCLE(packDuration:Long) extends BROADCAST_MODE
}