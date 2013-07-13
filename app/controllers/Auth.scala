package controllers

import play.api.mvc._
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.cache.Cache
import play.api.Play.current
import utils.CORSAction


object Auth extends Controller {

  val authSessionAttribute = current.configuration.getString("auth.attribute").get
  val authVerifyURL = current.configuration.getString("auth.verifyURL").get

  def login = Action { request =>
    request.body.asJson.map { json =>
      json.transform((__ \ "assertion").json.pickBranch).map { json =>
        Async {
          WS.url(authVerifyURL).post(
            Map(
              "assertion" -> Seq((json \ "assertion").as[String]),
              "audience" -> Seq(request.headers.get("Origin").getOrElse(""))
            )
          ).map { result =>
            if (result.status == OK) {
              if ((result.json \ "status").as[String] == "okay") {
                val email = (result.json \ "email").as[String]
                // TODO check the email match an application user
                Ok(Json.obj(authSessionAttribute -> email)).
                  withSession((authSessionAttribute, email)).
                  withHeaders("Access-Control-Allow-Origin" -> request.headers.get("Origin").getOrElse("")
                )
              }
              else Forbidden
            }
            else InternalServerError
          }
        }
      }.recoverTotal { err =>
        BadRequest(JsError.toFlatJson(err))
      }
    }.getOrElse(BadRequest("Expecting JSON data"))
  }

  def logout = CORSAction { request =>
    request.session.get(authSessionAttribute) match {
      case Some(s) => Cache.remove("User." + s)
      case _ => ()
    }
    Ok.withNewSession
  }
}
