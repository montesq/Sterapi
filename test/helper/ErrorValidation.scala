package helper

import play.api.libs.json._

object ErrorValidation {
  val missingPath = Json.arr(
    Json.obj(
      "msg" -> "validate.error.missing-path",
      "args" -> JsArray()
    )
  )
}
