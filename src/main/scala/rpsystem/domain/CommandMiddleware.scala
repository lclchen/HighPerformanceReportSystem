/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.domain

import rpsystem.recovery._

/** The trait or interface of CommandMiddleware */
trait ICommandMiddleware {
  /** Store command in persistency for error-recovery.
    * @param cmd Command
    */
  def storeCommand(cmd: Command)
}

/** The implementation of ICommandMiddleware.
  * @param cmdRecSrv ICommandRecoveryService for error-recovery.
  */
class CommandMiddleware(cmdRecSrv: ICommandRecoveryService) extends ICommandMiddleware{

  // store command and check whether the error-recovery module is available.
  override def storeCommand(cmd: Command) {
    if (cmdRecSrv.available) {
      cmdRecSrv.storeCommandByCM(cmd)
    }
  }
}