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

  def login = Action { implicit request =>
    Async {
      WS.url(authVerifyURL).post(
        Map(
          "assertion" -> request.body.asFormUrlEncoded.getOrElse("assertion",""),
          "audience" -> Seq(request.host)
        )
      ).map { result =>
        val email = (__ \ "email")(result.json).head.as[String]
        if (result.status == OK)
          Ok(Json.obj(authSessionAttribute -> email)).withSession((authSessionAttribute, email))
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
