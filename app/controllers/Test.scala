package controllers

import play.api.mvc._
import actions.CORSAction

object Test extends Controller {
  def test = CORSAction {
    Action {
      Ok.withSession("test"->"test")
    }
  }

  def test2 = CORSAction {
    Action {request =>
      Ok(request.session.get("test").getOrElse("Ko"))
    }
  }
}
