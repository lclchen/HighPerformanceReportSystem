/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.domain

import java.util.UUID

/** The trait or interface of Repository.
  * @tparam T T should be son of AggregateRoot.
  */
trait IRepository[T <: AggregateRoot] {
  /** Save the aggregate-root into repository */
  def save(aggregate: T, expectedVersion: Int): Unit

  /** Get a aggregate-root according to its uuid.
    * @param id UUID of aggregate-root.
    * @return Option[T] the result of aggregate-root.
    */
  def getById(id: UUID): Option[T]
}

/** The implementation of IRepository.
  * It's responsible for access to events and aggregate-root.
  * @param acctStorage AccountStore.
  * @param evtStorage EventStore.
  */
class Repository(val acctStorage: AccountStore, val evtStorage: EventStore) extends IRepository[AccountAggr] {
  /** Add account aggregate-root into account-store. */
  def add(acct: AccountAggr): Unit = {
    evtStorage.saveEvents(acct.id, acct.getUncommittedChanges, -1)
    acct.MarkChangesAsCommitted
    acctStorage.addAccount(acct)
  }

  /** Save the aggregate-root into repository
    * @param acct Account AggregateRoot.
    * @param expectedVersion It is -1 if no need to check the version of account in datbase.
    */
  override def save(acct: AccountAggr, expectedVersion: Int): Unit = {
    expectedVersion match {
      case -1 =>
        // save events.
        evtStorage.saveEvents(acct.id, acct.getUncommittedChanges, -1)
        // mark events as committed.
        acct.MarkChangesAsCommitted
        // save accounts
        acctStorage.saveAccount(acct, -1)
      case 0 =>
        // donnot save snapshot.
        evtStorage.saveEvents(acct.id, acct.getUncommittedChanges, -1)
        acct.MarkChangesAsCommitted
      case _ =>
        // save account and events with expected version.
        evtStorage.saveEvents(acct.id, acct.getUncommittedChanges, expectedVersion)
        acct.MarkChangesAsCommitted
        acctStorage.saveAccount(acct, expectedVersion)
    }
  }

  /** Get Account aggregate-root by its uuid.
    * @param accid UUID of account aggregate-root.
    * @return Option[T] the result of aggregate-root.
    */
  override def getById(accid: UUID): Option[AccountAggr] = {
    acctStorage.getAccount(accid) match{
      case Some(acct) => 
        acct.loadFromHistory(evtStorage.getEvents(acct.id, acct.getRevision))
        return Some(acct)
      case None =>
        return None
    }
  }
}
