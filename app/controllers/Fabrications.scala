package controllers

import play.api.libs.json._
import play.autosource.reactivemongo.ReactiveMongoAutoSourceController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import be.objectify.deadbolt.scala.DeadboltActions
import utils.{CORSAction, DBConnection}
import jsonFormaters.Accounts._
import play.modules.reactivemongo.json.collection.JSONCollection
import jsonFormaters.Common._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.GetLastError
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.GetLastError
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import reactivemongo.bson.BSONObjectID

object Fabrications extends Controller with MongoController with DeadboltActions{
  val coll = DBConnection.db.collection[JSONCollection]("fabrications")

  def create = //Restrict(Array("MANAGE_FABRICATIONS"), new SecurityHandler) {
    CORSAction{ request =>
      request.body.asJson.map { json =>
        Async {
          coll.insert(json, GetLastError(true)).map { lastError =>
            if (lastError.ok)
              Created
            else InternalServerError(JsString("exception %s".format(lastError.errMsg)))
          }
        }
      }.getOrElse(BadRequest)
    }
  //  }

  def list = //Restrict(Array("MANAGE_FABRICATIONS"), new SecurityHandler) {
    CORSAction {
      Async {
        val fabFutureList = coll.find(Json.obj())
          .sort(Json.obj("_id" -> 1))
          .cursor[JsObject]
          .toList
        fabFutureList.map { list =>
          val transformedList = for (fabrication <- list) yield fabrication.transform(removeOid).get
          Ok(JsArray(transformedList))
        }.recover { case e =>
          InternalServerError(JsString("exception %s".format(e.getMessage)))
        }
      }
    }
  //  }


  def getOne(id: String) = //Restrict(Array("MANAGE_FABRICATIONS"), new SecurityHandler) {
    CORSAction {
      if (BSONObjectID.parse(id).isSuccess) {
        Async {
          coll.find(Json.obj("_id" -> Json.obj("$oid" -> id))).one[JsObject].map {
            case Some(fabrication) => Ok(fabrication.transform(removeOid).get)
            case None => NotFound
          }.recover { case e =>
            InternalServerError(JsString("exception %s".format(e.getMessage)))
          }
        }
      } else NotFound
    }
//  }

  def updateOne(id: String) = //Restrict(Array("MANAGE_FABRICATIONS"), new SecurityHandler) {
    CORSAction { request =>
      request.body.asJson.map { jsobj =>
          jsobj.transform(toUpdate).map { updateSelector =>
            Async {
              coll.update(
                Json.obj("_id" -> Json.obj("$oid" -> id)),
                updateSelector
              ).map { lastError =>
                if (lastError.ok)
                  Ok(updateSelector)
                else
                  InternalServerError(JsString("exception %s".format(lastError.errMsg)))
              }
            }
          }.getOrElse(InternalServerError)
      }.getOrElse(InternalServerError)
    }
  //  }

}
