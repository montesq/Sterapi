package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import models.Utils._

object Accounts {
  val activeStatus = "ACTIVE"
  val inactiveStatus = "INACTIVE"

  def validateAccount: Reads[JsObject] = (
    (__ \ "id").json.pickBranch and
      (__ \ "name").json.pickBranch and
      (__ \ "contacts").json.copyFrom(idsOrEmptyArray("contacts"))
    ).reduce

  def outputAccount =
    convertDate("created_on") andThen
      convertDate("modified_on")
}
