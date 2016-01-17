package rpsystem.example

import redis.clients.jedis._

object JedisTest {
  def main(args:Array[String]) {
    val client = new Jedis()

    for (i <- 1 to 100000) {
      //val client = pool.getResource
      client.set("a", "b")
      //p.exec()
      logger.info(i.toString)
      //pool.returnResource(client)
    }
  }
}
