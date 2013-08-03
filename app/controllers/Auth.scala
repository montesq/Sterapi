package controllers

import play.api.mvc._
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.data._
import play.api.libs.json._
import play.api.cache.Cache
import play.api.Play.current
import utils.CORSAction

object Auth extends Controller {

  val authSessionAttribute = current.configuration.getString("auth.attribute").get
  val authVerifyURL = current.configuration.getString("auth.verifyURL").get

  def login = CORSAction { request =>
    request.body.asJson.map { json =>
      json.transform((__ \ "assertion").json.pickBranch).map { assertion =>
        Async {
          val postBody = Map(
            "assertion" -> Seq((assertion \ "assertion").as[String]),
            "audience" -> Seq(request.headers.get("Origin").getOrElse(""))
          )
          System.out.println(postBody)
          WS.url(authVerifyURL).post(postBody).map { result =>
            if (result.status == OK) {
              val status = (result.json \ "status").as[String]
              if (status == "okay") {
                val email = (result.json \ "email").as[String]
                Ok(Json.obj(authSessionAttribute -> email)).withSession((authSessionAttribute, email))
              }
              else BadRequest("Invalid assertion")
            }
            else InternalServerError("Impossible to check the assertion")
          }
        }
      }.getOrElse(BadRequest("Expecting JSON data"))
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
