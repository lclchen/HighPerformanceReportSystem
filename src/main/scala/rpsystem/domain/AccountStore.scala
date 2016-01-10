package rpsystem.domain

import rpsystem.persistence._
import com.mongodb._
import java.util.UUID

trait IAccountStore

class AccountStore(mongo: MongoPersistence) extends IAccountStore{
  def saveAccount(acct: AccountAggr, expectedVersion: Int) {
    expectedVersion match {
      case -1 =>
        mongo.saveSnapshot(acct)
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
            logger.warn("AccountStore cannot save this Account: " + acct.toString + " - because this account does not exist in the DB")
            throw new Exception("Account not found:saveAccount")
        }
      }
    }
  }

  def getAccount(accountID: UUID): Option[AccountAggr] = {
    mongo.getSnapshot(accountID)
  }

  def addAccount(acct: AccountAggr): Unit = {
    mongo.addSnapshot(acct)
  }

  def isAccountExist(accoutID: UUID): Boolean = {
    mongo.isSnapshotExist(accoutID)
  }
}

