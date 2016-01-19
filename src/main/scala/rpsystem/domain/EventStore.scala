/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.domain

import rpsystem.persistence._
import java.util.UUID
import scala.collection.mutable.ListBuffer

/** The trait of EventStore */
trait IEventStore {
  /** save events into the event-store */
  def saveEvents(aggrID:UUID, events:Traversable[Event], expectedVersion:Int)
}

/** The implementation of IEventStore.
  * It is responsible for storing events and make them in persistency.
  * @param dataBase MongoPersistence
  */
class EventStore(dataBase: MongoPersistence) extends IEventStore{
  /** Save events in persistence.
    * @param acctID UUID of bank account.
    * @param events Traversable[Event] and need to be stored into the database.
    * @param expectedVersion the expectedVersion of account snapshot, it's -1 if no need to check the account version.
    */
  def saveEvents(acctID: UUID, events: Traversable[Event], expectedVersion: Int){
    var version: Int = expectedVersion
    events.foreach(evt => {
      expectedVersion match{
        case -1 =>
          dataBase.saveEvent(acctID, evt, -1)
        case _ =>
          version += 1
          dataBase.saveEvent(acctID, evt, version)        
      }
    })
  }

  def saveEvent(evt: Event) {
    dataBase.saveEvent(evt)
  }

  /** Get events from the database after a certain expected version.
    * @param aggregateID UUID of bank account.
    * @param expectedVersion expected version of events.
    * @return ListBuffer[Event] as the events.
    */
  def getEvents(aggregateID: UUID, expectedVersion:Int): ListBuffer[Event] = {
    // get the events whose version is expectedVersion+1, ..+2, ...
    val allEvents = dataBase.getEventsByID(aggregateID)
    var nextVersion = expectedVersion + 1
    val events = new ListBuffer[Event]()
    allEvents.foreach(evt => {
      if(evt.revision > expectedVersion)
        events += evt
    })
    
    //sort the events
    val resultEvents = new ListBuffer[Event]()
    var isContinue = true
    while(isContinue){
      isContinue = false
      events.foreach(evt => {
        if(evt.revision == nextVersion){
          isContinue = true
          nextVersion += 1
          resultEvents += evt
        }         
      })
    }
    resultEvents
  }

  /** Check whether a event exist.
    * @param cmdID UUID of the command.
    * @param acctID UUID of the bank account.
    * @return whether such event exist.
    */
  def isEventExist(cmdID: UUID, acctID: UUID): Boolean = {
	  dataBase.isEventExist(cmdID, acctID)
  }
}