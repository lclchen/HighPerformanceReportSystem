/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.system

import java.util.UUID
import akka.actor._
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports.MongoClient
import com.mongodb.casbah.Imports.MongoDB
import com.mongodb.casbah.commons.Imports.MongoDBObject
import com.typesafe.config._
import redis.clients.jedis.Jedis
import rpsystem.actorsystem._
import rpsystem.domain._
import rpsystem.persistence.MongoPersistence
import rpsystem.recovery._

class EventHandlerSystem() {
  var uuid = config.getString("system.event-handler.uuid")
  var actorSystemName = "EventHandlerSystem_" + uuid
  val config:Config = ConfigFactory.load("./system/eventhandler_node.conf")

  var isEvtRecoveryAvailable:Boolean = config.getBoolean("system.recovery.event.available")
  var evtMessageMongoCol:MongoCollection = MongoClient(config.getString("system.recovery.event.mongo.host"),
    config.getInt("system.recovery.event.mongo.port"))
    .getDB(config.getString("system.recovery.event.mongo.db"))
    .apply(config.getString("system.recovery.event.mongo.collection"))

  var mongodbReportStore:MongoDB = MongoClient(config.getString("system.event-handler.mongo.report.host"),
    config.getInt("system.event-handler.mongo.report.port"))
    .getDB(config.getString("system.event-handler.mongo.report.db"))

  var system: ActorSystem = null
  var eventHandlerActor: ActorRef = null

  def setEvtRecoveryAvailable(bool:Boolean):Unit = {
    isEvtRecoveryAvailable = bool
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

    val reportPersistence = new MongoPersistence(mongodbReportStore)
    reportPersistence.ensureIndex
    val eventHandler = new EventHandler(reportPersistence, getDefaultEventRecoveryService, UUID.fromString(uuid))
    eventHandlerActor = system.actorOf(Props(new EventHandlerActor(eventHandler)), name = "EventHandlerActor_" + uuid)
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
