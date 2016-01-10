package rpsystem.persistence

import rpsystem.domain._
import com.mongodb.casbah.Imports._
import com.mongodb.WriteConcern
import java.util.Date
import java.util.UUID
import scala.collection.mutable.ListBuffer
import com.sun.beans.decoder.TrueElementHandler
import com.sun.beans.decoder.TrueElementHandler

case class TransactionStream(transactionid: Int, user: Int, events: ListBuffer[Event])

class MongoPersistence(mongo: MongoDB) {
  val EventCollection = mongo("events")
  val SnapshotCollection = mongo("accounts")

  def ensureIndex(): Unit = {
    EventCollection.ensureIndex("AccountID")
    SnapshotCollection.ensureIndex("AccountID")
  }

  def saveEvent(aggregateid: UUID, evt: Event, expectedVersion: Int): Unit = {    
    if(aggregateid == evt.accountID)
      if(expectedVersion == -1 || expectedVersion == evt.revision)
        EventCollection.insert(MongoORM.getObjFromEvent(evt), WriteConcern.SAFE)
  }
  
  def saveEvent(evt:Event){
    EventCollection.insert(MongoORM.getObjFromEvent(evt), WriteConcern.SAFE)
  }

  def getEventsByID(aggregateID: UUID): ListBuffer[Event] = {
    val mongoobject = MongoDBObject("AccountID" -> aggregateID)
    val events = new ListBuffer[Event]()
    EventCollection.find(mongoobject).foreach(obj => events += MongoORM.getEventFromDBObj(obj))
    events
  }
  
  def isEventExits(cmdID:UUID, acctID:UUID):Boolean = {
    EventCollection.find(MongoDBObject("CommandID" -> cmdID)).foreach(obj=>{
      if(MongoORM.getEventFromDBObj(obj).accountID.toString.equals(acctID.toString))
        return true
    })
    return false
  }

  /*
  def AddStreamtoMongo(stream: TransactionStream) {
    val mongoobject = grater[TransactionStream].asDBObject(stream)
    EventCollection.insert(mongoobject, WriteConcern.SAFE)
  }
  * */

  def addSnapshot(account: AccountAggr): Unit = {
    val mongoObj = MongoORM.getObjFromAcct(account)
    SnapshotCollection.insert(mongoObj, WriteConcern.SAFE)
  }

  def saveSnapshot(account: AccountAggr): Unit = {
    val query = MongoDBObject("AccountID" -> account.id.toString)
    val mongoObject = MongoDBObject("$set" -> MongoDBObject("UserName" -> account.username, "Currency" -> account.currency, "Balance" -> account.balance.toString, "Revision" -> account.getRevision, "Activated" -> account.getActivated))
    mongo.setWriteConcern(WriteConcern.SAFE)
    SnapshotCollection.update(query, mongoObject)
  }

  def getSnapshot(accountID: UUID): Option[AccountAggr] = {
    val queryDBObject = MongoDBObject("AccountID" -> accountID.toString)
    val result = SnapshotCollection.findOne(queryDBObject)
    result match {
      case Some(e) =>
        val acct = new AccountAggr(getUUIDfromDB(e, "AccountID"), getStringfromDB(e, "UserName"), getStringfromDB(e, "Currency"), getBigDecimalfromDB(e, "Balance"))
        acct.setRevision(getIntfromDB(e, "Revision"))
        acct.setActivated(getBooleanfromDB(e, "Activated"))
        return Some(acct)
      case None =>
        logger.warn("getSnapshot:no found account: " + accountID)
        return None
    }
  }

  def isSnapshotExist(accountID: UUID): Boolean = {
    val query = MongoDBObject("AccountID" -> accountID.toString)
    SnapshotCollection .findOne(query) match{
      case Some(e) => return true 
      case _ => return false
    }
  }

  /*
   * private sub-function
  */
  private def getIntfromDB(dbobject: DBObject, key: String): Int = {
    java.lang.Double.parseDouble(dbobject.get(key).toString).toInt
  }

  private def getStringfromDB(dbobject: DBObject, key: String): String = {
    dbobject.get(key).toString
  }

  private def getBooleanfromDB(dbobject: DBObject, key: String): Boolean = {
    java.lang.Boolean.parseBoolean(dbobject.get(key).toString)
  }

  private def getUUIDfromDB(dbobject: DBObject, key: String): UUID = {
    java.util.UUID.fromString(dbobject.get(key).toString)
  }

  private def getBigDecimalfromDB(dbobject: DBObject, key: String): java.math.BigDecimal = {
    new java.math.BigDecimal(dbobject.get(key).toString)
  }
}