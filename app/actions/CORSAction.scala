package actions

import play.api.mvc._

case class CORSAction[A](action: Action[A]) extends Action[A]{
  def apply(request: Request[A]): Result = {
    action(request).withHeaders("Access-Control-Allow-Origin" -> "*")
  }

  lazy val parser = action.parser
}