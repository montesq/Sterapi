package controllers

import play.api.mvc._
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.cache.Cache
import play.api.Play.current


object Auth extends Controller {

  val authSessionAttribute = current.configuration.getString("auth.attribute").get
  val authVerifyURL = current.configuration.getString("auth.verifyURL").get

  def login = Action(parse.json) { request =>
    request.body.transform((__ \ "assertion").json.pickBranch).map { json =>
      Async {
        WS.url(authVerifyURL).post(
          Map(
            "assertion" -> Seq((request.body \ "assertion").as[String]),
            "audience" -> Seq(request.host)
          )
        ).map { result =>
          if (result.status == OK) {
            if ((result.json \ "status").as[String] == "okay") {
              val email = (result.json \ "email").as[String]
              Ok(Json.obj(authSessionAttribute -> email)).withSession((authSessionAttribute, email))
            }
            else Forbidden
          }
          else InternalServerError
        }
      }
    }.recoverTotal { err =>
      BadRequest(JsError.toFlatJson(err))
    }
  }

  def logout = Action { request =>
    request.session.get(authSessionAttribute) match {
      case Some(s) => Cache.remove("User." + s)
      case _ => ()
    }
    Ok.withNewSession
  }
}
