package rpsystem.domain

import java.util.Date
import java.util.UUID
import java.math.BigDecimal

trait Message

trait Command extends Message{
  val commandID: UUID
  val committedTime: Date
  val accountID: UUID
}

case class TransferCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountOut: BigDecimal, transferInAccountID: UUID, amountIn: BigDecimal) extends Command
case class TransferOutCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountOut: BigDecimal, transferInAccountID: UUID, amountIn: BigDecimal) extends Command
case class TransferInCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountIn: BigDecimal, transferOutAccountID: UUID, amountOut: BigDecimal) extends Command
case class WithdrawCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountWithdrawn: BigDecimal, amountOut: BigDecimal) extends Command
case class DepositCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountDeposited: BigDecimal, amountIn: BigDecimal) extends Command

case class RegisterAccountCommand(commandID: UUID, committedTime: Date, accountID: UUID, userName: String, currency: String) extends Command
case class DeleteAccountCommand(commandID: UUID, committedTime: Date, accountID: UUID) extends Command
case class ChangeUserNameCommand(commandID: UUID, committedTime: Date, accountID: UUID, newUserName: String) extends Command
