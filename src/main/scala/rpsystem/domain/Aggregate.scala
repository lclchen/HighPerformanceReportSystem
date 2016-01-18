/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.domain

import scala.collection.mutable.ListBuffer

import java.util.Date
import java.util.UUID
import java.math.BigDecimal

/** The abstract class and definition of AggregateRoot */
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

/** The implementation of Account-AggregateRoot in a bank domain.
  * It handles commands to update aggregate-root's status and generate events.
  * @param id UUID: override the parameter
  * @param username String: the username of this account.
  * @param currency String: the currency of this account.
  * @param balance java.util.BigDecimal: The balance of account.
  */
class AccountAggr(val id: UUID, var username: String = "", var currency: String = "RMB", var balance: BigDecimal = new BigDecimal(0)) extends AggregateRoot {
  // whether the account is active.
  private var activated: Boolean = true

  /** Load history events.
    * @param history Traversable[Event]: history events after this snapshot.
    */
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

  /** transfer money to the other account */
  def transferMoneyOut(cmdID: UUID, committedTime: Date, amountOut: BigDecimal, toAcctID: UUID,
                       amountIn: BigDecimal): Unit = {
    // check the account balance if it below 0.
    if (amountOut.intValue() < 0)
      throw new Exception(id.toString + " : tranferring money below 0")
    applyChange(TransferOutEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance.subtract(amountOut),
      getRevision + 1, amountOut, toAcctID, amountIn))
  }

  /** get transferred money from the other account */
  def transferMoneyIn(cmdID: UUID, committedTime: Date, amountIn: BigDecimal, fromAcctID: UUID,
                      amountOut: BigDecimal): Unit = {
    if (amountIn.intValue < 0)
      throw new Exception(id.toString + " : tranferring money below 0")
    applyChange(TransferInEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance.add(amountOut),
      getRevision + 1, amountIn, fromAcctID, amountOut))
  }

  /** withdraw money from the bank */
  def withdrawMoney(cmdID: UUID, committedTime: Date, amountWithdrawn: BigDecimal, amountOut: BigDecimal): Unit = {
    if (amountOut.intValue < 0)
      throw new Exception(id.toString + " : withdrawal money below 0")
    applyChange(WithdrawEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance.subtract(amountOut),
      getRevision + 1, amountWithdrawn, amountOut))
  }

  /** deposit money into the bank */
  def depositMoney(cmdID: UUID, committedTime: Date, amountDeposited: BigDecimal, amountIn: BigDecimal): Unit = {
    if (amountIn.intValue < 0)
      throw new Exception(id.toString + " : deposit money below 0")
    applyChange(DepositEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance.add(amountIn),
      getRevision + 1, amountDeposited, amountIn))
  }

  /** create new bank account */
  def createAccount(cmdID: UUID, committedTime:Date, cmdUserName:String, cmdCurrency:String): Unit = {
    if (cmdUserName.length == 0)
      throw new Exception(id.toString + " : no username")
    applyChange(RegisterAccountEvent(UUID.randomUUID(), cmdID, committedTime, id, cmdCurrency, balance,
      getRevision + 1, cmdUserName))
  }

  /** disable a bank account */
  def deleteAccount(cmdID: UUID, committedTime:Date): Unit = {
    applyChange(DeleteAccountEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance, getRevision + 1))
  }

  /** change the username of a bank account */
  def changeUserName(cmdID: UUID, committedTime:Date, newUserName:String): Unit = {
    if (newUserName.length == 0)
      throw new Exception(id.toString + " : no new username")
    applyChange(ChangeUserNameEvent(UUID.randomUUID(), cmdID, committedTime, id, currency, balance,
      getRevision + 1, newUserName))
  }

  /** clear the changes */
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

  /** Update the active status of an account */
  def setActivated(isActivated: Boolean): Unit = {
    activated = isActivated
  }

  def getActivated: Boolean = {
    activated
  }
}