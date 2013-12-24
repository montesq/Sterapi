package controllers

import play.api.mvc._
import play.api.libs.json._
import utils.DBConnection
import reactivemongo.api.indexes._
import play.modules.reactivemongo.MongoController
import actions.Security._
import play.modules.reactivemongo.json.collection.JSONCollection

object Users extends Controller with MongoController{

  val usersColl = DBConnection.db.collection[JSONCollection]("users")

//  def getUser = {}

  def addProfile(email: String, idAccount: String, idProfile: String) =
    Authenticated(Some("ACCOUNT_MANAGER")) { user => request =>
    Async {
      usersColl.update(Json.obj("email" -> email),
        Json.obj("$addToSet" -> Json.obj("profiles" -> Json.obj("account" -> idAccount, "role" -> idProfile))),
        upsert = true).map { result =>
          NoContent
      }.recover { case e =>
        InternalServerError(JsString("exception %s".format(e.getMessage)))
      }
    }
  }

  def delProfile(email: String, idAccount: String, idProfile: String) =
    Authenticated(Some("ACCOUNT_MANAGER")) { user => request =>
      Async {
        usersColl.update(Json.obj("email" -> email),
          Json.obj("$pull" -> Json.obj("profiles" -> Json.obj("account" -> idAccount, "role" -> idProfile))),
          upsert = true).map { result =>
          NoContent
        }.recover { case e =>
          InternalServerError(JsString("exception %s".format(e.getMessage)))
        }
      }
    }
}