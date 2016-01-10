package rpsystem.example

import akka.actor._
import akka.actor.ActorRef


class CommandSender extends Actor{
  override def receive = {
    case mes:Any =>
      println(mes)
      val actorRef = context.actorFor("akka.tcp://SingleNodeReportSystem@0.0.0.0:2552/user/CommandMiddlewareActor")
      val actorRef2 = context.actorSelection("akka.tcp://Test@127.0.0.1:2552/user/CommandSender")
      val actorRef3 = context.actorSelection("akka://Test/user/CommandSender")

      //println(actorRef2.isTerminated)
      //println(actorRef3.isTerminated)
      //println(sender.path.toString)
      //sender() ! "Get"
      actorRef ! "Send"
      actorRef2 ! "reSend2"
      actorRef3 ! "reSend3"
    case _ =>
  }
}

object RemoteTest {
  def main(args:Array[String]): Unit ={
    var system = ActorSystem("Test")
    //print(system.isTerminated)
    //val actorRef = system.actorFor("akka.tcp://SingleNodeReportSystem@0.0.0.0:2552/user/CommandMiddlewareActor")
    //val selection = context.actorSelection("akka.tcp://actorSystemName@10.0.0.1:2552/user/actorName")
    //print(actorRef.isTerminated)
    val cmdsender = system.actorOf(Props(new CommandSender()), name="CommandSender")
    cmdsender ! "Hello"
  }
}
