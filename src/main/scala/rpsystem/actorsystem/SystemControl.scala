package rpsystem.actorsystem

import akka.actor._
import java.util.UUID

trait SystemControl

case class IsCommandBusReady() extends SystemControl
case class CommandBusIsReady() extends SystemControl
case class IsCommandHandlerReady() extends SystemControl
case class CommandHandlerIsReady() extends SystemControl
case class IsEventBusReady() extends SystemControl
case class EventBusIsReady() extends SystemControl

case class AddEventHandler(id: UUID, priority: Int, name: String) extends SystemControl
case class RemoveEventHandler(id: UUID) extends SystemControl
case class ResetEHPriority(id: UUID, priority: Int) extends SystemControl
case class ResetEHName(id: UUID, name: String) extends SystemControl
case class ResetEHPath(id: UUID) extends SystemControl
