/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.domain

import rpsystem.persistence._
import java.util.UUID

/** Trait or Interface of AccountStore */
trait IAccountStore

/** An implementation of IAccountStore
  * It is responsible for save, update or get accounts from the harddisk persistence.
  * @param mongo MongoPersistence to store accounts.
  */
class AccountStore(mongo: MongoPersistence) extends IAccountStore {

  /** Update account status.
    * @param acct AccountAggregate
    * @param expectedVersion expectedVesion. It equals to -1 if not need to check the previous version.
    */
  def saveAccount(acct: AccountAggr, expectedVersion: Int) {
    expectedVersion match {
      // get the account without checking the previous version of account in database.
      case -1 =>
        mongo.saveSnapshot(acct)
      // get the account and check whether it has the correct version with the expected version.
      case version: Int => {
        getAccount(acct.id) match {
          case Some(account) => {
            if(account.getRevision == expectedVersion){
              mongo.saveSnapshot(acct)
            }else{
              throw new Exception("Concurrent Exception: not expected Version")
            }
          }
          case None =>
            logger.warn("AccountStore cannot save this Account: " + acct.toString +
              " - because this account does not exist in the DB")
            throw new Exception("Account not found:saveAccount")
        }
      }
    }
  }

  /** Get an account from the database.
    * @param accountID UUID of this account.
    * @return Option[AccountAggregate].
    */
  def getAccount(accountID: UUID): Option[AccountAggr] = {
    mongo.getSnapshot(accountID)
  }

  /** Add a new account into the database.
    * @param acct AccountAggregateRoot
    */
  def addAccount(acct: AccountAggr): Unit = {
    mongo.addSnapshot(acct)
  }

  /** Check whether a account exists in the database.
    * @param accoutID UUID of the account.
    * @return whether this account exists in the databse.
    */
  def isAccountExist(accoutID: UUID): Boolean = {
    mongo.isSnapshotExist(accoutID)
  }
}

