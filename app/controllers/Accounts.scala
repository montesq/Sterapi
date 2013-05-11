package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.iteratee._
import scala.concurrent.{ ExecutionContext, Future }
import play.modules.reactivemongo._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import models.Accounts._

object Accounts extends Controller with MongoController{

  val collection = db.collection[JSONCollection]("accounts")

  def createAccount = Action(parse.json) { request =>
    request.body.transform(validateAccount).map{ json =>
      Async{
        collection.insert(json).map{lastError =>
          Created
        }.recover{ case e =>
          BadRequest(JsString("exception %s".format(e.getMessage)))
        }
      }
    }.recoverTotal{ err =>
      BadRequest(JsError.toFlatJson(err))
    }
  }

  def listAccounts = TODO

  def getAccount(id: String) = TODO

  def updateAccount(id: String) = TODO

  def getAccountContacts(id: String) = TODO

  def addAccountContact(idAccount: String, idContact: String) = TODO

  def removeAccountContact(idAccount: String, idContact: String) = TODO

}