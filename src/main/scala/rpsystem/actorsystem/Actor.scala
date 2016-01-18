/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.actorsystem

import java.util.UUID

import akka.actor._
import akka.pattern.ask
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.concurrent.Await
import scala.io.Source
import scala.util.control.Breaks._

import rpsystem.domain._
import rpsystem.recovery._

/** CommandMiddleware-Actor is an implementation of Actor.
  * It is a middleware between the online-system and the report-system.
  * Its responsiblility is to make sure that all commands are in persistency into database, in case that commands
  * in the mailbox lose due to system breaks down or other disasters.
  *
  * @param cmdMidware CommandMiddleware.
  * @param cmdBusActor ActorRef of CommandBus-Actor.
  */
class CommandMiddlewareActor(cmdMidware:CommandMiddleware, cmdBusActor:ActorRef) extends Actor{ 

  override def receive = {
    case cmd:Command =>
      // ensure the command is in persistency if the recovery-module is available.
      cmdMidware.storeCommand(cmd)
      // check whether the CommandBus-Actor is online.
      while(cmdBusActor.isTerminated){
        Thread.sleep(10000)
      }
      cmdBusActor ! cmd
      
    case CommandBusIsReady =>

    case _ =>
      logger.error("CommandMiddleware receive wrong type of message")
  }
  
  override def preStart = {
    try{
	    Await.result(cmdBusActor.ask(IsCommandBusReady)(60 seconds), Duration.Inf)
    }
    catch{
      case e: akka.pattern.AskTimeoutException => 
        logger.error("CommandBus not response, re-initialize CommandMiddleware")
        preStart
      case _ => 
        logger.error("Unknown Exception, re-initialize CommandMiddleware")
        preStart
    }
    logger.info("CommandMiddleware starts successfully!")
  }
}

/** CommandBus-Actor is an implementation of Actor.
  * It is responsible to receive commands from the CommandMiddleware-Actor and forward the command to the right
  * CommandHandler-Actor.
  * It will record the last sent command for error-recovery.
  *
  * @param cmdBus CommandBus
  * @param cmdHdlActors Seq of ActorRef, represent all CommandHandler-Actors
  * @param evtBusActor EventBus-Actor
  */
class CommandBusActor(cmdBus:CommandBus, cmdHdlActors: Seq[ActorRef], evtBusActor:ActorRef) extends Actor{

  override def receive = {
    case cmd: Command => 
      cmdBus.transform(cmd).foreach(c => {
        // find the correct CommandHandler to receive this message
        val cmdHdlActor = cmdHdlActors(cmdBus.transmit(c))
        while(cmdHdlActor.isTerminated){
          Thread.sleep(6000)
        }
        // record the last sent command for error-recovery
        cmdBus.recordSentCommand(c)
        cmdHdlActor ! c
      })
    
    case IsCommandBusReady => sender ! CommandBusIsReady

    case CommandHandlerIsReady =>

    case EventBusIsReady =>

    case _ => logger.error("Wrong Type of Command")
  }
  
  override def preStart = {
    // check whether EventBus-Actor is online and ready
    try{
	    Await.result(evtBusActor.ask(IsEventBusReady)(60 seconds), Duration.Inf)
    }catch{
      case e: akka.pattern.AskTimeoutException => 
        logger.error("EventBus not response, re-initialize CommandBus")
        preStart
      case _ => 
        logger.error("Unknown Exception, re-initialize CommandBus")
        preStart
    }

    // check whether all CommandHandler-Actors are ready
    try{
      cmdHdlActors.foreach(actor => Await.result(actor.ask(IsCommandHandlerReady)(60 seconds), Duration.Inf))
    }catch{
      case e: akka.pattern.AskTimeoutException => 
        logger.error("CommandHandler not response, re-initialize CommandBus")
        preStart
      case _ => 
        logger.error("Unknown Exception, re-initialize CommandBus")
        preStart
    }

    // resend missing or unfinished commands in the last-time system shutdown.
    val (recCmds:ListBuffer[RecoveryCommand], missingCmds:ListBuffer[Command]) = cmdBus.getRecoveryCommands
    cmdBus.removeRecInfo
    recCmds.foreach(cmd => self ! cmd)
    missingCmds.foreach(cmd => self ! cmd)
    logger.info("CommandBus starts successfully!")
  }
}

/** An implementation of CommandHandler-Actor.
  * It is responsible for handling the command sent from the CommandBus, and do operations in the domain.
  * It will store events and send them to the EventBus.
  *
  * @param cmdHdl CommandHandler
  * @param evtBusActor EventBus-Actor
  */
class CommandHandlerActor(cmdHdl: CommandHandler, evtBusActor: ActorRef) extends Actor {
  override def receive = {
    case command: Command => {
      // this commandHandler will handle the command
      cmdHdl.receive(command)
      // get events and send them to the EventBus-Actor
      val events = cmdHdl.getEvents
      if (events.length != 0){
        while(evtBusActor.isTerminated)
          Thread.sleep(6000)
        evtBusActor ! cmdHdl.getEvents.clone
      }
      cmdHdl.markEventsCommit
    }

    case IsCommandHandlerReady => sender ! CommandBusIsReady()

    case EventBusIsReady =>

    case _ => logger.error("Account Actor receive an error message.")
  }
  
  override def preStart = {
    // check whether the EventBus-Actor is ready.
    try{
	    Await.result(evtBusActor.ask(IsEventBusReady)(60 seconds), Duration.Inf)
    }
    catch{
      case e: akka.pattern.AskTimeoutException => 
        logger.error("EventBus not response, re-initialize EventHandler")
        preStart
      case _ => 
        logger.error("Unknown Exception, re-initialize EventHandler")
        preStart
    }
    logger.info("CommandHandler starts successfully!")
  }
}

/** An implementation of Akka-Actor
  * It receives events from several CommandHandler-Actors and forwards them to related EventHandler-Actors.
  * Each EventHandler can register for different kinds of events. You can take a look at rpsystem.domain.EventSubscribe.
  *
  * @param evtBus
  */
class EventBusActor(evtBus:EventBus) extends Actor {
  // store name, actorRef, priority for each EventHandler-Actor.
  val nameMap:HashMap[UUID, String] = new HashMap[UUID, String]
  val actorRefMap:HashMap[UUID, ActorRef] = new HashMap[UUID, ActorRef]
  val priorityMap:HashMap[UUID, Int] = new HashMap[UUID, Int]

  // store the subscribe topic and un-sent events for each EventHandler-Actor
  val contentMap:HashMap[UUID, ListBuffer[EventSubscribe]] = new HashMap[UUID, ListBuffer[EventSubscribe]]
  val packageMap:HashMap[UUID, ListBuffer[Event]] = new HashMap[UUID, ListBuffer[Event]]
  
  override def receive = {
    // receive normal events from CommandHandler-Actors
    case events: Traversable[Event] =>
      events.foreach(evt => {
        logger.debug("EB receive one event: " + evt.toString)
        // record last received event for error-recovery, and would delete "redis-cmd-all".
        evtBus.recordLastReceivedEvt(evt)
        actorRefMap.foreach(pair => {
          // detect whether this event is subscribed by this EventHandler-Actor
          if (isMatch(evt, pair._1)){
            evtBus.recordAllSentEvt(evt, pair._1)
            // store the message into package and check whether the package is full and need to send.
            packageMap.get(pair._1) match{
              case Some(list)=>
                val sentEvts = evtBus.isPackageFull(evt, list)
                if(sentEvts != null){
                  if(priorityMap.getOrElse(pair._1, -1) >= EventHandler.PRIORITY_HIGH)
                	while(pair._2.isTerminated)
               	      Thread.sleep(6000)
                  pair._2 ! sentEvts
                }

              case None =>
                logger.error("Eb receive(): can not find this packMap: " + pair._1)
            }
          }
        })
      })
    // receive recovery-event
    case recEvt: RecoveryEvent =>
      // RecoveryEvent is sent by Event-Bus itself at the beginning of system starting.
      logger.debug("EB receive onerRecovery-event: " + recEvt.toString)
      actorRefMap.foreach(pair => {
        if (isMatch(recEvt, pair._1)) {
          packageMap.get(pair._1) match {
            case Some(list) =>
              val sentEvts = evtBus.isPackageFull(recEvt, list)
              if (sentEvts != null) {
                if(priorityMap.getOrElse(pair._1, -1) >= EventHandler.PRIORITY_HIGH)
                  while(pair._2.isTerminated)
               	    Thread.sleep(6000)
                pair._2 ! sentEvts
              }
            case None => logger.error("Eb receive(): can not find this packMap: " + pair._1)
          }
        }
      })
    // receive add-subscription-request from the EventHandler-Actor
    case addSub: EventSubscribe => 
      addSub match{
        case c:EventSubscribe_AccoutID =>
          contentMap.get(c.id) match {
            case Some(li) => 
              var isSet = false
              breakable{
                li.foreach(content => {
                  content match{
                    case cont:EventSubscribe_AccoutID => 
                      cont.list ++= c.list
                      isSet = true 
                      break
                  }
                })
                if(! isSet)
                  li += c
              }
            case None => logger.error("EB receive EventSubscribe_account:can not find this EH'ID")
          }
        case c:EventSubscribe => 
          contentMap.get(c.id) match{
            case Some(li) => li += c             
            case None => logger.error("EB receive EventSubscribe:can not find this EH'ID")
          }          
      }
    // receive delete-subscription-request from the EventHandler-Actor
    case delSub: EventSubscribeCancel =>
      delSub match{
        case c: EventSubscribeCancel_AccoutID =>
          contentMap.get(c.id) match{
            case Some(li) => 
              breakable{
                li.foreach(content => {
                  content match{
                    case cont:EventSubscribe_AccoutID =>
                      cont.list --= c.list
                      break
                  }
                })
              }              
            case None => logger.error("EB receive EventSubscribeCancel_accountID:can not find this EH'ID")
          }
        case c: EventSubscribeCancel_All =>
          contentMap.get(c.id) match{
            case Some(li) =>
              li.clear
            case None => logger.error("EB receive EventSubscribeCancel_All:can not find this EH'ID")
          }
        case c: EventSubscribe =>
          contentMap.get(c.id) match{
            case Some(li) =>
              li -= c    
            case None => logger.error("EB receive EventSubscribe:can not find this EH'ID")
          }
      }
    // receive SystemControl from other actors
    case control: SystemControl => {
      control match{
        case c:IsEventBusReady =>
          sender ! EventBusIsReady
        // add new EventHandler-Actor
        case c:AddEventHandler => 
          nameMap += ((c.id, c.name))
          priorityMap += ((c.id, c.priority))
          actorRefMap  += ((c.id, sender))
          packageMap += ((c.id, new ListBuffer[Event]()))
        // remove new EventHandler-Actor
        case c:RemoveEventHandler =>
          nameMap.remove(c.id)
          priorityMap.remove(c.id)
          actorRefMap.remove(c.id)
          contentMap.remove(c.id)
          packageMap.remove(c.id)
        // reset the priority of an EventHandler-Actor
        case c:ResetEHPriority =>
          priorityMap.put(c.id, c.priority)
        // reset the name of an EventHandler-Actor
        case c:ResetEHName =>
          nameMap.put(c.id, c.name)
        // reset the Actor-Path of an EventHandler-Actor
        case c:ResetEHPath =>
          actorRefMap.put(c.id, sender)
        case _ =>
      }
    }
    case IsEventBusReady =>
      sender ! EventBusIsReady
    case _ =>
      logger.error("Error messages are sent to the Mongo-Actor")
  }
  
  override def preStart = {
    try {
      // load the default EventHandler-Actors' information from the file(json).
      implicit val formats = DefaultFormats     
      val json:List[Map[String,String]] = parse(Source.fromFile("./src/main/resources/EventHandlersInfo.json").mkString)
        .extract[List[Map[String, String]]]
      // if the json is empty, the List().length will be 0
      json.foreach(item => {
        nameMap += ((UUID.fromString(getString(item.get("ID"))), getString(item.get("Name"))))
        priorityMap += ((UUID.fromString(getString(item.get("ID"))),
          java.lang.Double.parseDouble(getString(item.get("Priority"))).toInt))
        packageMap += ((UUID.fromString(getString(item.get("ID"))), new ListBuffer[Event]()))
        try{
          val actorRef = Await.result(context.actorSelection(getString(item.get("Path"))).
            resolveOne()(600 second), Duration.Inf)
          actorRefMap += ((UUID.fromString(getString(item.get("ID"))), actorRef))
        }
        catch{
          case _ => logger.error("can not connect to EH:　" + getString(item.get("Name")))
          // make sure EventHandler-Actors with high priority are ready.
          if(priorityMap.getOrElse(UUID.fromString(getString(item.get("ID"))), -1) >= EventHandler.PRIORITY_HIGH)
            preStart
        }
      })      
    }
    catch {
      case _ => logger.error("Error happen when load 'EventHandlersInfo.json'");
      preStart     
    }

    // get unfinished or missing events in the last system shutdown.
    evtBus.getRecoveryEvents.foreach(recEvt => self ! recEvt)
    evtBus.removeRecInfo
    logger.info("EventBus starts successfully!")
  }

  /** Detect whether this event is subscribed by this EventHandler-Actor(identitied by UUID)
    * @param evt Event
    * @param id UUID of EventHandler-Actor
    * @return whether this event is subscribed by this EventHandler-Actor
    */
  private def isMatch(evt:Event, id:UUID):Boolean = {
    contentMap.get(id) match{
      case Some(li) =>
        li.foreach(content => {
          content match{
            // subscribe events according to their event-type.
            case c:EventSubscribe_EventType =>
              c.typeName match{
                case "All" => return true 
                case _ =>
                  if(evt.getClass.getName.contains(c.typeName))
                    return true 
                  else{
                    evt match{
                      case e:RecoveryEvent =>
                        if(e.getClass.getName.contains(c.typeName.substring(0, c.typeName.length - 5) + "RecEvent"))
                          return true
                    }
                  }
              }
            // subscribe events according to their committed time.
            case c:EventSubscribe_TimeDuration =>
              if(c.startTime == null || c.startTime <= evt.committedTime.getTime)//null is Inf
                if(c.endTime == null || evt.committedTime.getTime <= c.endTime)
                  return true
            // subscribe events according to their related account-id.
            case c:EventSubscribe_AccoutID =>
              if(c.list.contains(evt.accountID))
                return true
            // subscribe events according to their related account's balance.
            case c:EventSubscribe_Balance =>
              if(c.min ==null || c.min.compareTo(evt.balance) <= 0)
                if(c.max == null || evt.balance.compareTo(c.max) <= 0)
                  return true
            // subscribe events according to their related account's currency.
            case c:EventSubscribe_Currency =>
              if(c.typeName == evt.currency)
                return true
          }
        })
      case None => 
        logger.error("EB, isMatch(): can not find id:" + id.toString)
    }
    return false
  }

  // private function to get String from Option[String]
  private def getString(obj:Option[String]):String = {
    obj match{
      case Some(str) => return str
      case None => logger.error("getString: get None"); return null
    }
  }
}

/** An implementation of EventHandler and Akka-Actor.
  * It receives events from the EventBus and handle these events.
  * @param evtHdl EventHandler
  */
class EventHandlerActor(evtHdl: EventHandler) extends Actor {
  override def receive = {
    case evt: Event =>
      evtHdl.handle(evt)
    case _ =>
      logger.error("EventHandler:" + evtHdl.uuid.toString + " receive a wrong type message.")
  }

  override def preStart = {
    logger.info("EventHandler:" + evtHdl.uuid.toString + " starts successfully!")
  }
}