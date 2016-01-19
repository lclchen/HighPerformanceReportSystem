/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.recovery

import java.util.UUID
import java.util.Date

import redis.clients.jedis._
import com.mongodb.WriteConcern
import com.mongodb.casbah.MongoDB
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.commons.Imports.MongoDBObject
import com.mongodb.casbah.commons.Imports.DBObject

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._

import rpsystem.domain._
import rpsystem.persistence._

/** Basic interface for error-recovery. */
class RecoveryStorage {

  /** whether this error-recovery module is available. */
  var available: Boolean = true

  def setAvailable(isAvailable: Boolean): Unit = {
    available = isAvailable
  }

  def isAvailable: Boolean = {
    return available
  }
}

/** The trait for command error-recovery. */
trait CmdRecoveryStorage extends RecoveryStorage

/** The trait for event error-recovery. */
trait EvtRecoveryStorage extends RecoveryStorage

/** The trait for processing or unfinished commands recovery. */
trait ProcessingCmdRecStorage extends CmdRecoveryStorage {

  def updateLastSentCommand(cmd: Command)

  def getLastSendCommandID(): Option[UUID]

  def getLastSendCmdIDAcctID: String

  def removeLastSendCommand

  def storeProcessingCommand(cmd: Command, date: Date)

  // its data format is [cmdID+evtID.toString, Date.toString]
  def getProcessingCommand(): HashMap[String, String]

  def removeProcessedCommand(cmdID: UUID, acctID: UUID)

  def removeAllCommands()
}

/** The trait for missing commands recovery. */
trait MessageCmdRecStorage extends CmdRecoveryStorage {

  def storeCommand(cmd: Command)

  def getCommand(cmdID: UUID): Option[Command]

  def findMissingCommands(cmdID: UUID): ListBuffer[Command]

  def getAllCommands(): ListBuffer[Command]
}

/** The trait for unfinished or processing events recovery. */
trait ProcessingEvtRecStorage extends EvtRecoveryStorage {

  def updateLastSentEvent(evt: Event)

  def getLastSendEventID(): Option[UUID]

  def getLastSendEventMessage: String

  def removeLastSendEvent

  def storeProcessingEvent(evt: Event, date: Date, evtHdlNum: UUID)

  def getProcessingEvent(): HashMap[String, String]

  def removeProcessedEvent(evtID: UUID, evtHdlNum: UUID)

  def removeAllEvents()
}

/** The trait for missing events recovery. */
trait MessageEvtRecStorage extends EvtRecoveryStorage {
  def storeEvent(evt: Event)

  def isEventExist(cmdID: UUID, acctID: UUID): Boolean

  def getEvent(evtID: UUID): Option[Event]

  def getEvents(cmdID: UUID, acctID: UUID): ListBuffer[Event]

  def findMissingEvents(evtID:UUID): ListBuffer[Event]

  def getAllEvents(): ListBuffer[Event]
}

/** The Redis implementation of ProcessingCmdRecStorage. */
class RedisProcessingCmdRecStorage(client: Jedis) extends ProcessingCmdRecStorage {

  override def updateLastSentCommand(cmd: Command) {
    client.set("LastSentCommand", cmd.commandID.toString + cmd.accountID.toString)
  }

  override def getLastSendCommandID: Option[UUID] = {
    val str = client.get("LastSendCommand")
    str match {
      case null => return None
      case _ => return Some(UUID.fromString(str.substring(0, 36)))
    }
  }

  override def getLastSendCmdIDAcctID: String = {
    val result = client.get("LastSendCommand")
    if(result == null)
      return ""
    return result
  }

  override def removeLastSendCommand() {
    client.del("LastSendCommand")
  }

  override def storeProcessingCommand(cmd: Command, date: Date) {
    client.hset("ProcessingCommands", cmd.commandID.toString + cmd.accountID.toString, date.toString)
  }

  override def getProcessingCommand(): HashMap[String, String] = {
    // its data format is [cmdID+acctID.toString, Date.toString]
    var hashMap: HashMap[String, String] = new HashMap[String, String]
    mapAsScalaMap(client.hgetAll("ProcessingCommands")).foreach(pair => hashMap += pair)
    return hashMap
  }

  override def removeProcessedCommand(cmdID: UUID, acctID: UUID) {
    client.hdel("ProcessingCommands", cmdID.toString + acctID.toString)
  }

  override def removeAllCommands() = {
    client.del("ProcessingCommands")
  }
}

/** The MongoDB implementation of MessageCmdRecStorage. */
class MongoMessageCmdRecStorage(mongoCol: MongoCollection) extends MessageCmdRecStorage {

  override def storeCommand(cmd: Command) {
    mongoCol.insert(MongoORM.getObjFromCmd(cmd), WriteConcern.SAFE)
  }

  override def getCommand(cmdID: UUID): Option[Command] = {
    val obj = MongoDBObject("CommandID" -> cmdID.toString)
    mongoCol.findOne(obj) match {
      case Some(resultObj) => return Some(MongoORM.getCmdFromDBObj(resultObj))
      case _ => return None
    }
  }

  override def findMissingCommands(cmdID: UUID): ListBuffer[Command] = {
    var findThisCommand = false
    val listbuf = new ListBuffer[Command]
    mongoCol.find().foreach(obj => {
      findThisCommand match {
        case true =>
          listbuf += MongoORM.getCmdFromDBObj(obj)

        case false =>
          if (MongoORM.getUUID(obj, "CommandID").equals(cmdID))
            findThisCommand = true
      }
    })
    return listbuf
  }

  override def getAllCommands(): ListBuffer[Command] = {
    val listbuf = new ListBuffer[Command]
    mongoCol.find().foreach(obj => listbuf += MongoORM.getCmdFromDBObj(obj))
    return listbuf
  }
}

/** The Redis implementation of ProcessingEvtRecStorage. */
class RedisProcessingEvtRecStorage(client: Jedis) extends ProcessingEvtRecStorage {

  override def updateLastSentEvent(evt: Event) {
    client.set("LastSentEvent", evt.eventID.toString + evt.commandID.toString + evt.accountID.toString)
  }

  override def getLastSendEventID(): Option[UUID] = {
    val str = client.get("LastSendEvent")
    str match {
      case null => return None
      case _ => return Some(UUID.fromString(str.substring(0, 36)))
    }
  }
  
  override def getLastSendEventMessage: String = {
    val result = client.get("LastSendEvent")
    if (result == null)
      return ""
    return result
  }

  override def removeLastSendEvent {
    client.del("LastSendEvent")
  }

  override def storeProcessingEvent(evt: Event, date: Date, evtHdlNum: UUID) {
    client.hset("ProcessingEvents", evt.eventID.toString + evtHdlNum.toString, date.toString)
  }

  override def getProcessingEvent(): HashMap[String, String] = {
    // return [EventID.toString, Date.toString]
    var hashMap: HashMap[String, String] = new HashMap[String, String]
    mapAsScalaMap(client.hgetAll("ProcessingEvents")).foreach(pair => hashMap += pair)
    return hashMap
  }

  override def removeProcessedEvent(evtID: UUID, evtHdlNum: UUID) {
    client.hdel("ProcessingEvents", evtID.toString + evtHdlNum.toString)
  }
  
  override def removeAllEvents(){
    client.del("ProcessingEvents")
  }
}

/** The MongoDB implementation of MessageEvtRecStorage. */
class MongoMessageEvtRecStorage(mongoCol: MongoCollection) extends MessageEvtRecStorage {

  override def storeEvent(evt: Event) {
    mongoCol.insert(MongoORM.getObjFromEvent(evt), WriteConcern.SAFE)
  }

  override def isEventExist(cmdID: UUID, acctID: UUID): Boolean = {
    val obj = MongoDBObject("CommandID" -> cmdID.toString, "AccountID" -> acctID.toString)
    mongoCol.find(obj).foreach(e => return true)
    return false
  }

  override def getEvent(evtID: UUID): Option[Event] = {
    val obj = MongoDBObject("EventID" -> evtID.toString)
    mongoCol.findOne(obj) match {
      case Some(resultObj) => return Some(MongoORM.getEventFromDBObj(resultObj))
      case _ => return None
    }
  }

  override def getEvents(cmdID: UUID, acctID: UUID): ListBuffer[Event] = {
    val buf: ListBuffer[Event] = new ListBuffer[Event]()
    val obj = MongoDBObject("CommandID" -> cmdID.toString, "AccountID" -> acctID.toString)
    mongoCol.find(obj).foreach(e => buf += MongoORM.getEventFromDBObj(e))
    return buf
  }

  override def findMissingEvents(evtID:UUID): ListBuffer[Event] = {
    var findThisEvent = false
    val listbuf = new ListBuffer[Event]
    mongoCol.find().foreach(obj => {
      findThisEvent match {
        case true =>
          listbuf += MongoORM.getEventFromDBObj(obj)

        case false => 
          if (MongoORM.getUUID(obj, "EventID").equals(evtID)) 
            findThisEvent= true
      }
    })
    return listbuf
  }
  
  override def getAllEvents: ListBuffer[Event] = {
    val buf = new ListBuffer[Event]
    mongoCol.find().foreach(obj => buf += MongoORM.getEventFromDBObj(obj))
    return buf
  }
}