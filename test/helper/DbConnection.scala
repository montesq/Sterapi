package test.helper

import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global

object DbConnection {
  val driver = new MongoDriver
  val connection = driver.connection(List("localhost"))
  val db = connection("sterapi-db")

}
