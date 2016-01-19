/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.util

import rpsystem.domain._
import java.util.UUID
import java.math.BigDecimal

/** The trait of Factory. */
trait IFactory

/** The factory for Account AggregateRoot. */
object AccountAggrFactory extends IFactory{

  /** Get a account aggregate-root for the factory.
    * @param id UUID of account.
    * @param username String username of account.
    * @param currency String currency of this account.
    * @param balance BigDecimal, the balance of account.
    * @param activated Boolean, whether the account is active.
    * @param revision Int, the revision of account.
    * @return Account AggregateRoot.
    */
  def getAccountAggr(id:UUID, username:String="", currency:String="RMB", 
      balance:BigDecimal=new BigDecimal(0), activated:Boolean=true, revision:Int = 0):AccountAggr = {
    val account = new AccountAggr(id, username, currency, balance)
    account.setActivated(activated)
    account.setRevision(revision)
    return account
  }
}
