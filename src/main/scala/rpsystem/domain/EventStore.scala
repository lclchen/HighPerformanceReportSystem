package rpsystem.domain

import rpsystem.persistence._
import java.util.HashMap
import java.util.UUID
import scala.collection.mutable.ListBuffer

trait IEventStore {
  def saveEvents(aggrID:UUID, events:Traversable[Event], expectedVersion:Int)
}

class EventStore(dataBase: MongoPersistence) extends IEventStore{
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

  def getEvents(aggregateID: UUID, expectedVersion:Int): ListBuffer[Event] = {
    //get the events whose version is expectedVersion+1, ..+2, ...
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

  def isEventExist(cmdID: UUID, acctID: UUID): Boolean = {
	dataBase.isEventExits(cmdID, acctID)
  }
}