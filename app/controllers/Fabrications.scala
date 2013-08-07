package controllers

import play.api.libs.json._
import play.autosource.reactivemongo.ReactiveMongoAutoSourceController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import be.objectify.deadbolt.scala.DeadboltActions
import utils.{CORSAction, DBConnection}
import jsonFormaters.AccountsFormaters._
import play.modules.reactivemongo.json.collection.JSONCollection
import jsonFormaters.CommonFormaters._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.GetLastError
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.GetLastError
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import reactivemongo.bson.BSONObjectID
import play.api.mvc._
import java.io.File

object Fabrications extends Controller with MongoController with DeadboltActions{
  val coll = DBConnection.db.collection[JSONCollection]("fabrications")

  def create = //Restrict(Array("MANAGE_FABRICATIONS"), new SecurityHandler) {
    CORSAction {
      Action(parse.json) { json =>
        Async {
          coll.insert(json.body, GetLastError(true)).map { lastError =>
            if (lastError.ok)
              Created
            else InternalServerError(JsString("exception %s".format(lastError.errMsg)))
          }
        }
      }
    }
  //  }

  def list = //Restrict(Array("MANAGE_FABRICATIONS"), new SecurityHandler) {
    CORSAction {
      Action {
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
    }
  //  }


  def getOne(id: String) = //Restrict(Array("MANAGE_FABRICATIONS"), new SecurityHandler) {
    CORSAction {
      Action {
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
    }
//  }

  def updateOne(id: String) = //Restrict(Array("MANAGE_FABRICATIONS"), new SecurityHandler) {
    CORSAction {
      Action(parse.json) { json =>
        json.body.transform(toUpdate).map { updateSelector =>
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
      }
    }
  //  }

  def saveAttachment(idFab: String) = CORSAction {
    Action(parse.temporaryFile) { request =>
      request.getQueryString("name") match {
        case Some(s) => {
          request.body.moveTo(new File("uploadedFiles/fabrications/" + idFab + "/" + s),true)
          Ok("File uploaded")
        }
        case _ => BadRequest
      }
    }
  }

  def getAttachment(idFab: String, idAtt: String) = CORSAction {
    Action {
      Ok.sendFile(new File("uploadedFiles/fabrications/" + idFab + "/" + idAtt))
    }
  }
}
