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

  def addFabClient(id: String) = Authenticated(Some("ACCOUNT_MANAGER")) { user => request =>
    Async {
      usersColl.update(Json.obj("email" -> id),
        Json.obj("$addToSet" -> Json.obj("profiles" -> "FABRICATION_CLIENT")),
        upsert = true).map { result =>
          Ok
      }.recover { case e =>
        InternalServerError(JsString("exception %s".format(e.getMessage)))
      }
    }
  }
}