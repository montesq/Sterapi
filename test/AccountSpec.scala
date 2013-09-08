import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.api.libs.json._
import helper._
import play.api.cache.Cache
import play.api.libs.Crypto._
import play.api.mvc.Session
import play.api.Play.current
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DBConnection
import scala.concurrent.Await
import scala.concurrent.duration._

class AccountSpec extends Specification {

  val accountsColl = DBConnection.db.collection[JSONCollection]("accounts")
  accountsColl.drop()
  val usersColl = DBConnection.db.collection[JSONCollection]("users")
  usersColl.drop()

  val defaultAccountJson = Json.obj(
    "name" -> "ACME Company",
    "contacts" -> List("1", "2", "3", "4")
  )

  val emailUser = "test@test.fr"
  val manageAccountsRight = "ACCOUNT_MANAGER"
  Await.result(usersColl.insert(Json.obj("email" -> emailUser, "Profiles" -> List(manageAccountsRight))),
    Duration(10, SECONDS))
  def session(email: String) = ("email", email)

  "POST /accounts" should {
    "insert a new account with the status ACTIVE" in new WithApplication {
      val Some(result) = route(FakeRequest(POST, "/api/accounts")
        .withJsonBody(defaultAccountJson)
        .withSession(session(emailUser)))

      status(result) must equalTo(CREATED)
      val jsonResult = Json.parse(contentAsString(result))
      (__ \ "status")(jsonResult) must contain(JsString("ACTIVE"))
    }

    "generate an error when name is missing" in new WithApplication {
      val accountWithoutName = defaultAccountJson.transform((__ \ "name").json.prune)

      val Some(result) = route(FakeRequest(POST, "/api/accounts")
        .withJsonBody(accountWithoutName.get)
        .withSession(session(emailUser)))
      val errJson = Json.parse(contentAsString(result))
      status(result) must equalTo(BAD_REQUEST)
      (__ \ "obj.name")(errJson) must containAnyOf(Seq(ErrorValidation.missingPath))
    }

//    "return a 403 error if the user has not the right MANAGE_ACCOUNTS" in new WithApplication {
//      Cache.set("User." + emailUser, User(emailUser, List(UserRight("OTHER_RIGHT"))))
//      val Some(result) = route(FakeRequest(POST, "/api/accounts")
//        .withJsonBody(defaultAccountJson)
//        .withSession(session(emailUser)))
//
//      status(result) must equalTo(FORBIDDEN)
//    }
  }

  "GET /accounts" should {
    "generate empty array when the collection is empty" in new WithApplication {
      accountsColl.drop()

      val Some(result) = route(FakeRequest(GET, "/api/accounts")
        .withSession(session(emailUser)))

      status(result) must equalTo(OK)
      val jsonResult = Json.parse(contentAsString(result))
      jsonResult.as[JsArray] must equalTo(JsArray())
    }

    "return accounts that have just been inserted" in new WithApplication {
      val Some(result1) = route(FakeRequest(POST, "/api/accounts")
        .withJsonBody(defaultAccountJson)
        .withSession(session(emailUser)))
      val account1 = Json.parse(contentAsString(result1))

      val Some(result2) = route(FakeRequest(POST, "/api/accounts")
        .withJsonBody(defaultAccountJson)
        .withSession(session(emailUser)))
      val account2 = Json.parse(contentAsString(result2))

      val Some(result3) = route(FakeRequest(GET, "/api/accounts")
        .withSession(session(emailUser)))

      status(result3) must equalTo(OK)
      val listJsonAccounts = Json.parse(contentAsString(result3))
      listJsonAccounts.as[JsArray].value must contain(account1)
      listJsonAccounts.as[JsArray].value must contain(account2)
    }

//    "return a 403 error if the user has not the right MANAGE_ACCOUNTS" in new WithApplication {
//      Cache.set("User." + emailUser, User(emailUser, List(UserRight("OTHER_RIGHT"))))
//      val Some(result) = route(FakeRequest(GET, "/api/accounts")
//        .withJsonBody(defaultAccountJson)
//        .withSession(session(emailUser)))
//
//      status(result) must equalTo(FORBIDDEN)
//    }
  }

  "GET /accounts/:id" should {
    "return the jsonFormaters of the inserted account" in new WithApplication {
      val Some(result) = route(FakeRequest(POST, "/api/accounts")
        .withJsonBody(defaultAccountJson)
        .withSession(session(emailUser)))
      val json = Json.parse(contentAsString(result))
      val id = (__ \ "_id")(json).head.as[String]

      val Some(result2) = route(FakeRequest(GET, "/api/accounts/" + id)
        .withSession(session(emailUser)))
      status(result2) must equalTo(OK)

      val json2 = Json.parse(contentAsString(result2))
      json2 must equalTo(json)
    }

    "return a 404 error if the id is not a Mongo ObjectId" in new WithApplication{
      val Some(result) = route(FakeRequest(GET, "/api/accounts/azerty")
        .withSession(session(emailUser)))
      status(result) must beEqualTo(NOT_FOUND)
    }

    "return a 404 error if the id is not in the database" in new WithApplication{
      val Some(result) = route(FakeRequest(GET, "/api/accounts/51abb041ae01081d007afa11")
        .withSession(session(emailUser)))
      status(result) must beEqualTo(NOT_FOUND)
    }

//    "return a 403 error if the user has not the right MANAGE_ACCOUNTS" in new WithApplication {
//      Cache.set("User." + emailUser, User(emailUser, List(UserRight("MANAGE_ACCOUNTS"))))
//      val Some(result) = route(FakeRequest(POST, "/api/accounts")
//        .withJsonBody(defaultAccountJson)
//        .withSession(session(emailUser)))
//      val json = Json.parse(contentAsString(result))
//      val id = (__ \ "_id")(json).head.as[String]
//
//      val unauthorizedUser = "unauthorized@test.fr"
//      Cache.set("User." + unauthorizedUser, User(unauthorizedUser, List(UserRight("OTHER_RIGHT"))))
//      val Some(result2) = route(FakeRequest(GET, "/api/accounts/" + id)
//        .withSession(session(unauthorizedUser)))
//      status(result2) must equalTo(FORBIDDEN)
//    }
  }

  "PUT /accounts/:id" should {
    "update the fields" in new WithApplication {
      // First request to create an account
      val Some(result) = route(FakeRequest(POST, "/api/accounts")
        .withJsonBody(defaultAccountJson)
        .withSession(session(emailUser)))
      val json = Json.parse(contentAsString(result))
      val id = (__ \ "_id")(json).head.as[String]

      // Second request to update the account
      val newJson = defaultAccountJson.transform(
        __.json.update(
          (__ \ "name").json
            .put(JsString("Free Software Company"))) andThen
          __.json.update(
            (__ \ "contacts").json
              .put(JsArray(Seq(JsString("5"), JsString("6"))))
          )
      ).get

      val Some(result2) = route(FakeRequest(PUT, "/api/accounts/" + id)
        .withJsonBody(newJson)
        .withSession(session(emailUser)))
      val json2 = Json.parse(contentAsString(result2))
      status(result2) must equalTo(OK)

      // Third request to check that the account has been correctly updated
      val Some(result3) = route(FakeRequest(GET, "/api/accounts/" + id)
        .withSession(session(emailUser)))
      val json3 = Json.parse(contentAsString(result3))
      (__ \ "name")(json3) must contain(JsString("Free Software Company"))
      (__ \ "contacts")(json3) must contain(JsArray(Seq(JsString("5"), JsString("6"))))
      (__ \ "modifiedOn")(json3) must not equalTo (__ \ "createdOsn")(json3)
    }

//    "return a 403 error if the user has not the right MANAGE_ACCOUNTS" in new WithApplication {
//      // First request to create the account
//      setCache(emailUser, "MANAGE_ACCOUNTS")
//      val Some(result) = route(FakeRequest(POST, "/api/accounts")
//        .withJsonBody(defaultAccountJson)
//        .withSession(session(emailUser)))
//      val json = Json.parse(contentAsString(result))
//      val id = (__ \ "_id")(json).head.as[String]
//
//      // Second request to check the unauthorized user get a 403 error
//      val unauthorizedUser = "unauthorized@test.fr"
//      setCache(unauthorizedUser, "MANAGE_ACCOUNTS")
//      val Some(result2) = route(FakeRequest(PUT, "/api/accounts/" + id)
//        .withJsonBody(json)
//        .withSession(session(unauthorizedUser)))
//    }
  }
}
