package rpsystem.system

import akka.actor._
import redis.clients.jedis._
import com.typesafe.config.{Config, ConfigFactory}
import com.mongodb.casbah.Imports.MongoClient
import com.mongodb.casbah.commons.Imports.MongoDBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.MongoDB
import scala.collection.JavaConverters._
import rpsystem.domain._
import rpsystem.actorsystem._
import rpsystem.persistence._
import rpsystem.recovery._

class CommandMiddlewareOfMultiNodeSystem {
  var actorSystemName:String = "MultiNodeReportSystem_CommandMiddleware"
  val config: Config = ConfigFactory.load("./system/multi_node.conf")

  var isCmdRecoveryAvailable:Boolean = config.getBoolean("system.recovery.command.available")
  var cmdMessageMongoCol:MongoCollection = MongoClient(config.getString("system.recovery.command.mongo.host"),
    config.getInt("system.recovery.command.mongo.port"))
    .getDB(config.getString("system.recovery.command.mongo.db"))
    .apply(config.getString("system.recovery.command.mongo.collection"))

  var system:ActorSystem = null
  var commandMiddlewareActor:ActorRef = null
  var commandBusActor:ActorRef = null

  def setCmdRecoveryAvailable(bool:Boolean):Unit = {
    isCmdRecoveryAvailable = bool
  }

  def createDefaultCommandCappedCollection():Unit = {
    val options = MongoDBObject()
    options.put("capped", true)
    options.put("size", 102400000)
    options.put("max", 100000)
    val cmdDb = MongoClient(config.getString("system.recovery.command.mongo.host"),
      config.getInt("system.recovery.command.mongo.port"))
      .getDB(config.getString("system.recovery.command.mongo.db"))

    if(!cmdDb.collectionExists(config.getString("system.recovery.command.mongo.collection")))
      cmdDb.createCollection(config.getString("system.recovery.command.mongo.collection"), options)
  }

  def initial() = {
    // initial Actor System
    system = ActorSystem(actorSystemName)

    commandBusActor = system.actorFor(config.getString("system.command-bus.path"))

    val commandMiddleware = new CommandMiddleware(getDefaultCommandRecoveryService)
    commandMiddlewareActor = system.actorOf(Props(new CommandMiddlewareActor(commandMiddleware, commandBusActor)),
      name="CommandMiddlewareActor")
  }

  def sendCommand(cmd: Command):Unit = {
    commandMiddlewareActor ! cmd
  }

  def getDefaultCommandRecoveryService():CommandRecoveryService = {
    val redisProcessingCmd = new RedisProcessingCmdRecStorage(new Jedis(config.getString("system.recovery.command.redis.host"),
      config.getInt("system.recovery.command.redis.port")))
    val mongoMessageCmd = new MongoMessageCmdRecStorage(cmdMessageMongoCol)
    val cmdRecoveryService = new CommandRecoveryService(redisProcessingCmd, mongoMessageCmd)
    cmdRecoveryService.available = isCmdRecoveryAvailable
    return cmdRecoveryService
  }

  def shutdown():Unit = {
    system.shutdown()
  }
}

class CommandBusOfMultiNodeSystem {
  var actorSystemName:String = "MultiNodeReportSystem_CommandBus"
  val config: Config = ConfigFactory.load("./system/multi_node.conf")
  var shardingsNumber:Int = config.getInt("system.command-handler.shardings")
  var isCmdRecoveryAvailable:Boolean = config.getBoolean("system.recovery.command.available")
  var cmdBusMode:CommandBus.SHARDINGS_MODE = CommandBus.SHARDINGS_MODE_MOD_ACCOUTID(shardingsNumber)

  var cmdMessageMongoCol:MongoCollection = MongoClient(config.getString("system.recovery.command.mongo.host"),
    config.getInt("system.recovery.command.mongo.port"))
    .getDB(config.getString("system.recovery.command.mongo.db"))
    .apply(config.getString("system.recovery.command.mongo.collection"))

  var system:ActorSystem = null
  var commandBusActor:ActorRef = null
  var cmdHdlActors:Seq[ActorRef] = null
  var eventBusActor:ActorRef = null

  def setShardingsNumber(num:Int):Unit = {
    if(num != shardingsNumber){
      cmdHdlActors = null
    }
    shardingsNumber = num
  }

  def setCmdRecoveryAvailable(bool:Boolean):Unit = {
    isCmdRecoveryAvailable = bool
  }

  def setCmdBusMode(mode:CommandBus.SHARDINGS_MODE):Unit = {
    cmdBusMode = mode
  }

  def setCommandHandlerActors(actors: Seq[ActorRef]) = {
    //set shardingsNum first before this function
    cmdHdlActors = actors
    if(actors.length != shardingsNumber)
      cmdHdlActors = null
  }

  def createDefaultCommandCappedCollection():Unit = {
    val options = MongoDBObject()
    options.put("capped", true)
    options.put("size", 102400000)
    options.put("max", 100000)
    val cmdDb = MongoClient(config.getString("system.recovery.command.mongo.host"),
      config.getInt("system.recovery.command.mongo.port"))
      .getDB(config.getString("system.recovery.command.mongo.db"))

    if(!cmdDb.collectionExists(config.getString("system.recovery.command.mongo.collection")))
      cmdDb.createCollection(config.getString("system.recovery.command.mongo.collection"), options)
  }

  def initial() = {
    // initial Actor System
    system = ActorSystem(actorSystemName)

    eventBusActor = system.actorFor(config.getString("system.event-bus.path"))

    cmdHdlActors match{
      case null =>
        config.getStringList("system.command-handler.path").asScala.foreach(path =>
          cmdHdlActors = cmdHdlActors :+ system.actorFor(path))
      case _ =>
    }

    val commandBus = new CommandBus(getDefaultCommandRecoveryService)
    commandBus.setMode(cmdBusMode)
    commandBusActor = system.actorOf(Props(new CommandBusActor(commandBus, cmdHdlActors, eventBusActor)),
      name="CommandBusActor")
  }

  def getDefaultCommandRecoveryService(): CommandRecoveryService = {
    val redisProcessingCmd = new RedisProcessingCmdRecStorage(new Jedis(config.getString("system.recovery.command.redis.host"),
      config.getInt("system.recovery.command.redis.port")))
    val mongoMessageCmd = new MongoMessageCmdRecStorage(cmdMessageMongoCol)
    val cmdRecoveryService = new CommandRecoveryService(redisProcessingCmd, mongoMessageCmd)
    cmdRecoveryService.available = isCmdRecoveryAvailable
    return cmdRecoveryService
  }

  def shutdown():Unit = {
    system.shutdown()
  }
}

class CommandHandlerOfMultiNodeSystem(numberOfCommandHandler: Int) {
  // numberOfCommandHandler should start from 1
  val config: Config = ConfigFactory.load("./system/multi_node.conf")
  var actorSystemName:String = "MultiNodeReportSystem_CommandHandler_" + numberOfCommandHandler
  var shardingsNumber:Int = config.getInt("system.command-handler.shardings")
  var isEvtRecoveryAvailable:Boolean = config.getBoolean("system.recovery.event.available")
  var cmdHdlMode:CommandHandler.SNAPSHOT_MODE = CommandHandler.SNAPSHOT_MODE_ALWAYS()

  var evtMessageMongoCol:MongoCollection = MongoClient(config.getString("system.recovery.event.mongo.host"),
    config.getInt("system.recovery.event.mongo.port"))
    .getDB(config.getString("system.recovery.event.mongo.db"))
    .apply(config.getString("system.recovery.event.mongo.collection"))

  var mongodbAccountStore:MongoDB = MongoClient(config.getString("system.command-handler.mongo.default.account-store.host"),
    config.getInt("system.command-handler.mongo.default.account-store.port"))
    .getDB(config.getString("system.command-handler.mongo.default.account-store.db"))
  var mongodbEventStore:MongoDB = MongoClient(config.getString("system.command-handler.mongo.default.event-store.host"),
    config.getInt("system.command-handler.mongo.default.event-store.port"))
    .getDB(config.getString("system.command-handler.mongo.default.event-store.db"))

  var system:ActorSystem = null
  var cmdHdlActor: ActorRef = null
  var eventBusActor: ActorRef = null

  def setShardingsNumber(num:Int):Unit = {
    if(num != shardingsNumber){
      cmdHdlActor = null
    }
    shardingsNumber = num
  }

  def setEvtRecoveryAvailable(bool:Boolean):Unit = {
    isEvtRecoveryAvailable = bool
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

  def createDefaultEventCappedCollection():Unit = {
    val options = MongoDBObject()
    options.put("capped", true)
    options.put("size", 102400000)
    options.put("max", 100000)
    val evtDb = MongoClient(config.getString("system.recovery.event.mongo.host"),
      config.getInt("system.recovery.event.mongo.port"))
      .getDB(config.getString("system.recovery.event.mongo.db"))

    if(!evtDb.collectionExists(config.getString("system.recovery.event.mongo.collection")))
      evtDb.createCollection(config.getString("system.recovery.event.mongo.collection"), options)
  }

  def initial() = {
    // initial Actor System
    system = ActorSystem(actorSystemName)

    eventBusActor = system.actorFor(config.getString("system.event-bus.path"))

    val accountPersistence = new MongoPersistence(mongodbAccountStore)
    accountPersistence.ensureIndex
    val accountStore = new AccountStore(accountPersistence)

    val eventPersistence = new MongoPersistence(mongodbEventStore)
    eventPersistence.ensureIndex
    val eventStore = new EventStore(eventPersistence)

    val repository = new Repository(accountStore, eventStore)
    val commandHandler = new CommandHandler(repository, getDefaultEventRecoveryService)
    commandHandler.setMode(getCmdHdlMode)
    cmdHdlActor = system.actorOf(Props(new CommandHandlerActor(commandHandler, eventBusActor)),
      name = "CommandHandlerActor_" + numberOfCommandHandler)
  }

  def addAccountSnapshot(account: AccountAggr):Unit = {
    val persistence = new MongoPersistence(mongodbAccountStore)
    persistence.addSnapshot(account)
  }

  def getDefaultEventRecoveryService():EventRecoveryService = {
    val redisProcessingEvt = new RedisProcessingEvtRecStorage(new Jedis(config.getString("system.recovery.event.redis.host"),
      config.getInt("system.recovery.event.redis.port")))
    val mongoMessageEvt = new MongoMessageEvtRecStorage(evtMessageMongoCol)
    val evtRecoveryService = new EventRecoveryService(redisProcessingEvt, mongoMessageEvt)
    evtRecoveryService.available = isEvtRecoveryAvailable
    return evtRecoveryService
  }

  def shutdown():Unit = {
    system.shutdown()
  }
}

class EventBusOfMultiNodeSystem {
  var actorSystemName:String = "MultiNodeReportSystem_EventBus"
  val config: Config = ConfigFactory.load("./system/multi_node.conf")
  var isCmdRecoveryAvailable:Boolean = config.getBoolean("system.recovery.command.available")
  var isEvtRecoveryAvailable:Boolean = config.getBoolean("system.recovery.event.available")
  var evtBusMode:EventBus.BROADCAST_MODE = EventBus.BROADCAST_MODE_NOPACK()

  var cmdMessageMongoCol:MongoCollection = MongoClient(config.getString("system.recovery.command.mongo.host"),
    config.getInt("system.recovery.command.mongo.port"))
    .getDB(config.getString("system.recovery.command.mongo.db"))
    .apply(config.getString("system.recovery.command.mongo.collection"))
  var evtMessageMongoCol:MongoCollection = MongoClient(config.getString("system.recovery.event.mongo.host"),
    config.getInt("system.recovery.event.mongo.port"))
    .getDB(config.getString("system.recovery.event.mongo.db"))
    .apply(config.getString("system.recovery.event.mongo.collection"))

  var system:ActorSystem = null
  var eventBusActor:ActorRef = null

  def setCmdRecoveryAvailable(bool:Boolean):Unit = {
    isCmdRecoveryAvailable = bool
  }

  def setEvtRecoveryAvailable(bool:Boolean):Unit = {
    isEvtRecoveryAvailable = bool
  }

  def setEvtBusMode(mode:EventBus.BROADCAST_MODE):Unit = {
    evtBusMode = mode
  }

  def createDefaultCappedCollection():Unit = {
    val options = MongoDBObject()
    options.put("capped", true)
    options.put("size", 102400000)
    options.put("max", 100000)
    val cmdDb = MongoClient(config.getString("system.recovery.command.mongo.host"),
      config.getInt("system.recovery.command.mongo.port"))
      .getDB(config.getString("system.recovery.command.mongo.db"))
    val evtDb = MongoClient(config.getString("system.recovery.event.mongo.host"),
      config.getInt("system.recovery.event.mongo.port"))
      .getDB(config.getString("system.recovery.event.mongo.db"))

    if(!cmdDb.collectionExists(config.getString("system.recovery.command.mongo.collection")))
      cmdDb.createCollection(config.getString("system.recovery.command.mongo.collection"), options)

    if(!evtDb.collectionExists(config.getString("system.recovery.event.mongo.collection")))
      evtDb.createCollection(config.getString("system.recovery.event.mongo.collection"), options)
  }

  def initial() = {
    // initial Actor System
    system = ActorSystem(actorSystemName)

    val eventBus = new EventBus(getDefaultCommandRecoveryService, getDefaultEventRecoveryService)
    eventBus.setMode(evtBusMode)
    eventBusActor = system.actorOf(Props(new EventBusActor(eventBus)), name ="EventBusActor")
  }

  def getDefaultCommandRecoveryService():CommandRecoveryService = {
    val redisProcessingCmd = new RedisProcessingCmdRecStorage(new Jedis(config.getString("system.recovery.command.redis.host"),
      config.getInt("system.recovery.command.redis.port")))
    val mongoMessageCmd = new MongoMessageCmdRecStorage(cmdMessageMongoCol)
    val cmdRecoveryService = new CommandRecoveryService(redisProcessingCmd, mongoMessageCmd)
    cmdRecoveryService.available = isCmdRecoveryAvailable
    return cmdRecoveryService
  }

  def getDefaultEventRecoveryService():EventRecoveryService = {
    val redisProcessingEvt = new RedisProcessingEvtRecStorage(new Jedis(config.getString("system.recovery.event.redis.host"),
      config.getInt("system.recovery.event.redis.port")))
    val mongoMessageEvt = new MongoMessageEvtRecStorage(evtMessageMongoCol)
    val evtRecoveryService = new EventRecoveryService(redisProcessingEvt, mongoMessageEvt)
    evtRecoveryService.available = isEvtRecoveryAvailable
    return evtRecoveryService
  }

  def shutdown():Unit = {
    system.shutdown()
  }
}