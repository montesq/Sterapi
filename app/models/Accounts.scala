package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Accounts {
  val validateAccount: Reads[JsObject] = (
    (__ \ 'technical_name).json.pickBranch and
      (__ \ 'name).json.pickBranch
    ).reduce
}
