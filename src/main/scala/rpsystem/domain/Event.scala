package rpsystem.domain

import java.util.Date
import java.util.UUID
import java.math.BigDecimal

trait Event extends Message{
  val eventID: UUID
  val commandID: UUID
  val committedTime: Date
  val accountID: UUID
  val currency: String
  val balance: BigDecimal
  val revision: Int
}

//event-account aggregate
case class TransferOutEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, amountOut:BigDecimal, transferInAccountID:UUID, amountIn:BigDecimal) extends Event
case class TransferInEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, amountIn:BigDecimal, transferOutAccountID:UUID, amountOut:BigDecimal) extends Event
case class WithdrawEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, amountWithdrawn:BigDecimal, amountOut:BigDecimal) extends Event
case class DepositEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, amountDeposited:BigDecimal, amountIn:BigDecimal) extends Event

case class RegisterAccountEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, userName:String) extends Event
case class DeleteAccountEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int) extends Event
case class ChangeUserNameEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, newUserName:String) extends Event
