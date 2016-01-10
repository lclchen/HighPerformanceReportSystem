package rpsystem.persistence

import rpsystem.domain._
import java.util.UUID
import java.util.Date
import java.math.BigDecimal
import scala.collection.mutable.ListBuffer
import com.mongodb.casbah.commons.Imports.DBObject
import com.mongodb.casbah.commons.Imports.MongoDBObject

object MongoORM {
  def getEventFromDBObj(obj: DBObject): Event = {
    obj.get("EventType").toString match {
      case "TransferOutEvent" => return TransferOutEvent(getUUID(obj, "EventID"), getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getStr(obj, "Currency"), getDecimal(obj, "Balance"), getInt(obj, "Revision"), getDecimal(obj, "AmountOut"), getUUID(obj, "TransferInAccountID"), getDecimal(obj, "AmountIn"))
      case "TransferInEvent" => return TransferInEvent(getUUID(obj, "EventID"), getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getStr(obj, "Currency"), getDecimal(obj, "Balance"), getInt(obj, "Revision"), getDecimal(obj, "AmountIn"), getUUID(obj, "TransferOutAccountID"), getDecimal(obj, "AmountOut"))
      case "WithdrawEvent" => return WithdrawEvent(getUUID(obj, "EventID"), getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getStr(obj, "Currency"), getDecimal(obj, "Balance"), getInt(obj, "Revision"), getDecimal(obj, "AmountWithdrawn"), getDecimal(obj, "AmountOut"))
      case "DepositEvent" => return DepositEvent(getUUID(obj, "EventID"), getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getStr(obj, "Currency"), getDecimal(obj, "Balance"), getInt(obj, "Revision"), getDecimal(obj, "AmountDeposited"), getDecimal(obj, "AmountIn"))
      case "RegisterAccountEvent" => return RegisterAccountEvent(getUUID(obj, "EventID"), getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getStr(obj, "Currency"), getDecimal(obj, "Balance"), getInt(obj, "Revision"), getStr(obj, "UserName"))
      case "DeleteAccountEvent" => return DeleteAccountEvent(getUUID(obj, "EventID"), getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getStr(obj, "Currency"), getDecimal(obj, "Balance"), getInt(obj, "Revision"))
      case "ChangeUserNameEvent" => return ChangeUserNameEvent(getUUID(obj, "EventID"), getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getStr(obj, "Currency"), getDecimal(obj, "Balance"), getInt(obj, "Revision"), getStr(obj, "NewUserName"))
      case _ => throw new Exception("getEventFromDBObj: no matched event.")
    }
  }

  def getCmdFromDBObj(obj: DBObject): Command = {
    obj.get("CommandType").toString match {
      case "TransferCommand" => TransferCommand(getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getDecimal(obj, "AmountOut"), getUUID(obj, "TransferInAccountID"), getDecimal(obj, "AmountIn"))
      case "TransferOutCommand" => TransferOutCommand(getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getDecimal(obj, "AmountOut"), getUUID(obj, "TransferInAccountID"), getDecimal(obj, "AmountIn"))
      case "TransferInCommand" => TransferInCommand(getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getDecimal(obj, "AmountIn"), getUUID(obj, "TransferOutAccountID"), getDecimal(obj, "AmountOut"))
      case "WithdrawCommand" => WithdrawCommand(getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getDecimal(obj, "AmountWithdrawn"), getDecimal(obj, "AmountOut"))
      case "DepositCommand" => DepositCommand(getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getDecimal(obj, "AmountDeposited"), getDecimal(obj, "AmountIn"))
      case "RegisterAccountCommand" => RegisterAccountCommand(getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getStr(obj, "UserName"), getStr(obj, "Currency"))
      case "DeleteAccountCommand" => DeleteAccountCommand(getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"))
      case "ChangeUserNameCommand" => ChangeUserNameCommand(getUUID(obj, "CommandID"), getDate(obj, "CommittedTime"), getUUID(obj, "AccountID"), getStr(obj, "NewUserName"))
      case _ => throw new Exception("getCmdFromDBObj: no matched command")
    }
  }

  def getAcctSnapshotFromDBObj(obj: DBObject): AccountAggr = {
    val acct = new AccountAggr(getUUID(obj, "AccountID"), getStr(obj, "UserName"), getStr(obj, "Currency"), getDecimal(obj, "Balance"))
    acct.setRevision(getInt(obj, "Revision"))
    acct.setActivated(getBoolean(obj, "Activated"))
    acct
  }

  //
  def getObjFromEvent(evt: Event): DBObject = {
    val obj = MongoDBObject()
    obj.put("EventID", evt.eventID.toString)
    obj.put("CommandID", evt.commandID.toString)
    obj.put("CommittedTime", evt.committedTime.toString)
    obj.put("AccountID", evt.accountID.toString)
    obj.put("Currency", evt.currency)
    obj.put("Balance", evt.balance.toString)
    obj.put("Revision", evt.revision)
    evt match {
      case e: TransferOutEvent =>
        obj.put("AmountOut", e.amountOut.toString()); obj.put("TransferInAccountID", e.transferInAccountID.toString()); obj.put("AmountIn", e.amountIn.toString())
      case e: TransferInEvent =>
        obj.put("AmountIn", e.amountIn.toString()); obj.put("TransferOutAccountID", e.transferOutAccountID.toString()); obj.put("AmountOut", e.amountOut.toString())
      case e: WithdrawEvent =>
        obj.put("AmountWithdrawn", e.amountWithdrawn.toString()); obj.put("AmountOut", e.amountOut.toString())
      case e: DepositEvent =>
        obj.put("AmountDeposited", e.amountDeposited.toString()); obj.put("AmountIn", e.amountIn.toString())
      case e: RegisterAccountEvent =>
        obj.put("UserName", e.userName); obj.put("Currency", e.currency)
      case e: DeleteAccountEvent =>
      case e: ChangeUserNameEvent => obj.put("NewUserName", e.newUserName)
      case _ => throw new Exception("getObjFromEvent: no matched event")
    }
    obj
  }

  def getObjFromCmd(cmd: Command): DBObject = {
    val obj = MongoDBObject()
    obj.put("CommandID", cmd.commandID.toString)
    obj.put("CommittedTime", cmd.committedTime.toString)
    obj.put("AccountID", cmd.accountID.toString)
    cmd match {
      case c: TransferCommand =>
        obj.put("AmountOut", c.amountOut.toString()); obj.put("TransferInAccountID", c.transferInAccountID.toString()); obj.put("AmountIn", c.amountIn.toString())
      case c: TransferOutCommand =>
        obj.put("AmountOut", c.amountOut.toString()); obj.put("TransferInAccountID", c.transferInAccountID.toString()); obj.put("AmountIn", c.amountIn.toString())
      case c: TransferInCommand =>
        obj.put("AmountIn", c.amountIn.toString()); obj.put("TransferOutAccountID", c.transferOutAccountID.toString()); obj.put("AmountOut", c.amountOut.toString())
      case c: WithdrawCommand =>
        obj.put("AmountWithdrawn", c.amountWithdrawn.toString()); obj.put("AmountOut", c.amountOut.toString())
      case c: DepositCommand =>
        obj.put("AmountDeposited", c.amountDeposited.toString()); obj.put("AmountIn", c.amountIn.toString())
      case c: RegisterAccountCommand =>
        obj.put("UserName", c.userName); obj.put("Currency", c.currency)
      case c: DeleteAccountCommand =>
      case c: ChangeUserNameCommand => obj.put("NewUserName", c.newUserName)
      case _ => throw new Exception("getObjFromCmd: no matched command")
    }
    obj
  }

  def getObjFromAcct(acct: AccountAggr): DBObject = {
    val obj = MongoDBObject()
    obj.put("AccountID", acct.id.toString)
    obj.put("UserName", acct.username)
    obj.put("Currency", acct.currency)
    obj.put("Balance", acct.balance.toString)
    obj.put("Revision", acct.getRevision)
    obj.put("Activated", acct.getActivated)
    obj
  }

  //
  def getUUID(obj: DBObject, key: String): UUID = {
    UUID.fromString(obj.get(key).toString)
  }

  def getBoolean(obj: DBObject, key: String): Boolean = {
    java.lang.Boolean.parseBoolean(obj.get(key).toString)
  }

  def getStr(obj: DBObject, key: String): String = {
    obj.get(key).toString()
  }

  def getInt(obj: DBObject, key: String): Int = {
    java.lang.Double.parseDouble(obj.get(key).toString).toInt
  }

  def getDecimal(obj: DBObject, key: String): java.math.BigDecimal = {
    new java.math.BigDecimal(obj.get(key).toString)
  }

  def getDate(obj: DBObject, key: String): Date = {
    new java.util.Date(obj.get(key).toString())
  }
}