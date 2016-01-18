/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.actorsystem

import java.util.UUID

/** SystemConrol of actor-sytem */
trait SystemControl

// Check whether the related actor is ready or not.
case class IsCommandBusReady() extends SystemControl
case class CommandBusIsReady() extends SystemControl
case class IsCommandHandlerReady() extends SystemControl
case class CommandHandlerIsReady() extends SystemControl
case class IsEventBusReady() extends SystemControl
case class EventBusIsReady() extends SystemControl

// Control related to EventHandler-Actor
case class AddEventHandler(id: UUID, priority: Int, name: String) extends SystemControl
case class RemoveEventHandler(id: UUID) extends SystemControl
case class ResetEHPriority(id: UUID, priority: Int) extends SystemControl
case class ResetEHName(id: UUID, name: String) extends SystemControl
case class ResetEHPath(id: UUID) extends SystemControl
