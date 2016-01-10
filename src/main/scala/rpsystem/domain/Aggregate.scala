package rpsystem.domain

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import java.util.Date
import java.util.UUID
import java.math.BigDecimal

abstract class AggregateRoot {
  val id: UUID
  protected var revision: Int = 0
  protected var changes = ListBuffer[Event]()
  
  protected def uncommittedChanges = changes
  def getUncommittedChanges = uncommittedChanges.toIterable
  
  def getRevision = revision
  def setRevision(newRevision: Int) {revision = newRevision}

  def handle: PartialFunction[Event, Unit]

  def applyChange(e: Event, isNew: Boolean = true) = {
    if (handle.isDefinedAt(e)) {
      handle(e)
    }
    if (isNew) changes = changes :+ e
  }

  def loadFromHistory(history: Traversable[Event])

  //protected def loadState(state: IMemento) = ???

  //def loadFromMemento(state: IMemento, streamId: UUID, streamRevision: Int)
}


class AccountAggr(val id: UUID, var username: String = "", var currency: String = "RMB", var balance: BigDecimal = new BigDecimal(0)) extends AggregateRoot {
  private var activated: Boolean = true
  
  override def loadFromHistory(history: Traversable[Event]){
    history.foreach(evt => {
      if(evt.revision == getRevision + 1){
        setRevision(evt.revision)
        changes += evt
      }
      else
        throw new Exception("loadFromHistory: revision not match")        
    })
  }

  def transferMoneyOut(cmdID: UUID, committedTime: Date, amountOut: BigDecimal, toAcctID: UUID, amountIn: BigDecimal): Unit = {
    if (amountOut.intValue() < 0)
      throw new Exception(id.toString + " : tranferring money below 0")
    applyChange(TransferOutEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance.subtract(amountOut), getRevision + 1, amountOut, toAcctID, amountIn))
  }

  def transferMoneyIn(cmdID: UUID, committedTime: Date, amountIn: BigDecimal, fromAcctID: UUID, amountOut: BigDecimal): Unit = {
    if (amountIn.intValue < 0)
      throw new Exception(id.toString + " : tranferring money below 0")
    applyChange(TransferInEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance.add(amountOut), getRevision + 1, amountIn, fromAcctID, amountOut))
  }

  def withdrawMoney(cmdID: UUID, committedTime: Date, amountWithdrawn: BigDecimal, amountOut: BigDecimal): Unit = {
    if (amountOut.intValue < 0)
      throw new Exception(id.toString + " : withdrawal money below 0")
    applyChange(WithdrawEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance.subtract(amountOut), getRevision + 1, amountWithdrawn, amountOut))
  }

  def depositMoney(cmdID: UUID, committedTime: Date, amountDeposited: BigDecimal, amountIn: BigDecimal): Unit = {
    if (amountIn.intValue < 0)
      throw new Exception(id.toString + " : deposit money below 0")
    applyChange(DepositEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance.add(amountIn), getRevision + 1, amountDeposited, amountIn))
  }

  def createAccount(cmdID: UUID, committedTime:Date, cmdUserName:String, cmdCurrency:String): Unit = {
    if (cmdUserName.length == 0)
      throw new Exception(id.toString + " : no username")
    applyChange(RegisterAccountEvent(UUID.randomUUID(), cmdID, committedTime, id, cmdCurrency, balance, getRevision + 1, cmdUserName))
  }

  def deleteAccount(cmdID: UUID, committedTime:Date): Unit = {
    applyChange(DeleteAccountEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance, getRevision + 1))
  }
  
  def changeUserName(cmdID: UUID, committedTime:Date, newUserName:String): Unit = {
    if (newUserName.length == 0)
      throw new Exception(id.toString + " : no new username")
    applyChange(ChangeUserNameEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance, getRevision + 1, newUserName))
  }

  //***
  def MarkChangesAsCommitted(): Unit = {
    changes.clear
  }

  override def applyChange(e: Event, isNew: Boolean = true) = {
    if (handle.isDefinedAt(e)) {
      handle(e)
    }
    if (isNew) changes = changes :+ e
  }

  def handle: PartialFunction[Event, Unit] = {
    case e: TransferOutEvent => handle(e)
    case e: TransferInEvent => handle(e)
    case e: WithdrawEvent => handle(e)
    case e: DepositEvent => handle(e)
    case e: RegisterAccountEvent => handle(e)
    case e: DeleteAccountEvent => handle(e)
    case e: ChangeUserNameEvent => handle(e)
    case _ => logger.warn("warn event in account aggregate");
  }

  def handle(e: TransferOutEvent): Unit = {
    balance = balance.subtract(e.amountOut)
    setRevision(getRevision + 1)
  }

  def handle(e: TransferInEvent): Unit = {
    balance = balance.add(e.amountIn)
    setRevision(getRevision + 1)
  }

  def handle(e: WithdrawEvent): Unit = {
    balance = balance.subtract(e.amountOut)
    setRevision(getRevision + 1)
  }

  def handle(e: DepositEvent): Unit = {
    balance = balance.add(e.amountIn)
    setRevision(getRevision + 1)
  }

  def handle(e: RegisterAccountEvent): Unit = {
    activated = true
    username = e.userName
    currency = e.currency
    setRevision(getRevision + 1)
  }

  def handle(e: DeleteAccountEvent): Unit = {
    activated = false
    setRevision(getRevision + 1)
  }
  
  def handle(e: ChangeUserNameEvent): Unit = {
    username = e.newUserName
    setRevision(getRevision + 1)
  }
  
  
  //***


  def setActivated(isActivated: Boolean): Unit = {
    activated = isActivated
  }

  def getActivated: Boolean = {
    activated
  }
}