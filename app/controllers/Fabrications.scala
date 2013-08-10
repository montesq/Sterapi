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
import reactivemongo.core.commands.{Update, FindAndModify, GetLastError}
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import play.api.mvc._
import java.io.File
import jsonFormaters.FabricationFormaters._
import scala.concurrent.Future
import org.joda.time.DateTime

object Fabrications extends Controller with MongoController with DeadboltActions{
  val fabCollection = DBConnection.db.collection[JSONCollection]("fabrications")
  val accountCollection = DBConnection.db.collection[JSONCollection]("accounts")
  val seqCollection = DBConnection.db.collection[JSONCollection]("sequences")

  def generateFabId: Future[JsObject] = {
    val currentYear = ((new DateTime).getYear() % 100)
    val futureId: Future[Option[JsObject]] = seqCollection.find(Json.obj(
      "_id" -> "fabrications",
      "year" -> currentYear)).one[JsObject]

    futureId.map { result => {
      seqCollection.update(
        Json.obj(
        "_id" -> "fabrications",
        "year" -> currentYear),
        Json.obj("$inc" -> Json.obj("lastValue" -> 1)),
        upsert = true
      ).value
        result match {
          case None => Json.obj("_id" -> JsString(currentYear + "0001"))
          case Some(jsObj) => {
            val seqVal = (jsObj \ "lastValue").asOpt[Int].get
            Json.obj("_id" -> JsString(currentYear + "%04d".format(seqVal)))
          }
        }
      }
    }
  }


  def create = //Restrict(Array("MANAGE_FABRICATIONS"), new SecurityHandler) {
    CORSAction {
      Action(parse.json) { request =>
        request.body.transform(validateFabrication).map { jsObj =>
          Async {
            accountCollection.find(
              jsObj.transform((__ \ "_id" \ "$oid").json.copyFrom(( __ \ "client" \ "_id").json.pick)).get
            ).one[JsObject].map {
              case Some(clientObj) => Async {
                generateFabId.map { jsId =>
                  Async {
                    fabCollection.insert(
                      jsObj ++
                        jsId ++
                        Json.obj("client" -> clientObj.transform(outputAccount).get),
                      GetLastError(true)
                    ).map { lastError =>
                      if (lastError.ok)
                        Created
                      else InternalServerError("erreur")
                    }
                  }
                }
              }
              case None => BadRequest(Json.obj("err" -> "Incoherent Client ID"))
            }
          }
        }.recoverTotal { err =>
          BadRequest(JsError.toFlatJson(err))
        }
      }
    }
  //  }

  def list = //Restrict(Array("MANAGE_FABRICATIONS"), new SecurityHandler) {
    CORSAction {
      Action {
        Async {
          val fabFutureList = fabCollection.find(Json.obj())
            .sort(Json.obj("_id" -> 1))
            .cursor[JsObject]
            .toList
          fabFutureList.map { list =>
            val transformedList = for (fabrication <- list) yield fabrication
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
            fabCollection.find(Json.obj("_id" -> Json.obj("$oid" -> id))).one[JsObject].map {
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
      Action(parse.json) { request =>
        request.body.transform(validateFabrication).map { jsObj =>
          Async {
            accountCollection.find(
              jsObj.transform((__ \ "_id" \ "$oid").json.copyFrom(( __ \ "client" \ "_id").json.pick)).get
            ).one[JsObject].map {
              case Some(clientObj) => Async {
                val updateObj = (jsObj ++ Json.obj("client" -> clientObj.transform(outputAccount).get)).
                  transform(toUpdate).get
                fabCollection.update(
                  Json.obj("_id" -> Json.obj("$oid" -> id)),
                  updateObj
                ).map { lastError =>
                  if (lastError.ok)
                    Ok(updateObj)
                  else
                    InternalServerError(JsString("exception %s".format(lastError.errMsg)))
                }
              }
              case None => BadRequest(Json.obj("err" -> "Incoherent Client ID"))
            }
          }
        }.recoverTotal { err =>
          BadRequest(JsError.toFlatJson(err))
        }
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
