package controllers

import play.api.mvc._
import play.api.libs.json._
import reactivemongo.api.indexes._
import reactivemongo.bson.BSONObjectID
import play.modules.reactivemongo.json.collection.JSONCollection
import models.Accounts._
import models.Common._
import utils.{CORSAction, DBConnection}
import scala.concurrent.ExecutionContext.Implicits.global
import security.SecurityHandler
import be.objectify.deadbolt.core.PatternType
import be.objectify.deadbolt.scala.DeadboltActions
import reactivemongo.core.commands.{LastError, GetLastError}
import play.modules.reactivemongo.MongoController


object Accounts extends Controller with MongoController with DeadboltActions {

  val accountsColl = DBConnection.db.collection[JSONCollection]("accounts")

  accountsColl.indexesManager.ensure(
    Index(Seq("status" -> IndexType.Ascending), None))

  def createAccount = Restrict(Array("MANAGE_ACCOUNTS"), new SecurityHandler) {
    Action(parse.json) { request =>
      request.body.transform(
        validateAccount andThen
          addId andThen
          addStatus(activeStatus) andThen
          addTrailingDates)
        .map { json =>
        Async {
          accountsColl.insert(json, GetLastError(true)).map { lastError =>
            if (lastError.ok)
              Created(json.transform(outputAccount).get)
            else InternalServerError(JsString("exception %s".format(lastError.errMsg)))
          }
        }
      }.recoverTotal { err =>
        BadRequest(JsError.toFlatJson(err))
      }
    }
  }

  def listAccounts = Restrict(Array("MANAGE_ACCOUNTS"), new SecurityHandler) {
    CORSAction {
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
  }

  def getAccount(id: String) = Restrict(Array("MANAGE_ACCOUNTS"), new SecurityHandler) {
    CORSAction {
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
  }

  def updateAccount(id: String) = Restrict(Array("MANAGE_ACCOUNTS"), new SecurityHandler) {
    Action(parse.json) { request =>
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
}