package controllers

import play.api.mvc.{Cookie, Controller, Action}
import play.api.libs.Crypto._
import be.objectify.deadbolt.scala.DeadboltActions
import security.SecurityHandler

object test extends Controller with DeadboltActions {
  def test = Restrict(Array("foo"), new SecurityHandler) {
    Action {
      Ok
    }
  }
}
