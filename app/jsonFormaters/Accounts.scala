package jsonFormaters

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import jsonFormaters.Common._

object Accounts {
  val activeStatus = "ACTIVE"
  val inactiveStatus = "INACTIVE"

  def validateAccount: Reads[JsObject] = (
    ((__ \ "name").json.pickBranch) and
      ((__ \ "status").json.pickBranch or emptyObj) and
      ((__ \ "contacts").json.copyFrom(idsOrEmptyArray((__ \ "contacts"))))
    ).reduce

  val outputAccount =
    removeOid andThen
      convertDate((__ \ "created_on")) andThen
      convertDate((__ \ "modified_on"))
}
