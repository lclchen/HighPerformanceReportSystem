package rpsystem.recovery

import rpsystem.domain._
import java.util.UUID
import java.util.Date
import java.util.TreeMap
import java.util.Comparator
import scala.util.control.Breaks._
import scala.collection.mutable.ListBuffer
import com.sun.beans.decoder.TrueElementHandler

trait ICommandRecoveryService{
  var available: Boolean = true
  
  def storeCommandByCM(cmd: Command)
  def recordSentCmdByCB(cmd: Command)
  def getRecCommandsByCB(): (ListBuffer[RecoveryCommand], ListBuffer[Command])
  def removeAllCmdsByCB()
  def removeProcessedCmdByEB(cmdID: UUID, acctID: UUID)
  def isCmdExistByEB(cmdID: UUID, acctID: UUID):Boolean
}

class CommandRecoveryService(stateRec: ProcessingCmdRecStorage, messageRec: MessageCmdRecStorage) extends ICommandRecoveryService {
  override def storeCommandByCM(cmd: Command) {
    messageRec.storeCommand(cmd)
  }

  override def recordSentCmdByCB(cmd: Command) {
    cmd match{
      case c:RecoveryCommand =>
      case c:Command => 
        stateRec.storeProcessingCommand(c, new Date())
        stateRec.updateLastSentCommand(cmd)
    }    
  }

  override def removeProcessedCmdByEB(cmdID: UUID, acctID: UUID) {
    stateRec.removeProcessedCommand(cmdID, acctID)
  }
  
  override def isCmdExistByEB(cmdID: UUID, acctID: UUID):Boolean = {
    stateRec.getProcessingCommand.get(cmdID.toString + acctID.toString) match{
      case Some(v) => return true
      case None => return false
    }
  }

  override def getRecCommandsByCB(): (ListBuffer[RecoveryCommand], ListBuffer[Command]) = {
    val recCmds = new ListBuffer[RecoveryCommand]()
    val cmds = new ListBuffer[Command]()

    val lastCmd = stateRec.getLastSendCmdIDAcctID
    if (lastCmd.equals(""))
      return (recCmds, cmds)

    val processedCmds = stateRec.getProcessingCommand //[cmdID+acctID.toString, Date.toString]
    val allCmds = messageRec.getAllCommands

    val treeMap: TreeMap[Date, String] = new TreeMap[Date, String](new Comparator[Date] {
      override def compare(date1: Date, date2: Date): Int = {
        return date1.compareTo(date2)
      }
    })
    processedCmds.foreach(pair => treeMap.put(new Date(pair._2), pair._1)) //update treeMap

    var reachMissingCmds: Boolean = false
    var findLastSentCmd: Boolean = false
    var iter = treeMap.keySet().iterator()
    var nextRedoCmdDate: Date = new Date()
    if (iter.hasNext())
      nextRedoCmdDate = iter.next()

    breakable {//breakable is not necessary
      allCmds.foreach(cmd => {
        if (reachMissingCmds) {
          cmds += cmd
        } else {
          if (findLastSentCmd) {
            recCmds += getRecCommand(cmd)
            findLastSentCmd = false;
            reachMissingCmds = true;
          } 
          else {
            var transferDoneTimes = 0;
            // if this command is in all-processing-cmds
            while ((treeMap.get(nextRedoCmdDate) != null) && (cmd.commandID.toString.equals(treeMap.get(nextRedoCmdDate).substring(0, 36)))) {
              //only recover small event in Redis
              recCmds += getRecCommand(cmd, UUID.fromString(treeMap.get(nextRedoCmdDate).substring(36)))
              transferDoneTimes += 1
              if (iter.hasNext()) {
                nextRedoCmdDate = iter.next()
              } else {
                nextRedoCmdDate = new Date()
              }
            }

            // if this command is last-sent-cmd
            if (cmd.commandID.toString.equals(lastCmd.substring(0, 36))) {
              cmd match {
                case c: TransferCommand => {
                  if (c.accountID.toString.equals(lastCmd.substring(36)) && (transferDoneTimes != 2)) {
                    recCmds += getRecCommand(c, c.transferInAccountID)
                    findLastSentCmd = false;
                    reachMissingCmds = true;
                  } else {
                    findLastSentCmd = true;
                  }
                }
                case _ => {
                  findLastSentCmd = true;
                }
              }
            }
            
          }
        }
      })
    }
    return (recCmds, cmds)
  }
  
  override def removeAllCmdsByCB(){
    stateRec.removeLastSendCommand
    stateRec.removeAllCommands
  }

  private def getRecCommand(cmd: Command): RecoveryCommand = {
    cmd match {
      case c: TransferCommand => return TransferRecCommand(c.commandID, c.committedTime, c.accountID, c.amountOut, c.transferInAccountID, c.amountIn)
      case c: TransferOutCommand => return TransferOutRecCommand(c.commandID, c.committedTime, c.accountID, c.amountOut, c.transferInAccountID, c.amountIn)
      case c: TransferInCommand => return TransferInRecCommand(c.commandID, c.committedTime, c.accountID, c.amountIn, c.transferOutAccountID, c.amountOut)
      case c: WithdrawCommand => return WithdrawRecCommand(c.commandID, c.committedTime, c.accountID, c.amountWithdrawn, c.amountOut)
      case c: DepositCommand => return DepositRecCommand(c.commandID, c.committedTime, c.accountID, c.amountDeposited, c.amountIn)
      case c: RegisterAccountCommand => return RegisterAccountRecCommand(c.commandID, c.committedTime, c.accountID, c.userName, c.currency)
      case c: DeleteAccountCommand => return DeleteAccountRecCommand(c.commandID, c.committedTime, c.accountID)
      case c: ChangeUserNameCommand => return ChangeUserNameRecCommand(c.commandID, c.committedTime, c.accountID, c.newUserName)
    }
  }

  private def getRecCommand(cmd: Command, accountID: UUID): RecoveryCommand = {
    cmd match {
      case c: TransferCommand => {
        if (accountID.equals(c.accountID))
          return TransferOutRecCommand(c.commandID, c.committedTime, c.accountID, c.amountOut, c.transferInAccountID, c.amountIn)
        else {
          return TransferInRecCommand(c.commandID, c.committedTime, c.transferInAccountID, c.amountIn, c.accountID, c.amountOut)
        }
      }
      case c: TransferOutCommand => return TransferOutRecCommand(c.commandID, c.committedTime, c.accountID, c.amountOut, c.transferInAccountID, c.amountIn)
      case c: TransferInCommand => return TransferInRecCommand(c.commandID, c.committedTime, c.accountID, c.amountIn, c.transferOutAccountID, c.amountOut)
      case c: WithdrawCommand => return WithdrawRecCommand(c.commandID, c.committedTime, c.accountID, c.amountWithdrawn, c.amountOut)
      case c: DepositCommand => return DepositRecCommand(c.commandID, c.committedTime, c.accountID, c.amountDeposited, c.amountIn)
      case c: RegisterAccountCommand => return RegisterAccountRecCommand(c.commandID, c.committedTime, c.accountID, c.userName, c.currency)
      case c: DeleteAccountCommand => return DeleteAccountRecCommand(c.commandID, c.committedTime, c.accountID)
      case c: ChangeUserNameCommand => return ChangeUserNameRecCommand(c.commandID, c.committedTime, c.accountID, c.newUserName)
    }
  }
}