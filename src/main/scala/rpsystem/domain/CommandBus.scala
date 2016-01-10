package rpsystem.domain

import rpsystem.recovery._
import scala.collection.mutable.ListBuffer

trait ICommandBus {
  def transform(cmd: Command): ListBuffer[Command]//transform tranfer-command into two commands
  def transmit(cmd: Command): Int //decide which cmdHandler should receive this command
  def setMode(newMode:CommandBus.SHARDINGS_MODE)
  
  // Error-Recovery
  def recordSentCommand(cmd: Command)
  def getRecoveryCommands():(ListBuffer[RecoveryCommand], ListBuffer[Command])
  def removeRecInfo()
}

class CommandBus(cmdRecSrv: ICommandRecoveryService) extends ICommandBus{
  var mode:CommandBus.SHARDINGS_MODE = CommandBus.SHARDINGS_MODE_MOD_ACCOUTID(1)
  
  override def transform(cmd:Command): ListBuffer[Command] = {
    val buf = new ListBuffer[Command]
    cmd match{
      case c:TransferCommand =>
        buf += TransferOutCommand(c.commandID, c.committedTime, c.accountID, c.amountOut, c.transferInAccountID, c.amountIn)
        buf += TransferInCommand(c.commandID, c.committedTime, c.transferInAccountID, c.amountIn, c.accountID, c.amountOut)
      case c:TransferRecCommand =>
        buf += TransferOutRecCommand(c.commandID, c.committedTime, c.accountID, c.amountOut, c.transferInAccountID, c.amountIn)
        buf += TransferInRecCommand(c.commandID, c.committedTime, c.transferInAccountID, c.amountIn, c.accountID, c.amountOut)
      case c:Command => buf += c
      case _ => throw new Exception("Wrong type Command in CommandBus-transform()")
    }
  }
  
  override def transmit(cmd: Command): Int = {
    mode match {
      case mode:CommandBus.SHARDINGS_MODE_MOD_ACCOUTID => 
        return (Integer.valueOf(cmd.accountID.toString.substring(32, 36), 16)) % mode.numOfCmdHdl
        //take the last 4 digits(32-36) of UUID, UUID is 16-digit-number
      case _ => return 0
    }
  }
  
  override def setMode(newMode:CommandBus.SHARDINGS_MODE){
    mode = newMode
  }
  
  override def recordSentCommand(cmd: Command) {
    if(cmdRecSrv.available){
      cmdRecSrv.recordSentCmdByCB(cmd)
    }
  }
  
  override def getRecoveryCommands():(ListBuffer[RecoveryCommand], ListBuffer[Command])={
    cmdRecSrv.available match{
      case true => return cmdRecSrv.getRecCommandsByCB
      case false => return (new ListBuffer[RecoveryCommand], new ListBuffer[Command])
    }
  }
  
  override def removeRecInfo(){
    if(cmdRecSrv.available){
      cmdRecSrv.removeAllCmdsByCB
    }
  }
}

object CommandBus {
  trait SHARDINGS_MODE
  
  case class SHARDINGS_MODE_MOD_ACCOUTID(numOfCmdHdl: Int) extends SHARDINGS_MODE
}