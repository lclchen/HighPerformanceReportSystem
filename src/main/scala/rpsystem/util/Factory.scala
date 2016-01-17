package rpsystem.util

import rpsystem.domain._
import java.util.UUID
import java.math.BigDecimal

trait IFactory

object AccountAggrFactory extends IFactory{
  def getAccountAggr(id:UUID, username:String="", currency:String="RMB", 
      balance:BigDecimal=new BigDecimal(0), activated:Boolean=true, revision:Int = 0):AccountAggr = {
    
    val account = new AccountAggr(id, username, currency, balance)
    account.setActivated(activated)
    account.setRevision(revision)
    return account
  }
}
