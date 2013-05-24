package controllers

import play.api.mvc._
import play.api.libs.json._
import play.modules.reactivemongo._
import play.modules.reactivemongo.json.collection.JSONCollection
import models.Accounts._

object Accounts extends Controller with MongoController {

  val accountsColl = db.collection[JSONCollection]("accounts")

  def createAccount = Action(parse.json) {
    request =>
      request.body.transform(validateAccount).map {
        json =>
          Async {
            accountsColl.insert(json).map {
              lastError =>
                Created
            }.recover {
              case e =>
                BadRequest(JsString("exception %s".format(e.getMessage)))
            }
          }
      }.recoverTotal {
        err =>
          BadRequest(JsError.toFlatJson(err))
      }
  }

  def listAccounts = Action {
    Async {
      val accountsList = accountsColl.find(Json.obj()).cursor[JsObject].toList
      accountsList.map {
        list =>
          Ok(Json.arr(list))
      }.recover {
        case e =>
          InternalServerError(JsString("exception %s".format(e.getMessage)))
      }
    }
  }

  def getAccount(id: String) = Action {
    Async {
      accountsColl.find(Json.obj("technical_name" -> id)).one[JsObject].map {
        e =>
          e match {
            case Some(account) => Ok(account)
            case None => NoContent
          }
      }.recover {
        case e =>
          InternalServerError(JsString("exception %s".format(e.getMessage)))
      }
    }
  }

  def updateAccount(id: String) = TODO

}
