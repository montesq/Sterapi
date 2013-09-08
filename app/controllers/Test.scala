package controllers

import play.api.mvc._
import actions.CORSAction
import utils.DBConnection
import play.modules.reactivemongo.json.collection.JSONCollection

object Test extends Controller {
  def test = CORSAction {
    Action { request =>
      System.out.println(request.headers.get("Origin").getOrElse("NO-ORIGIN"))
      Ok
    }
  }

}
