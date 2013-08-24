package controllers

import play.api.mvc._
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.cache.Cache
import play.api.Play.current


object Cors extends Controller {

  def option(url: String) = Action { request =>
    Ok.withHeaders(
      "Access-Control-Allow-Origin" -> request.headers.get("Origin").getOrElse(""),
      "Access-Control-Allow-Credentials" -> "true",
      "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
      "Access-Control-Allow-Headers" -> "Content-Type, X-Requested-With, Accept ",
      "Access-Control-Max-Age" -> (60 * 60 * 24).toString
    )
  }
}
