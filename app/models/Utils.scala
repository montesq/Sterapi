package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._


object Utils {
  def addStatus(status: String) = {
    __.json.update(
      (__ \ "status").json.put(JsString(status))
    )
  }

  def addTrailingDates =
    __.json.update((
      (__ \ "created_on" \ "$date").json.put(JsNumber(System.currentTimeMillis())) and
        (__ \ "modified_on" \ "$date").json.put(JsNumber(System.currentTimeMillis()))
      ).reduce
    )

  def idsOrEmptyArray(field: String) =
    ((__ \ field).json.pick[JsArray] orElse Reads.pure(Json.arr())) andThen
      validateIdsList

  def validateIdsList =
    Reads.verifyingIf((arr: JsArray) => !arr.value.isEmpty)(Reads.list[JsNumber])

  def toUpdate = (__ \ "$set").json.copyFrom(__.json.pick) andThen updateModifiedDate

  def updateModifiedDate = {
    __.json.update(
      (__ \ "$set" \ "modified_on" \ "$date").json.put(JsNumber(System.currentTimeMillis()))
    )
  }

  def convertDate(field: String) = {
    //    val tsToISO = (__ \ field).json.pickBranch.
    __.json.update((__ \ field).json.copyFrom((__ \ field \ "$date").json.pick))
  }
}
