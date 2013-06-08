package controllers

import play.api.mvc._
import play.api.libs.json._
import reactivemongo.api.indexes._
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.collection.JSONCollection
import models.Accounts._
import models.Common._
import utils.DBConnection
import scala.concurrent.ExecutionContext.Implicits.global


object Accounts extends Controller {

  val accountsColl = DBConnection.db.collection[JSONCollection]("accounts")

  accountsColl.indexesManager.ensure(
    Index(Seq("status" -> IndexType.Ascending), None))

  def createAccount = Action(parse.json) { request =>
    request.body.transform(
      validateAccount andThen
        addId andThen
        addStatus(activeStatus) andThen
        addTrailingDates)
      .map { json =>
        Async {
          accountsColl.insert(json).map { lastError =>
            Created(json.transform(outputAccount).get)
          }
        }
    }.recoverTotal { err =>
      BadRequest(JsError.toFlatJson(err))
    }
  }

  def listAccounts = Action {
    Async {
      val accountsFutureList = accountsColl.find(Json.obj("status" -> activeStatus))
        .sort(Json.obj("_id" -> 1))
        .cursor[JsObject]
        .toList
      accountsFutureList.map { list =>
        val transformedList = for (account <- list) yield account.transform(outputAccount).get
        Ok(JsArray(transformedList))
      }.recover { case e =>
        InternalServerError(JsString("exception %s".format(e.getMessage)))
      }
    }
  }

  def getAccount(id: String) = Action {
      if (BSONObjectID.parse(id).isSuccess) {
        Async {
          accountsColl.find(Json.obj("_id" -> Json.obj("$oid" -> id))).one[JsObject].map {
            case Some(account) => Ok(account.transform(outputAccount).get)
            case None => NotFound
          }.recover { case e =>
            InternalServerError(JsString("exception %s".format(e.getMessage)))
          }
        }
      } else NotFound
  }

  def updateAccount(id: String) = Action(parse.json) { request =>
    request.body.transform(validateAccount).flatMap { jsobj =>
      jsobj.transform(toUpdate).map { updateSelector =>
        Async {
          accountsColl.update(
            Json.obj("_id" -> Json.obj("$oid" -> id)),
            updateSelector
          ).map { lastError =>
            if (lastError.ok)
              Ok(updateSelector)
            else
              InternalServerError(JsString("exception %s".format(lastError.errMsg)))
          }
        }
      }
    }.recoverTotal { e =>
      BadRequest(JsError.toFlatJson(e))
    }
  }
}