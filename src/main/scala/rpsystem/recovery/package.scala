package rpsystem

package object recovery {
  val logger = rpsystem.logger
  implicit val akkaSystem = akka.actor.ActorSystem()
}