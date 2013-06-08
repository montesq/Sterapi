package utils

import reactivemongo.api.MongoDriver
import scala.concurrent.ExecutionContext.Implicits.global

object DBConnection {
  //Can't use the mongoController or ReactiveMongoPlugin for the moment
  //https://github.com/zenexity/Play-ReactiveMongo/issues/32
  val driver = new MongoDriver
  val connection = driver.connection(List("localhost"))
  val db = connection("sterapi-db")
}
