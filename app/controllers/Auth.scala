package controllers

import play.api.mvc._
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._

object Auth extends Controller {

  val authForm = Form(
      "assertion" -> nonEmptyText
  )

  def login = Action { implicit request =>
    authForm.bindFromRequest.fold(
      errors => BadRequest,
      assertion => Async {
        WS.url("https://verifier.login.persona.org/verify").post(
          Map(
            "assertion" -> Seq(assertion),
            "audience" -> Seq(request.host)
          )
        ).map { result =>
          System.out.println(result.body)
          val email = (__ \ "email")(result.json).head.as[String]
          if (result.status == OK) Ok(Json.obj("email" -> email)).withCookies(Cookie("email", email))
          else BadRequest
        }
      }
    )
  }
  def logout = Action {
    Ok
  }
}

