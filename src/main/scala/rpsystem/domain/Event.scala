/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.domain

import java.util.Date
import java.util.UUID
import java.math.BigDecimal

/** The trait of bank event */
trait Event extends Message{
  val eventID: UUID
  val commandID: UUID
  val committedTime: Date
  val accountID: UUID
  val currency: String
  val balance: BigDecimal
  val revision: Int
}

/** Event of transferring money out */
case class TransferOutEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                            balance:BigDecimal, revision:Int, amountOut:BigDecimal, transferInAccountID:UUID,
                            amountIn:BigDecimal) extends Event

/** Event of getting transferred money */
case class TransferInEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                           balance:BigDecimal, revision:Int, amountIn:BigDecimal, transferOutAccountID:UUID,
                           amountOut:BigDecimal) extends Event

/** Event of withdrawal */
case class WithdrawEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                         balance:BigDecimal, revision:Int, amountWithdrawn:BigDecimal,
                         amountOut:BigDecimal) extends Event

/** Event of deposit */
case class DepositEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                        balance:BigDecimal, revision:Int, amountDeposited:BigDecimal,
                        amountIn:BigDecimal) extends Event

/** Event of registering new bank account */
case class RegisterAccountEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                                balance:BigDecimal, revision:Int, userName:String) extends Event

/** Event of disabling a bank account */
case class DeleteAccountEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                              balance:BigDecimal, revision:Int) extends Event

/** Event of changing the username of a bank account */
case class ChangeUserNameEvent(eventID:UUID, commandID:UUID, committedTime:Date, accountID:UUID, currency:String,
                               balance:BigDecimal, revision:Int, newUserName:String) extends Event
