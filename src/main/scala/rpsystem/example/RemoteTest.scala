package rpsystem.example

import akka.actor._
import akka.actor.ActorRef


class CommandSender extends Actor{
  override def receive = {
    case mes:Any =>
      println(mes)
      val actorRef = context.actorFor("akka.tcp://SingleNodeReportSystem@0.0.0.0:2552/user/CommandMiddlewareActor")
      //val actorRef2 = context.actorSelection("akka.tcp://Test@127.0.0.1:2552/user/CommandSender")

      actorRef ! "Receive"

    case _ =>
  }
}

object RemoteTest {
  def main(args:Array[String]): Unit ={
    var system = ActorSystem("Test")
    val cmdSender = system.actorOf(Props(new CommandSender()), name="CommandSender")
    cmdSender ! "Test"
  }
}
