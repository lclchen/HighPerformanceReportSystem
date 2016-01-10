package rpsystem.domain

import rpsystem.recovery._
import scala.collection.mutable.ListBuffer

trait ICommandHandler {
  def receive: PartialFunction[Command, Unit]
}

class CommandHandler(val repository: Repository, evtRecSrv: IEventRecoveryService) extends ICommandHandler {
  private var evtStorage = ListBuffer[Event]()
  def getEvents = evtStorage
  def markEventsCommit(): Unit = { evtStorage.clear }
  
  var mode:CommandHandler.SNAPSHOT_MODE = new CommandHandler.SNAPSHOT_MODE_ALWAYS()
  
  def setMode(newMode:CommandHandler.SNAPSHOT_MODE){
    mode = newMode
  }

  def receive: PartialFunction[Command, Unit] = {
    case cmd: TransferOutCommand => handle(cmd)
    case cmd: TransferInCommand => handle(cmd)
    case cmd: WithdrawCommand => handle(cmd)
    case cmd: DepositCommand => handle(cmd)
    case cmd: RegisterAccountCommand => handle(cmd)
    case cmd: DeleteAccountCommand => handle(cmd)
    case cmd: ChangeUserNameCommand => handle(cmd)

    case cmd: TransferOutRecCommand => handle(cmd)
    case cmd: TransferInRecCommand => handle(cmd)
    case cmd: WithdrawRecCommand => handle(cmd)
    case cmd: DepositRecCommand => handle(cmd)
    case cmd: RegisterAccountRecCommand => handle(cmd)
    case cmd: DeleteAccountRecCommand => handle(cmd)
    case cmd: ChangeUserNameRecCommand => handle(cmd)
    case _ => logger.warn("wrong type command")
  }

  def handle(cmd: TransferOutCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl TransferOut:The accountOut isn't activated" + acct.id.toString);
          return
        }
        acct.transferMoneyOut(cmd.commandID, cmd.committedTime, cmd.amountOut, cmd.transferInAccountID, cmd.amountIn)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)
        mode match{
          case m:CommandHandler.SNAPSHOT_MODE_ALWAYS => repository.save(acct, -1)
          case m:CommandHandler.SNAPSHOT_MODE_EVENTSNUM =>
            m.sum += 1
            if(m.sum == m.num){
              m.sum = 0
              repository.save(acct, -1)
            }
            else{
              repository.save(acct, 0)
            }
          case m:CommandHandler.SNAPSHOT_MODE_MILLISECOND =>
            if(java.lang.System.currentTimeMillis - m.lastTime > m.duration){
              repository.save(acct, -1)
              m.lastTime = java.lang.System.currentTimeMillis
            }
            else{
              repository.save(acct, 0)
            }
        }

        if (evtRecSrv.available)
          evtStorage.foreach(evt => evtRecSrv.storeEventByCH(evt))
      }
      case _ => logger.warn("CmdHdl TransferOut:Can't find this account-Out in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }

  def handle(cmd: TransferInCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl TransferIn:The accountIn isn't activated" + acct.id.toString);
          return
        }
        acct.transferMoneyIn(cmd.commandID, cmd.committedTime, cmd.amountIn, cmd.transferOutAccountID, cmd.amountOut)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)
        mode match{
          case m:CommandHandler.SNAPSHOT_MODE_ALWAYS => repository.save(acct, -1)
          case m:CommandHandler.SNAPSHOT_MODE_EVENTSNUM =>
            m.sum += 1
            if(m.sum == m.num){
              m.sum = 0
              repository.save(acct, -1)
            }
            else{
              repository.save(acct, 0)
            }
          case m:CommandHandler.SNAPSHOT_MODE_MILLISECOND =>
            if(java.lang.System.currentTimeMillis - m.lastTime > m.duration){
              repository.save(acct, -1)
              m.lastTime = java.lang.System.currentTimeMillis
            }
            else{
              repository.save(acct, 0)
            }
        }

        if (evtRecSrv.available)
          evtStorage.foreach(evt => evtRecSrv.storeEventByCH(evt))
      }
      case _ => logger.warn("CmdHdl TransferIn:Can't find this account-In in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }

  def handle(cmd: WithdrawCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl Withdraw:The account isn't activated" + acct.id.toString)
          return
        }
        acct.withdrawMoney(cmd.commandID, cmd.committedTime, cmd.amountWithdrawn, cmd.amountOut)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)
        mode match{
          case m:CommandHandler.SNAPSHOT_MODE_ALWAYS => repository.save(acct, -1)
          case m:CommandHandler.SNAPSHOT_MODE_EVENTSNUM =>
            m.sum += 1
            if(m.sum == m.num){
              m.sum = 0
              repository.save(acct, -1)
            }
            else{
              repository.save(acct, 0)
            }
          case m:CommandHandler.SNAPSHOT_MODE_MILLISECOND =>
            if(java.lang.System.currentTimeMillis - m.lastTime > m.duration){
              repository.save(acct, -1)
              m.lastTime = java.lang.System.currentTimeMillis
            }
            else{
              repository.save(acct, 0)
            }
        }

        if (evtRecSrv.available)
          evtStorage.foreach(evt => evtRecSrv.storeEventByCH(evt))
      }
      case _ => logger.warn("CmdHdl Withdraw:Can't find this account in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }

  def handle(cmd: DepositCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl Deposit:The account isn't activated" + acct.id.toString)
          return
        }
        acct.depositMoney(cmd.commandID, cmd.committedTime, cmd.amountDeposited, cmd.amountIn)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)
        mode match{
          case m:CommandHandler.SNAPSHOT_MODE_ALWAYS => repository.save(acct, -1)
          case m:CommandHandler.SNAPSHOT_MODE_EVENTSNUM =>
            m.sum += 1
            if(m.sum == m.num){
              m.sum = 0
              repository.save(acct, -1)
            }
            else{
              repository.save(acct, 0)
            }
          case m:CommandHandler.SNAPSHOT_MODE_MILLISECOND =>
            if(java.lang.System.currentTimeMillis - m.lastTime > m.duration){
              repository.save(acct, -1)
              m.lastTime = java.lang.System.currentTimeMillis
            }
            else{
              repository.save(acct, 0)
            }
        }

        if (evtRecSrv.available)
          evtStorage.foreach(evt => evtRecSrv.storeEventByCH(evt))
      }
      case _ => logger.warn("CmdHdl Deposit:Can't find this account in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }

  def handle(cmd: RegisterAccountCommand): Unit = {
    val acct: AccountAggr = new AccountAggr(cmd.accountID, cmd.userName, cmd.currency)
    acct.createAccount(cmd.commandID, cmd.committedTime, cmd.userName, cmd.currency)
    acct.getUncommittedChanges.foreach(ev => evtStorage += ev)
    repository.add(acct)  //mode does not matter
    if (evtRecSrv.available)
      evtStorage.foreach(evt => evtRecSrv.storeEventByCH(evt))
  }

  def handle(cmd: DeleteAccountCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl DeleteAccount:The account isn't activated" + acct.id.toString)
          return
        }
        acct.deleteAccount(cmd.commandID, cmd.committedTime)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)
        mode match{
          case m:CommandHandler.SNAPSHOT_MODE_ALWAYS => repository.save(acct, -1)
          case m:CommandHandler.SNAPSHOT_MODE_EVENTSNUM =>
            m.sum += 1
            if(m.sum == m.num){
              m.sum = 0
              repository.save(acct, -1)
            }
            else{
              repository.save(acct, 0)
            }
          case m:CommandHandler.SNAPSHOT_MODE_MILLISECOND =>
            if(java.lang.System.currentTimeMillis - m.lastTime > m.duration){
              repository.save(acct, -1)
              m.lastTime = java.lang.System.currentTimeMillis
            }
            else{
              repository.save(acct, 0)
            }
        }
        
        if (evtRecSrv.available)
          evtStorage.foreach(evt => evtRecSrv.storeEventByCH(evt))
      }
      case _ => logger.warn("CmdHdl DeleteAccount:Can't find this account in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }

  def handle(cmd: ChangeUserNameCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl ChangeUserName:The account isn't activated" + acct.id.toString)
          return
        }
        acct.changeUserName(cmd.commandID, cmd.committedTime, cmd.newUserName)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)
        mode match{
          case m:CommandHandler.SNAPSHOT_MODE_ALWAYS => repository.save(acct, -1)
          case m:CommandHandler.SNAPSHOT_MODE_EVENTSNUM =>
            m.sum += 1
            if(m.sum == m.num){
              m.sum = 0
              repository.save(acct, -1)
            }
            else{
              repository.save(acct, 0)
            }
          case m:CommandHandler.SNAPSHOT_MODE_MILLISECOND =>
            if(java.lang.System.currentTimeMillis - m.lastTime > m.duration){
              repository.save(acct, -1)
              m.lastTime = java.lang.System.currentTimeMillis
            }
            else{
              repository.save(acct, 0)
            }
        }
        
        if (evtRecSrv.available)
          evtStorage.foreach(evt => evtRecSrv.storeEventByCH(evt))
      }
      case _ => logger.warn("CmdHdl ChangeUserName:Can't find this account in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }

  //To handle Recovery-Command
  
  def handle(cmd: TransferOutRecCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl TransferOut:The accountOut isn't activated" + acct.id.toString);
          return
        }
        acct.transferMoneyOut(cmd.commandID, cmd.committedTime, cmd.amountOut, cmd.transferInAccountID, cmd.amountIn)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)

        var isNewAccountCreated = false
        evtStorage.foreach(evt => {
          evt match {
            case e: RegisterAccountEvent => isNewAccountCreated = true
          }
          repository.evtStorage.isEventExist(evt.commandID, evt.accountID) match {
            case false => repository.evtStorage.saveEvent(evt)
            case true=>
          }
        })
        isNewAccountCreated match {
          //case false => repository.acctStorage.saveAccount(acct, -1)
          case true => 
            if(repository.acctStorage.isAccountExist(acct.id))
              repository.acctStorage.addAccount(acct)
          case false =>
        }
        
        if (evtRecSrv.available) {
          evtStorage.foreach(evt => {
            evtRecSrv.isEventExist(evt.commandID, evt.accountID) match {
              case false => evtRecSrv.storeEventByCH(evt)
              case true =>
            }
          })
        }
        
        markEventsCommit
      }
      case _ => logger.warn("CmdHdl TransferRecOut:Can't find this account-Out in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }

  def handle(cmd: TransferInRecCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl TransferIn:The accountIn isn't activated" + acct.id.toString);
          return
        }
        acct.transferMoneyIn(cmd.commandID, cmd.committedTime, cmd.amountIn, cmd.transferOutAccountID, cmd.amountOut)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)

        var isNewAccountCreated = false
        evtStorage.foreach(evt => {
          evt match {
            case e: RegisterAccountEvent => isNewAccountCreated = true
          }
          repository.evtStorage.isEventExist(evt.commandID, evt.accountID) match {
            case false => repository.evtStorage.saveEvent(evt)
            case true =>
          }
        })
        isNewAccountCreated match {
          //case false => repository.acctStorage.saveAccount(acct, -1)
          case true => 
            if(repository.acctStorage.isAccountExist(acct.id))
              repository.acctStorage.addAccount(acct)
          case false =>
        }

        if (evtRecSrv.available) {
          evtStorage.foreach(evt => {
            evtRecSrv.isEventExist(evt.commandID, evt.accountID) match {
              case false => evtRecSrv.storeEventByCH(evt)
              case true =>
            }
          })
        }
                
        markEventsCommit
      }
      case _ => logger.warn("CmdHdl TransferIn:Can't find this account-In in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }

  def handle(cmd: WithdrawRecCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl Withdraw:The account isn't activated" + acct.id.toString)
          return
        }
        acct.withdrawMoney(cmd.commandID, cmd.committedTime, cmd.amountWithdrawn, cmd.amountOut)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)

        var isNewAccountCreated = false
        evtStorage.foreach(evt => {
          evt match {
            case e: RegisterAccountEvent => isNewAccountCreated = true
          }
          repository.evtStorage.isEventExist(evt.commandID, evt.accountID) match {
            case false => repository.evtStorage.saveEvent(evt)
            case true =>
          }
        })
        isNewAccountCreated match {
          //case false => repository.acctStorage.saveAccount(acct, -1)
          case true => 
            if(repository.acctStorage.isAccountExist(acct.id))
              repository.acctStorage.addAccount(acct)
          case false =>
        }        

        if (evtRecSrv.available) {
          evtStorage.foreach(evt => {
            evtRecSrv.isEventExist(evt.commandID, evt.accountID) match {
              case false => evtRecSrv.storeEventByCH(evt)
              case true =>
            }
          })
        }
        
        markEventsCommit
      }
      case _ => logger.warn("CmdHdl Withdraw:Can't find this account in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }

  def handle(cmd: DepositRecCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl Deposit:The account isn't activated" + acct.id.toString)
          return
        }
        acct.depositMoney(cmd.commandID, cmd.committedTime, cmd.amountDeposited, cmd.amountIn)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)

        var isNewAccountCreated = false
        evtStorage.foreach(evt => {
          evt match {
            case e: RegisterAccountEvent => isNewAccountCreated = true
          }
          repository.evtStorage.isEventExist(evt.commandID, evt.accountID) match {
            case false => repository.evtStorage.saveEvent(evt)
            case true =>
          }
        })
        isNewAccountCreated match {
          //case false => repository.acctStorage.saveAccount(acct, -1)
          case true => 
            if(repository.acctStorage.isAccountExist(acct.id))
              repository.acctStorage.addAccount(acct)
          case false =>
        }        

        if (evtRecSrv.available) {
          evtStorage.foreach(evt => {
            evtRecSrv.isEventExist(evt.commandID, evt.accountID) match {
              case false => evtRecSrv.storeEventByCH(evt)
              case true =>
            }
          })
        }
        
        markEventsCommit
      }
      case _ => logger.warn("CmdHdl Deposit:Can't find this account in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }

  def handle(cmd: RegisterAccountRecCommand): Unit = {
    val acct: AccountAggr = new AccountAggr(cmd.accountID, cmd.userName, cmd.currency)
    acct.createAccount(cmd.commandID, cmd.committedTime, cmd.userName, cmd.currency)
    acct.getUncommittedChanges.foreach(ev => evtStorage += ev)

    var isNewAccountCreated = false
    evtStorage.foreach(evt => {
      evt match {
        case e: RegisterAccountEvent => isNewAccountCreated = true
      }
      repository.evtStorage.isEventExist(evt.commandID, evt.accountID) match {
        case false => repository.evtStorage.saveEvent(evt)
        case true =>
      }
    })
    isNewAccountCreated match {
      //case false => repository.acctStorage.saveAccount(acct, -1)
      case true => 
        if(repository.acctStorage.isAccountExist(acct.id))
              repository.acctStorage.addAccount(acct)
      case false =>
    }

    if (evtRecSrv.available) {
      evtStorage.foreach(evt => {
        evtRecSrv.isEventExist(evt.commandID, evt.accountID) match {
          case false => evtRecSrv.storeEventByCH(evt)
          case true =>
        }
      })
    }
    
    markEventsCommit
  }

  def handle(cmd: DeleteAccountRecCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl DeleteAccount:The account isn't activated" + acct.id.toString)
          return
        }
        acct.deleteAccount(cmd.commandID, cmd.committedTime)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)

        var isNewAccountCreated = false
        evtStorage.foreach(evt => {
          evt match {
            case e: RegisterAccountEvent => isNewAccountCreated = true
          }
          repository.evtStorage.isEventExist(evt.commandID, evt.accountID) match {
            case false => repository.evtStorage.saveEvent(evt)
            case true =>
          }
        })
        isNewAccountCreated match {
          //case false => repository.acctStorage.saveAccount(acct, -1)
          case true => 
            if(repository.acctStorage.isAccountExist(acct.id))
              repository.acctStorage.addAccount(acct)
          case false =>
        }        

        if (evtRecSrv.available) {
          evtStorage.foreach(evt => {
            evtRecSrv.isEventExist(evt.commandID, evt.accountID) match {
              case false => evtRecSrv.storeEventByCH(evt)
              case true =>
            }
          })
        }
        
        markEventsCommit
      }
      case _ => logger.warn("CmdHdl DeleteAccount:Can't find this account in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }

  def handle(cmd: ChangeUserNameRecCommand): Unit = {
    repository.getById(cmd.accountID) match {
      case Some(acct) => {
        if (!acct.getActivated) {
          logger.warn("CmdHdl ChangeUserName:The account isn't activated" + acct.id.toString)
          return
        }
        acct.changeUserName(cmd.commandID, cmd.committedTime, cmd.newUserName)
        acct.getUncommittedChanges.foreach(ev => evtStorage += ev)

        var isNewAccountCreated = false
        evtStorage.foreach(evt => {
          evt match {
            case e: RegisterAccountEvent => isNewAccountCreated = true
          }
          repository.evtStorage.isEventExist(evt.commandID, evt.accountID) match {
            case false => repository.evtStorage.saveEvent(evt)
            case true =>
          }
        })
        isNewAccountCreated match {
          //case false => repository.acctStorage.saveAccount(acct, -1)
          case true => 
            if(repository.acctStorage.isAccountExist(acct.id))
              repository.acctStorage.addAccount(acct)
          case false =>
        }        

        if (evtRecSrv.available) {
          evtStorage.foreach(evt => {
            evtRecSrv.isEventExist(evt.commandID, evt.accountID) match {
              case false => evtRecSrv.storeEventByCH(evt)
              case true =>
            }
          })
        }
        
        markEventsCommit
      }
      case _ => logger.warn("CmdHdl ChangeUserName:Can't find this account in repository:" + cmd.accountID + ":" + cmd.toString)
    }
  }
}

object CommandHandler{
  trait SNAPSHOT_MODE
  
  //must new-different-mode for different CommandHandlers
  case class SNAPSHOT_MODE_ALWAYS() extends SNAPSHOT_MODE
  case class SNAPSHOT_MODE_EVENTSNUM(num:Int, var sum:Int) extends SNAPSHOT_MODE //num-maximum number, sum-number now
  case class SNAPSHOT_MODE_MILLISECOND(duration:Long, var lastTime:Long) extends SNAPSHOT_MODE  
}