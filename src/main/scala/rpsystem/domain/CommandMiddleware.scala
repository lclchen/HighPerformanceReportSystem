package rpsystem.domain

import rpsystem.recovery._

trait ICommandMiddleware {
  def storeCommand(cmd: Command)
}

class CommandMiddleware(cmdRecSrv: ICommandRecoveryService) extends ICommandMiddleware{
  override def storeCommand(cmd: Command) {
    if (cmdRecSrv.available) {
      cmdRecSrv.storeCommandByCM(cmd)
    }
  }
}