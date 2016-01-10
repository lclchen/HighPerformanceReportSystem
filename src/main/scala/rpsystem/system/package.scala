package rpsystem

package object system {
  implicit val akkaSystem = akka.actor.ActorSystem()
  val logger = rpsystem.logger
}