package rpsystem.domain

import scala.collection.mutable.HashMap
import scala.reflect._
import java.util.Date
import java.util.UUID

trait IRepository[T <: AggregateRoot] {
  def save(aggregate: T, expectedVersion: Int): Unit
  def getById(id: UUID): Option[T]
}

class Repository(val acctStorage: AccountStore, val evtStorage: EventStore) extends IRepository[AccountAggr] {
  def add(acct: AccountAggr): Unit = {
    evtStorage.saveEvents(acct.id, acct.getUncommittedChanges, -1)
    acct.MarkChangesAsCommitted //event
    acctStorage.addAccount(acct) //account
  }

  override def save(acct: AccountAggr, expectedVersion: Int): Unit = {
    expectedVersion match {
      case -1 =>
        evtStorage.saveEvents(acct.id, acct.getUncommittedChanges, -1) //save events
        acct.MarkChangesAsCommitted //mark events committed
        acctStorage.saveAccount(acct, -1) //save account
      case 0 =>
        evtStorage.saveEvents(acct.id, acct.getUncommittedChanges, -1)
        acct.MarkChangesAsCommitted//don't save snapshot
      case _ =>
        evtStorage.saveEvents(acct.id, acct.getUncommittedChanges, expectedVersion) //save events
        acct.MarkChangesAsCommitted //mark events committed
        acctStorage.saveAccount(acct, expectedVersion) //save account
    }
  }

  override def getById(accid: UUID): Option[AccountAggr] = {
    acctStorage.getAccount(accid) match{
      case Some(acct) => 
        acct.loadFromHistory(evtStorage.getEvents(acct.id, acct.getRevision))
        return Some(acct)
      case None => return None       
    }
    //reconstruct the aggregate state by events and tem-state
  }
}
