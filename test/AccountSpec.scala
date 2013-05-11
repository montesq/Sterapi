import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, FakeApplication}
import play.api.libs.json._
import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global
import helper._


class AccountSpec extends Specification {

  DbConnection.db("accounts").drop()

  "Account" should {

    "insert a new account" in {
      running(FakeApplication()) {
        val json = Json.obj(
          "technical_name" -> "sterichem",
          "name" -> "Stérichem"
        )

        val Some(result) = route(FakeRequest(POST, "/accounts").
          withJsonBody(json))

        status(result) must equalTo(CREATED)
      }
    }

    "insert a new account2" in {
      running(FakeApplication()) {
        val json = Json.obj(
          "technical_name" -> "sterichem2",
          "name" -> "Stérichem2"
        )

        val Some(result) = route(FakeRequest(POST, "/accounts").
          withJsonBody(json))

        status(result) must equalTo(CREATED)
      }
    }

    "prevent duplicate creation" in {
      running(FakeApplication()) {
        val json = Json.obj(
          "technical_name" -> "electricite_de_france",
          "name" -> "Électricité de France"
        )

        val Some(result1) = route(FakeRequest(POST, "/accounts").
          withJsonBody(json))
        val Some(result2) = route(FakeRequest(POST, "/accounts").
          withJsonBody(json))

        status(result2) must equalTo(BAD_REQUEST)
      }
    }

    "technical_name and name are mandatory" in {
      running(FakeApplication()) {
        val json = Json.obj()

        val Some(result) = route(FakeRequest(POST, "/accounts").
          withJsonBody(json))
        val errJson = Json.parse(contentAsString(result))
        status(result) must equalTo(BAD_REQUEST)
        (__ \ "obj.technical_name")(errJson) must containAnyOf(Seq(ErrorValidation.missingPath))
        (__ \ "obj.name")(errJson) must containAnyOf(Seq(ErrorValidation.missingPath))
      }
    }

  }
}
