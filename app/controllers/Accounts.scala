package controllers

import play.api.mvc._
import play.api.libs.json._
import play.modules.reactivemongo._
import play.modules.reactivemongo.json.collection.JSONCollection
import models.Accounts._
import models.Utils._

object Accounts extends Controller with MongoController {

  val accountsColl = db.collection[JSONCollection]("accounts")

  def createAccount = Action(parse.json) {
    request =>
      request.body.transform(validateAccount andThen
        addStatus(activeStatus) andThen
        addTrailingDates).map {
        json =>
          Async {
            accountsColl.insert(json).map {
              lastError =>
                Created(json.transform(outputAccount).get)
            }
          }
      }.recoverTotal {
        err =>
          BadRequest(JsError.toFlatJson(err))
      }
  }

  def listAccounts = Action {
    Async {
      val accountsList = accountsColl.find(Json.obj())
        .sort(Json.obj("id" -> 1,
        "status" -> activeStatus))
        .cursor[JsObject]
        .toList
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
      accountsColl.find(Json.obj("id" -> id)).one[JsObject].map {
        mayAccount =>
          mayAccount match {
            case Some(account) => Ok(account)
            case None => NoContent
          }
      }.recover {
        case e =>
          InternalServerError(JsString("exception %s".format(e.getMessage)))
      }
    }
  }

  def updateAccount(id: String) = Action(parse.json) {
    request =>
      request.body.transform(validateAccount).flatMap {
        jsobj =>
          jsobj.transform(toUpdate).map {
            updateSelector =>
              Async {
                accountsColl.update(
                  Json.obj("id" -> id),
                  updateSelector
                ).map {
                  lastError =>
                    if (lastError.ok)
                      Ok(updateSelector)
                    else
                      InternalServerError(JsString("exception %s".format(lastError.errMsg)))
                }
              }
          }
      }.recoverTotal {
        e =>
          BadRequest(JsError.toFlatJson(e))
      }
  }

}


//import org.joda.time._
//import org.joda.time.format.ISODateTimeFormat
//import java.lang.Long
//val date = new DateTime(new Long("1369499656000"))
//val dateFormatter = ISODateTimeFormat.dateTime()
//Ok(dateFormatter.print(date))
