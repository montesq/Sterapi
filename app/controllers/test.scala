package controllers

import play.api.mvc.{Cookie, Controller, Action}
import play.api.libs.Crypto._

object test extends Controller {
  def test = Action { request =>
    Ok(Some("Blabla").toString).withCookies(new Cookie("CookieName", encryptAES("CookieValue")))
  }
}
