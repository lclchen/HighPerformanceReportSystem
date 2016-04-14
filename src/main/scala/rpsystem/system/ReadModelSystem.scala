/*
 * Collaborative Applied Research and Development between Morgan Stanley and University
 */

package rpsystem.system

import java.util.UUID

import akka.actor._
import redis.clients.jedis._
import com.typesafe.config.{Config, ConfigFactory}
import com.mongodb.casbah.Imports.MongoClient
import com.mongodb.casbah.commons.Imports.MongoDBObject
import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.MongoDB

import rpsystem.domain.Event

class ReadModelSystem {
  val config: Config = ConfigFactory.load("./system/read_model.conf")
  val accountMongoDB: MongoCollection = MongoClient(
    config.getString("system.mongo.account.host"),
    config.getInt("system.mongo.account.port"))
    .getDB(config.getString("system.mongo.account.db"))
    .apply(config.getString("system.mongo.account.collection"))
  val eventMongoDB: MongoCollection = MongoClient(
    config.getString("system.mongo.event.host"),
    config.getInt("system.mongo.event.port"))
    .getDB(config.getString("system.mongo.event.db"))
    .apply(config.getString("system.mongo.event.collection"))

 /**
  * define any report types according to the business requirement
  * Example:
  * def readEventsByAccountID(accountId: UUID): List[Event] = {
  *   ... ...
  * }
  */
}
