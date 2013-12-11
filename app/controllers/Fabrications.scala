package controllers

import play.api.Logger._
import play.api.libs.json._
import play.modules.reactivemongo.MongoController
import utils.DBConnection
import jsonFormaters.AccountsFormaters._
import jsonFormaters.CommonFormaters._
import reactivemongo.core.commands.GetLastError
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import reactivemongo.bson.BSONObjectID
import play.api.mvc._
import java.io.File
import jsonFormaters.FabricationFormaters._
import scala.concurrent.Future
import org.joda.time.DateTime
import actions.CORSAction
import actions.Security._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.GetLastError
import play.api.libs.json.JsString
import scala.Some
import play.api.libs.json.JsObject

object Fabrications extends Controller with MongoController {
  val fabCollection = DBConnection.db.collection[JSONCollection]("fabrications")
  val accountCollection = DBConnection.db.collection[JSONCollection]("accounts")
  val seqCollection = DBConnection.db.collection[JSONCollection]("sequences")

  def generateFabId: Future[JsObject] = {
    val currentYear = (new DateTime).getYear() % 100
    val futureId: Future[Option[JsObject]] = seqCollection.find(Json.obj(
      "_id" -> "fabrications",
      "year" -> currentYear)).one[JsObject]

    futureId.map {
      result => {
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


  def create = Authenticated(parse.json)(Some("WRITE_FABRICATION")) { user => request =>
    request.body.transform(validateFabrication).map { jsObj =>
      // verify whether the client exists
      val clientId = (jsObj \ "client" \ "_id").asOpt[String].getOrElse("")
      logger.debug("clientId: " + clientId)

      if (!BSONObjectID.parse(clientId).isSuccess)
        BadRequest(jsonError("INVALID_CLIENT_ID", "There doesn't exist any client with this _id"))
      else {
        Async {
          accountCollection.find(Json.obj("_id" -> Json.obj("$oid" -> clientId))).one[JsObject].map {
            case None => BadRequest(jsonError("INVALID_CLIENT_ID", "There doesn't exist any client with this _id"))
            case Some(clientObj) => Async {
              generateFabId.map { jsId =>
                Async {
                  val jsFab = jsObj ++
                    jsId ++
                    Json.obj("client" -> clientObj.transform(minimalOutputAccount).get)
                  fabCollection.insert(jsFab, GetLastError(true)
                  ).map { lastError =>
                    if (lastError.ok)
                      Created(jsFab)
                    else InternalServerError(jsonDatabaseError)
                  }
                }
              }
            }
          }
        }
      }
    }.recoverTotal { err =>
      BadRequest(jsonError("INVALID_JSON_INPUT", "The json input is invalid, cf details",
        JsError.toFlatJson(err)))
    }
  }

  def list = Authenticated(Some("READ_FABRICATION")) { user => request =>
    Async {
      val clientFilter = if (user.rights contains "ADMIN") Json.obj()
        else Json.obj("client._id" -> Json.obj("$in" -> user.clients))
      val fabFutureList = fabCollection.find(clientFilter)
      .sort(Json.obj("_id" -> 1))
      .cursor[JsObject]
      .toList(100)
      fabFutureList.map { list =>
        val transformedList = for (fabrication <- list) yield fabrication
        Ok(JsArray(transformedList))
      }.recover { case e =>
        InternalServerError(JsString("exception %s".format(e.getMessage)))
      }
    }
  }


  def getOne(id: String) = Authenticated(Some("READ_FABRICATION")) { user => request =>
    Async {
      fabCollection.find(Json.obj("_id" -> id)).one[JsObject].map {
        case Some(fabrication) => Ok(fabrication)
        case None => NotFound
      }.recover { case e =>
        InternalServerError(JsString("exception %s".format(e.getMessage)))
      }
    }
  }

  def updateOne(id: String) = Authenticated(parse.json)(Some("WRITE_FABRICATION")) { user => request =>
    request.body.transform(validateFabrication).map { jsObj =>
      Async {
        accountCollection.find(
          jsObj.transform((__ \ "_id" \ "$oid").json.copyFrom(( __ \ "client" \ "_id").json.pick)).get
        ).one[JsObject].map {
          case Some(clientObj) => Async {
            val updateObj = (jsObj ++ Json.obj("client" -> clientObj.transform(minimalOutputAccount).get)).
              transform(toUpdate).get
            fabCollection.update(
              Json.obj("_id" -> id),
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

  def saveAttachment(idFab: String) = Authenticated(parse.temporaryFile)(Some("WRITE_FABRICATION")) { user => request =>
    request.getQueryString("name") match {
      case Some(s) => {
        request.body.moveTo(new File("uploadedFiles/fabrications/" + idFab + "/" + s),true)
        Ok("File uploaded")
      }
      case _ => BadRequest
    }
  }

  def getAttachment(idFab: String, idAtt: String) = Authenticated(Some("READ_FABRICATION")) { user => request =>
    Ok.sendFile(new File("uploadedFiles/fabrications/" + idFab + "/" + idAtt))
  }
}
