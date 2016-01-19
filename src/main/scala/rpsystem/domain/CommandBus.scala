/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.domain

import scala.collection.mutable.ListBuffer
import rpsystem.recovery._

/** The interface or trait of CommandBus */
trait ICommandBus {

  // separate a big command into several commands to make sure that each command is just related to one aggregateroot
  def transform(cmd: Command): ListBuffer[Command]
  // decide which CommandHandler should receive this command
  def transmit(cmd: Command): Int
  // set the shardings mode of CommandBus
  def setMode(newMode:CommandBus.SHARDINGS_MODE)
  
  // Error-Recovery
  def recordSentCommand(cmd: Command)
  def getRecoveryCommands():(ListBuffer[RecoveryCommand], ListBuffer[Command])
  def removeRecInfo()
}

/** The implementation of ICommandBus.
  * CommandBus is responsible for send commands to the correct CommandHandler.
  * @param cmdRecSrv ICommandRecoveryService for error-recovery.
  */
class CommandBus(cmdRecSrv: ICommandRecoveryService) extends ICommandBus{
  // default mode is no shardings.
  var mode:CommandBus.SHARDINGS_MODE = CommandBus.SHARDINGS_MODE_MOD_ACCOUTID(1)
  
  override def transform(cmd:Command): ListBuffer[Command] = {
    val buf = new ListBuffer[Command]
    cmd match{
      case c:TransferCommand =>
        buf += TransferOutCommand(c.commandID, c.committedTime, c.accountID,
          c.amountOut, c.transferInAccountID, c.amountIn)
        buf += TransferInCommand(c.commandID, c.committedTime, c.transferInAccountID,
          c.amountIn, c.accountID, c.amountOut)

      case c:TransferRecCommand =>
        buf += TransferOutRecCommand(c.commandID, c.committedTime, c.accountID,
          c.amountOut, c.transferInAccountID, c.amountIn)
        buf += TransferInRecCommand(c.commandID, c.committedTime,
          c.transferInAccountID, c.amountIn, c.accountID, c.amountOut)

      case c:Command =>
        buf += c

      case _ =>
        throw new Exception("Wrong type Command in CommandBus-transform()")
    }
  }
  
  override def transmit(cmd: Command): Int = {
    mode match {
      case mode:CommandBus.SHARDINGS_MODE_MOD_ACCOUTID =>
        // take the last 4 digits(32-36) of UUID to hash, UUID is 16-digit-number.
        return (Integer.valueOf(cmd.accountID.toString.substring(32, 36), 16)) % mode.numOfCmdHdl
      case _ =>
        return 0
    }
  }
  
  override def setMode(newMode:CommandBus.SHARDINGS_MODE){
    mode = newMode
  }

  /** Record the last sent command. */
  override def recordSentCommand(cmd: Command) {
    if(cmdRecSrv.available){
      cmdRecSrv.recordSentCmdByCB(cmd)
    }
  }

  /** Get the missing and unfinished commands in the last system shutdown. */
  override def getRecoveryCommands():(ListBuffer[RecoveryCommand], ListBuffer[Command])={
    cmdRecSrv.available match{
      case true => return cmdRecSrv.getRecCommandsByCB
      case false => return (new ListBuffer[RecoveryCommand], new ListBuffer[Command])
    }
  }

  /** Clear the record of error-recovery. */
  override def removeRecInfo(){
    if(cmdRecSrv.available){
      cmdRecSrv.removeAllCmdsByCB
    }
  }
}

object CommandBus {
  /** The interface of Shardings mode */
  trait SHARDINGS_MODE

  /** Shardings mode with different number of shardings. */
  case class SHARDINGS_MODE_MOD_ACCOUTID(numOfCmdHdl: Int) extends SHARDINGS_MODE
}