package controllers

import play.api.mvc._
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.data._
import play.api.libs.json._
import play.api.cache.Cache
import play.api.Play.current
import utils.DBConnection
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import actions.CORSAction
import play.api.libs.Crypto._
import java.lang.System
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTime

object Auth extends Controller with MongoController {

  val usersColl = DBConnection.db.collection[JSONCollection]("users")
  val authVerifyURL = current.configuration.getString("auth.verifyURL").get
  val authRequestHeader = current.configuration.getString("auth.requestHeader").get
  val authExpiration = current.configuration.getString("auth.expiration").get

  def login = CORSAction {
    Action (parse.json) {  json =>
      json.body.transform((__ \ "assertion").json.pickBranch).map { assertion =>
        Async {
          val postBody = Map(
            "assertion" -> Seq((assertion \ "assertion").as[String]),
            "audience" -> Seq(json.headers.get("Origin").getOrElse("http://localhost:9000"))
          )
          WS.url(authVerifyURL).post(postBody).map { result =>
            if (result.status == OK) {
              val status = (result.json \ "status").as[String]
              if (status == "okay") {
                val email = (result.json \ "email").as[String]
                Async{
                  usersColl.find(Json.obj("email" -> email)).one[JsObject].map {
                    case Some(user) => {
                      Created(Json.obj("email" -> email, authRequestHeader -> generateToken(email)))
                    }
                    case None => BadRequest("This user doesn't exist")
                  }.recover { case e =>
                    InternalServerError(JsString("exception %s".format(e.getMessage)))
                  }
                }
              }
              else BadRequest("Invalid assertion")
            }
            else InternalServerError("Impossible to check the assertion")
          }
        }
      }.getOrElse(BadRequest("Expecting JSON data"))
    }
  }

  def logout = CORSAction {
    Action { request =>
      request.headers.get("authRequestHeader") match {
        case Some(s) => Cache.remove(authRequestHeader + "." + s)
        case _ => ()
      }
      Ok
    }
  }

  def generateToken(email: String) : String = {
    val currentTimeStamp = System.currentTimeMillis()
    val endTimeStamp = currentTimeStamp + authExpiration.toLong * 60 * 1000
    val dateFormatter = ISODateTimeFormat.dateTime()
    val endDate = dateFormatter.print(new DateTime(endTimeStamp))
    encryptAES(Json.obj("email" -> email, "endDate" -> endDate).toString)
  }
}
