package utils

import play.api.mvc._

object CORSAction {

  type ResultWithHeaders = Result {def withHeaders(headers: (String, String)*): Result}

  def apply(block: Request[AnyContent] => ResultWithHeaders): Action[AnyContent] = {
    Action {
      request =>
        block(request).withHeaders("Access-Control-Allow-Origin" -> request.headers.get("Origin").getOrElse(""))
    }
  }

  def apply(block: => ResultWithHeaders): Action[AnyContent] = {
    this.apply(_ => block)
  }

}