/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.recovery

import rpsystem.domain._
import java.util.UUID
import java.util.Date
import java.math.BigDecimal

/** Recovery Command for error-recovery */
abstract class RecoveryCommand extends Command

/** Transfer recovery-command */
case class TransferRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountOut: BigDecimal,
                              transferInAccountID: UUID, amountIn: BigDecimal) extends RecoveryCommand

/** Transfer money out recovery-command */
case class TransferOutRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountOut: BigDecimal,
                                 transferInAccountID: UUID, amountIn: BigDecimal) extends RecoveryCommand

/** Get Transfer-in money recovery-command */
case class TransferInRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountIn: BigDecimal,
                                transferOutAccountID: UUID, amountOut: BigDecimal) extends RecoveryCommand

/** Withdraw money recovery-command */
case class WithdrawRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountWithdrawn: BigDecimal,
                              amountOut: BigDecimal) extends RecoveryCommand

/** Deposit transaction recovery-command */
case class DepositRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, amountDeposited: BigDecimal,
                             amountIn: BigDecimal) extends RecoveryCommand

/** Register new account recovery-command */
case class RegisterAccountRecCommand(commandID: UUID, committedTime: Date, accountID: UUID, userName: String,
                                     currency: String) extends RecoveryCommand

/** Disable a bank account recovery-command */
case class DeleteAccountRecCommand(commandID: UUID, committedTime: Date, accountID: UUID) extends RecoveryCommand

/** RecoveryCommand to change the username of a bank account */
case class ChangeUserNameRecCommand(commandID: UUID, committedTime: Date, accountID: UUID,
                                    newUserName: String) extends RecoveryCommand