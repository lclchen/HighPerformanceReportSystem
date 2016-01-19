/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.domain

import java.util.Date
import java.util.UUID
import java.math.BigDecimal

/** Message that use to communicate between actors. */
trait Message

/** Bank basic command trait or interface */
trait Command extends Message{
  val commandID: UUID
  val committedTime: Date
  val accountID: UUID
}

/** Transfer Command */
case class TransferCommand(commandID: UUID, committedTime: Date, accountID: UUID,
                           amountOut: BigDecimal, transferInAccountID: UUID, amountIn: BigDecimal) extends Command

/** Transfer money out Command */
case class TransferOutCommand(commandID: UUID, committedTime: Date, accountID: UUID,
                              amountOut: BigDecimal, transferInAccountID: UUID, amountIn: BigDecimal) extends Command

/** Get Transfer-in money Command */
case class TransferInCommand(commandID: UUID, committedTime: Date, accountID: UUID,
                             amountIn: BigDecimal, transferOutAccountID: UUID, amountOut: BigDecimal) extends Command

/** Withdraw money Command */
case class WithdrawCommand(commandID: UUID, committedTime: Date, accountID: UUID,
                           amountWithdrawn: BigDecimal, amountOut: BigDecimal) extends Command

/** Deposit money Command */
case class DepositCommand(commandID: UUID, committedTime: Date, accountID: UUID,
                          amountDeposited: BigDecimal, amountIn: BigDecimal) extends Command

/** Register new bank account Command */
case class RegisterAccountCommand(commandID: UUID, committedTime: Date, accountID: UUID,
                                  userName: String, currency: String) extends Command

/** Disable account Command */
case class DeleteAccountCommand(commandID: UUID, committedTime: Date, accountID: UUID) extends Command

/** Change the username of a bank account Command */
case class ChangeUserNameCommand(commandID: UUID, committedTime: Date, accountID: UUID
                                 , newUserName: String) extends Command
