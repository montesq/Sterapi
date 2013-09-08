package test

import org.specs2.mutable._
import play.api.libs.ws.{Response, WS}
import play.api.test._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.libs.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.Some
import utils.DBConnection

class AuthSpec extends Specification {

  val usersColl = DBConnection.db.collection[JSONCollection]("users")
  // get an assertion from the persona test API
  val future = WS.url("http://personatestuser.org/email_with_assertion/http%3A%2F%2Flocalhost%3A9000").get()
  val wsResult = Await.result(future, Duration(10, SECONDS))

  Await.result(usersColl.insert(wsResult.json), Duration(10, SECONDS))


  "POST /api/login" should {
    "return 200 with a good assertion" in new WithApplication {
      val Some(result) = route(FakeRequest(POST, "/api/login").withBody(wsResult.json))
      status(result) must equalTo(OK)
    }
  }
}
