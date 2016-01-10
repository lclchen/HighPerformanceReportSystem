package rpsystem.recovery

import rpsystem.domain._
import java.util.UUID
import java.util.Date
import java.math.BigDecimal

abstract class RecoveryCommand extends Command

case class TransferRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountOut: BigDecimal, transferInAccountID: UUID, amountIn: BigDecimal) extends RecoveryCommand
case class TransferOutRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountOut: BigDecimal, transferInAccountID: UUID, amountIn: BigDecimal) extends RecoveryCommand
case class TransferInRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountIn: BigDecimal, transferOutAccountID: UUID, amountOut: BigDecimal) extends RecoveryCommand
case class WithdrawRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountWithdrawn: BigDecimal, amountOut: BigDecimal) extends RecoveryCommand
case class DepositRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountDeposited: BigDecimal, amountIn: BigDecimal) extends RecoveryCommand

case class RegisterAccountRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, userName: String, currency: String) extends RecoveryCommand
case class DeleteAccountRecCommand(commandID: UUID, committedTime: Date, accountID: UUID) extends RecoveryCommand
case class ChangeUserNameRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, newUserName: String) extends RecoveryCommand