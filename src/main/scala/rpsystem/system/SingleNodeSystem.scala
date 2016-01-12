package rpsystem.system

import akka._
import akka.actor._
import redis.clients.jedis._
import com.mongodb.casbah.Imports.MongoClient
import com.mongodb.casbah.commons.Imports.MongoDBObject
import com.mongodb.casbah.commons.Imports.DBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.MongoDB
import rpsystem.domain._
import rpsystem.actorsystem._
import rpsystem.persistence._
import rpsystem.recovery._

class SingleNodeSystem {
  var actorSystemName:String = "SingleNodeReportSystem"
  var shardingsNumber:Int = 1
  var isCmdRecoveryAvailable:Boolean = true
  var isEvtRecoveryAvailable:Boolean = true
  var cmdBusMode:CommandBus.SHARDINGS_MODE = CommandBus.SHARDINGS_MODE_MOD_ACCOUTID(shardingsNumber)
  var cmdHdlMode:CommandHandler.SNAPSHOT_MODE = CommandHandler.SNAPSHOT_MODE_ALWAYS()
  var evtBusMode:EventBus.BROADCAST_MODE = EventBus.BROADCAST_MODE_NOPACK()
  
  //var cmdProcessingRedisClient:RedisClient = new RedisClient("localhost", 6379)
  //var evtProcessingRedisClient:RedisClient = new RedisClient("localhost", 6379)
  var cmdMessageMongoCol:MongoCollection = MongoClient("localhost", 27017).getDB("rpsystem").apply("cmd_backup")
  var evtMessageMongoCol:MongoCollection = MongoClient("localhost", 27017).getDB("rpsystem").apply("evt_backup")
  
  var mongodbAccountStore:MongoDB = MongoClient("localhost", 27017).getDB("rpsystem")
  var mongodbEventStore:MongoDB = MongoClient("localhost", 27017).getDB("rpsystem")  
  
  var system:ActorSystem = null
  var commandMiddlewareActor:ActorRef = null
  var commandBusActor:ActorRef = null
  var cmdhdlActors:Seq[ActorRef] = null
  var eventBusActor:ActorRef = null
      
  def setActorSystemName(name:String):Unit = {
    actorSystemName = name
  }
  
  def setShardingsNumber(num:Int):Unit = {
    if(num != shardingsNumber){
      cmdhdlActors = null
    }
    shardingsNumber = num
  }
  
  def setCmdRecoveryAvailable(bool:Boolean):Unit = {
    isCmdRecoveryAvailable = bool
  }
  
  def setEvtRecoveryAvailable(bool:Boolean):Unit = {
    isEvtRecoveryAvailable = bool
  }
  
  def setCmdBusMode(mode:CommandBus.SHARDINGS_MODE):Unit = {
    cmdBusMode = mode
  }
  
  def setCmdHdlMode(mode:CommandHandler.SNAPSHOT_MODE):Unit = {
    cmdHdlMode = mode
  }
  
  def getCmdHdlMode():CommandHandler.SNAPSHOT_MODE = {
    cmdHdlMode match{
      case mode:CommandHandler.SNAPSHOT_MODE_ALWAYS =>
        return CommandHandler.SNAPSHOT_MODE_ALWAYS()
      case mode:CommandHandler.SNAPSHOT_MODE_EVENTSNUM =>
        return CommandHandler.SNAPSHOT_MODE_EVENTSNUM(mode.num, 0)
      case mode:CommandHandler.SNAPSHOT_MODE_MILLISECOND =>
        return CommandHandler.SNAPSHOT_MODE_MILLISECOND(mode.duration, new java.util.Date().getTime())
      case _ =>
        return CommandHandler.SNAPSHOT_MODE_ALWAYS()
    }
  }
  
  def setEvtBusMode(mode:EventBus.BROADCAST_MODE):Unit = {
    evtBusMode = mode
  }
  
  /*
  def setCmdProcessingRedisClient(client:RedisClient):Unit = {
    cmdProcessingRedisClient = client
  }
  
  def setEvtProcessingRedisClient(client:RedisClient):Unit = {
    evtProcessingRedisClient = client
  }
  * */
  
  def setCmdMessageMongoCol(col:MongoCollection):Unit = {
    cmdMessageMongoCol = col
  }
  
  def setEvtMessageMongoCol(col:MongoCollection):Unit = {
    evtMessageMongoCol = col
  }
  
  def setMongodbAccountStore(db:MongoDB):Unit = {
    mongodbAccountStore = db
  }
  
  def setMongodbEventStore(db:MongoDB):Unit = {
    mongodbEventStore = db
  }
  
  def setCommandHandlerActors(actors:Seq[ActorRef]) = {
    //set shardingsNum first before this function
    cmdhdlActors = actors
    if(actors.length != shardingsNumber)
      cmdhdlActors = null
  }
  
  def createDefaultCappedCollection():Unit = {
    val options = MongoDBObject()
    options.put("capped", true)
    options.put("size", 102400000)
    options.put("max", 100000)
    if(!MongoClient("localhost", 27017).getDB("rpsystem").collectionExists("cmd_backup"))
    	MongoClient("localhost", 27017).getDB("rpsystem").createCollection("cmd_backup", options) 
    if(!MongoClient("localhost", 27017).getDB("rpsystem").collectionExists("evt_backup"))
    	MongoClient("localhost", 27017).getDB("rpsystem").createCollection("evt_backup", options) 
  }
  
  def initial() = {
    
    // initial Actor System
    system = ActorSystem(actorSystemName)
    
    val eventBus = new EventBus(getDefaultCommandRecoveryService, getDefaultEventRecoveryService)
    eventBus.setMode(evtBusMode)
    eventBusActor = system.actorOf(Props(new EventBusActor(eventBus)), name ="EventBusActor")
    
    cmdhdlActors match{
      case null => 
        cmdhdlActors = new scala.collection.mutable.ArrayBuffer[ActorRef]()
        for(i <- 1 to shardingsNumber){
          val accountPersistence = new MongoPersistence(mongodbAccountStore)
          accountPersistence.ensureIndex
          val accountStore = new AccountStore(accountPersistence)
          
          val eventPersistence = new MongoPersistence(mongodbEventStore)
          eventPersistence.ensureIndex
          val eventStore = new EventStore(eventPersistence)
          
          val repository = new Repository(accountStore, eventStore)
          val commandHandler = new CommandHandler(repository, getDefaultEventRecoveryService)
          commandHandler.setMode(getCmdHdlMode)
          val commandHandlerActor = system.actorOf(Props(new CommandHandlerActor(commandHandler, eventBusActor)), name="CommandHandlerActor"+i)
          cmdhdlActors = cmdhdlActors :+ commandHandlerActor
        }
      case _ =>
    }
    
    val commandBus = new CommandBus(getDefaultCommandRecoveryService)
    commandBus.setMode(cmdBusMode)
    commandBusActor = system.actorOf(Props(new CommandBusActor(commandBus, cmdhdlActors, eventBusActor)), name="CommandBusActor")
      
    val commandMiddleware = new CommandMiddleware(getDefaultCommandRecoveryService)
    commandMiddlewareActor = system.actorOf(Props(new CommandMiddlewareActor(commandMiddleware, commandBusActor)), name="CommandMiddlewareActor")    
  }
  
  def sendCommand(cmd: Command):Unit = {
    commandMiddlewareActor ! cmd
  }
  
  def addAccountSnapshot(account: AccountAggr):Unit = {
    val persistence = new MongoPersistence(mongodbAccountStore)
    persistence.addSnapshot(account)
  }
  
  def getDefaultCommandRecoveryService():CommandRecoveryService = {
    val redisProcessingCmd = new RedisProcessingCmdRecStorage(new Jedis("localhost"))
    val mongoMessageCmd = new MongoMessageCmdRecStorage(cmdMessageMongoCol)        
    val cmdRecoveryService = new CommandRecoveryService(redisProcessingCmd, mongoMessageCmd) 
    cmdRecoveryService.available = isCmdRecoveryAvailable 
    return cmdRecoveryService
  }
  
  def getDefaultEventRecoveryService():EventRecoveryService = {
    val redisProcessingEvt = new RedisProcessingEvtRecStorage(new Jedis("localhost"))
    val mongoMessageEvt = new MongoMessageEvtRecStorage(evtMessageMongoCol)
    val evtRecoveryService = new EventRecoveryService(redisProcessingEvt, mongoMessageEvt) 
    evtRecoveryService.available = isEvtRecoveryAvailable 
    return evtRecoveryService
  }
  
  def shutdown():Unit = {
    system.shutdown()
  }
}