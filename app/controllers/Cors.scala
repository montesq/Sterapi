package controllers

import play.api.mvc._

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
