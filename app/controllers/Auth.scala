package controllers

import play.api.mvc._
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.cache.Cache
import play.api.Play.current
import models.User


object Auth extends Controller {

  val authSessionAttribute = current.configuration.getString("auth.attribute").get
  val authVerifyURL = current.configuration.getString("auth.verifyURL").get

  def login = Action(parse.json) { (request) =>
    Async {
      WS.url(authVerifyURL).post(
        Map(
          "assertion" -> Seq((request.body \ "assertion").as[String]),
          "audience" -> Seq(request.host)
        )
      ).map { result =>
        if (result.status == OK) {
          val email = (result.json \ "email").as[String]
          Ok(Json.obj(authSessionAttribute -> email)).withSession((authSessionAttribute, email))
        }
        else BadRequest
      }
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
