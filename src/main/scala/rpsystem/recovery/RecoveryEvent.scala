/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.recovery

import rpsystem.domain._
import java.util.UUID
import java.util.Date
import java.math.BigDecimal

/** RecoveryEvent for error-recovery. */
abstract class RecoveryEvent extends Event

/** RecoveryEvent for transferring money out */
case class TransferOutRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                               balance:BigDecimal, revision:Int, amountOut:BigDecimal, transferInAccountID:UUID,
                               amountIn:BigDecimal) extends RecoveryEvent

/** RecoveryEvent for getting Transfer-in money */
case class TransferInRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                              balance:BigDecimal, revision:Int, amountIn:BigDecimal, transferOutAccountID:UUID,
                              amountOut:BigDecimal) extends RecoveryEvent

/** RecoveryEvent for withdrawing money */
case class WithdrawRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                            balance:BigDecimal, revision:Int, amountWithdrawn:BigDecimal,
                            amountOut:BigDecimal) extends RecoveryEvent

/** RecoveryEvent for depositing money */
case class DepositRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                           balance:BigDecimal, revision:Int, amountDeposited:BigDecimal,
                           amountIn:BigDecimal) extends RecoveryEvent

/** RecoveryEvent for registerring a new bank account */
case class RegisterAccountRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                                   balance:BigDecimal, revision:Int, userName:String) extends RecoveryEvent

/** RecoveryEvent for disabling a bank account. */
case class DeleteAccountRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                                 balance:BigDecimal, revision:Int) extends RecoveryEvent

/** RecoveryEvent for changing the username of a bank account. */
case class ChangeUserNameRecEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                                  balance:BigDecimal, revision:Int, newUserName:String) extends RecoveryEvent