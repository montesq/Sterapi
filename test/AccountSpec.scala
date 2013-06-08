import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.libs.json._
import helper._

import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DBConnection

class AccountSpec extends Specification {

  val accountsColl = DBConnection.db.collection[JSONCollection]("accounts")
  accountsColl.drop()

  val defaultAccountJson = Json.obj(
    "name" -> "ACME Company",
    "contacts" -> List("1", "2", "3", "4")
  )

  "POST /accounts" should {
    "insert a new account with the status ACTIVE" in new WithApplication {
      val Some(result) = route(FakeRequest(POST, "/accounts").
        withJsonBody(defaultAccountJson))

      status(result) must equalTo(CREATED)
      val jsonResult = Json.parse(contentAsString(result))
      (__ \ "status")(jsonResult) must contain(JsString("ACTIVE"))

    }

    "generate an error when name is missing" in new WithApplication {
      val accountWithoutName = defaultAccountJson.transform((__ \ "name").json.prune)

      val Some(result) = route(FakeRequest(POST, "/accounts").
        withJsonBody(accountWithoutName.get))
      val errJson = Json.parse(contentAsString(result))
      status(result) must equalTo(BAD_REQUEST)
      (__ \ "obj.name")(errJson) must containAnyOf(Seq(ErrorValidation.missingPath))
    }
  }

  "GET /accounts" should {
    "generate empty array when the collection is empty" in new WithApplication {
      accountsColl.drop()

      val Some(result) = route(FakeRequest(GET, "/accounts"))

      status(result) must equalTo(OK)
      val jsonResult = Json.parse(contentAsString(result))
      jsonResult.as[JsArray] must equalTo(JsArray())
    }

    "return accounts that have just been inserted" in new WithApplication {
      val Some(result1) = route(FakeRequest(POST, "/accounts").
        withJsonBody(defaultAccountJson))
      val account1 = Json.parse(contentAsString(result1))

      val Some(result2) = route(FakeRequest(POST, "/accounts").
        withJsonBody(defaultAccountJson))
      val account2 = Json.parse(contentAsString(result2))

      val Some(result3) = route(FakeRequest(GET, "/accounts"))

      status(result3) must equalTo(OK)
      val listJsonAccounts = Json.parse(contentAsString(result3))
      listJsonAccounts.as[JsArray].value must contain(account1)
      listJsonAccounts.as[JsArray].value must contain(account2)
    }
  }

  "GET /accounts/:id" should {
    "return the json of the inserted account" in new WithApplication {
      val Some(result) = route(FakeRequest(POST, "/accounts").
        withJsonBody(defaultAccountJson))
      val json = Json.parse(contentAsString(result))
      val id = (__ \ "_id")(json).head.as[String]

      val Some(result2) = route(FakeRequest(GET, "/accounts/" + id))
      status(result2) must equalTo(OK)

      val json2 = Json.parse(contentAsString(result2))
      json2 must equalTo(json)
    }

    "return a 404 error if the id is not a Mongo ObjectId" in new WithApplication{
      val Some(result) = route(FakeRequest(GET, "/accounts/azerty"))
      status(result) must beEqualTo(NOT_FOUND)
    }

    "return a 404 error if the id is not in the database" in new WithApplication{
      val Some(result) = route(FakeRequest(GET, "/accounts/51abb041ae01081d007afa11"))
      status(result) must beEqualTo(NOT_FOUND)
    }
  }

  "PUT /accounts/:id" should {
    "update the fields" in new WithApplication {
      val Some(result) = route(FakeRequest(POST, "/accounts").
        withJsonBody(defaultAccountJson))
      val json = Json.parse(contentAsString(result))
      val id = (__ \ "_id")(json).head.as[String]

      val newJson = defaultAccountJson.transform(
        __.json.update(
          (__ \ "name").json
            .put(JsString("Free Software Company"))) andThen
          __.json.update(
            (__ \ "contacts").json
              .put(JsArray(Seq(JsString("5"), JsString("6"))))
          )
      ).get

      val Some(result2) = route(FakeRequest(PUT, "/accounts/" + id).
        withJsonBody(newJson))
      val json2 = Json.parse(contentAsString(result2))
      status(result2) must equalTo(OK)

      val Some(result3) = route(FakeRequest(GET, "/accounts/" + id))
      val json3 = Json.parse(contentAsString(result3))
      (__ \ "name")(json3) must contain(JsString("Free Software Company"))
      (__ \ "contacts")(json3) must contain(JsArray(Seq(JsString("5"), JsString("6"))))
      (__ \ "modified_on")(json3) must not equalTo (((__ \ "created_on")(json3)))
    }
  }
}
