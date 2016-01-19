/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.persistence

import rpsystem.domain._
import com.mongodb.casbah.Imports._
import com.mongodb.WriteConcern
import java.util.Date
import java.util.UUID
import scala.collection.mutable.ListBuffer

case class TransactionStream(transactionid: Int, user: Int, events: ListBuffer[Event])

/** The trait of Persistence */
trait IPersistence {
  /** Save a event into persistence
    * @param evt Event
    */
  def saveEvent(evt:Event): Unit

  /** Check whether a event exists in the persistence.
    * @param evtId UUID of a event.
    * @return whether this event exists.
    */
  def isEventExist(evtId:UUID): Boolean

  /** Add snapshot into database.
    * @param account Account aggregate-root.
    */
  def addSnapshot(account: AccountAggr): Unit

  /** Save and update account snapshot into the datebase.
    * @param account Account aggregate-root.
    */
  def saveSnapshot(account: AccountAggr): Unit

  /** Check whether an account snapshot exists.
    * @param accountID UUID of account.
    * @return whether this account exists in the database.
    */
  def isSnapshotExist(accountID: UUID): Boolean
}

/** The MongoDB implementation of IPersistence.
  * It is responsible for storing accounts and events into the MongoDB.
  * @param mongo MongDB, MongoDB Databse.
  */
class MongoPersistence(mongo: MongoDB) extends IPersistence {
  /** MongoDB collection to store events. */
  val EventCollection = mongo("events")
  /** MongoDB collection to store accounts aggregate-root and snapshot. */
  val SnapshotCollection = mongo("accounts")

  /** Ensure the indexes for databse. */
  def ensureIndex(): Unit = {
    EventCollection.ensureIndex("AccountID")
    SnapshotCollection.ensureIndex("AccountID")
  }

  /** Save the event into the database.
    * @param aggregateid UUID of account aggregate-root.
    * @param evt Event.
    * @param expectedVersion expected-version, is -1 if no need to check the account version.
    */
  def saveEvent(aggregateid: UUID, evt: Event, expectedVersion: Int): Unit = {    
    if(aggregateid == evt.accountID)
      if(expectedVersion == -1 || expectedVersion == evt.revision)
        // save data using WriteConcern.SAFE in MongoDB.
        EventCollection.insert(MongoORM.getObjFromEvent(evt), WriteConcern.SAFE)
  }

  /** Save the event into the database.
    * @param evt Event Event.
    */
  override def saveEvent(evt:Event): Unit = {
    EventCollection.insert(MongoORM.getObjFromEvent(evt), WriteConcern.SAFE)
  }

  /** Get events according to the Account Aggregate-Root.
    * @param aggregateID UUID of Account Aggregate-Root.
    * @return ListBuffer[Event] of events result.
    */
  def getEventsByID(aggregateID: UUID): ListBuffer[Event] = {
    val mongoobject = MongoDBObject("AccountID" -> aggregateID)
    val events = new ListBuffer[Event]()
    EventCollection.find(mongoobject).foreach(obj => events += MongoORM.getEventFromDBObj(obj))
    return events
  }

  /** Check whether such events exist in the database.
    * @param cmdID UUID of command.
    * @param acctID UUID of Account aggregate-root.
    * @return whether events with such conditions exists.
    */
  def isEventExist(cmdID:UUID, acctID:UUID):Boolean = {
    EventCollection.find(MongoDBObject("CommandID" -> cmdID)).foreach(obj=>{
      if(MongoORM.getEventFromDBObj(obj).accountID.toString.equals(acctID.toString))
        return true
    })
    return false
  }

  /** Check whether event with certain event-uuid exist.
    * @param evtID UUID of event.
    * @return whether this event exists.
    */
  override def isEventExist(evtID: UUID):Boolean = {
    EventCollection.find(MongoDBObject("EventID" -> evtID)).foreach(obj=>
        return true
    )
    return false
  }

  /* Store TransactionStream into MongoDB.
  def AddStreamtoMongo(stream: TransactionStream) {
    val mongoobject = grater[TransactionStream].asDBObject(stream)
    EventCollection.insert(mongoobject, WriteConcern.SAFE)
  }
  */

  /** Add a new account snapshot into the MongoDB.
    * @param account Account aggregate-root.
    */
  override def addSnapshot(account: AccountAggr): Unit = {
    val mongoObj = MongoORM.getObjFromAcct(account)
    SnapshotCollection.insert(mongoObj, WriteConcern.SAFE)
  }

  /** Save and update the account snapshot or aggregate-root into the mongodb.
    * @param account Account aggregate-root.
    */
  override def saveSnapshot(account: AccountAggr): Unit = {
    val query = MongoDBObject("AccountID" -> account.id.toString)
    val mongoObject = MongoDBObject("$set" -> MongoDBObject("UserName" -> account.username,
      "Currency" -> account.currency, "Balance" -> account.balance.toString, "Revision" -> account.getRevision,
      "Activated" -> account.getActivated))
    mongo.setWriteConcern(WriteConcern.SAFE)
    SnapshotCollection.update(query, mongoObject)
  }

  /** Find and get a account snapshot from the MongoDb.
    * @param accountID UUID of the account aggregate-root.
    * @return Opetion[Account AggregateRoot]
    */
  def getSnapshot(accountID: UUID): Option[AccountAggr] = {
    val queryDBObject = MongoDBObject("AccountID" -> accountID.toString)
    val result = SnapshotCollection.findOne(queryDBObject)
    result match {
      case Some(e) =>
        val acct = new AccountAggr(getUUIDfromDB(e, "AccountID"), getStringfromDB(e, "UserName"),
          getStringfromDB(e, "Currency"), getBigDecimalfromDB(e, "Balance"))
        acct.setRevision(getIntfromDB(e, "Revision"))
        acct.setActivated(getBooleanfromDB(e, "Activated"))
        return Some(acct)

      case None =>
        logger.warn("getSnapshot:no found account: " + accountID)
        return None
    }
  }

  /** Check whether a account snapshot exists.
    * @param accountID UUID of account.
    * @return whether this account exists in the database.
    */
  override def isSnapshotExist(accountID: UUID): Boolean = {
    val query = MongoDBObject("AccountID" -> accountID.toString)
    SnapshotCollection .findOne(query) match{
      case Some(e) => return true 
      case _ => return false
    }
  }

  // below is private functions

  /** Get a Int param from a MongoDBObject account to the Key'name */
  private def getIntfromDB(dbobject: DBObject, key: String): Int = {
    java.lang.Double.parseDouble(dbobject.get(key).toString).toInt
  }

  /** Get a String param from a MongoDBObject account to the Key'name */
  private def getStringfromDB(dbobject: DBObject, key: String): String = {
    dbobject.get(key).toString
  }

  /** Get a Boolean param from a MongoDBObject account to the Key'name */
  private def getBooleanfromDB(dbobject: DBObject, key: String): Boolean = {
    java.lang.Boolean.parseBoolean(dbobject.get(key).toString)
  }

  /** Get a UUID param from a MongoDBObject account to the Key'name */
  private def getUUIDfromDB(dbobject: DBObject, key: String): UUID = {
    java.util.UUID.fromString(dbobject.get(key).toString)
  }

  /** Get a BigDecimal param from a MongoDBObject account to the Key'name */
  private def getBigDecimalfromDB(dbobject: DBObject, key: String): java.math.BigDecimal = {
    new java.math.BigDecimal(dbobject.get(key).toString)
  }
}