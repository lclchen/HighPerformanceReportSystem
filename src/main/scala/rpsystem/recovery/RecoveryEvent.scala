package rpsystem.recovery

import rpsystem.domain._
import java.util.UUID
import java.util.Date
import java.math.BigDecimal

//Event for Error Recovery
abstract class RecoveryEvent extends Event

case class TransferOutRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, amountOut:BigDecimal, transferInAccountID:UUID, amountIn:BigDecimal) extends RecoveryEvent
case class TransferInRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, amountIn:BigDecimal, transferOutAccountID:UUID, amountOut:BigDecimal) extends RecoveryEvent
case class WithdrawRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, amountWithdrawn:BigDecimal, amountOut:BigDecimal) extends RecoveryEvent
case class DepositRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, amountDeposited:BigDecimal, amountIn:BigDecimal) extends RecoveryEvent

case class RegisterAccountRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, userName:String) extends RecoveryEvent
case class DeleteAccountRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int) extends RecoveryEvent
case class ChangeUserNameRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String, balance:BigDecimal, revision:Int, newUserName:String) extends RecoveryEvent