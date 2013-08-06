package jsonFormaters

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import reactivemongo.bson.BSONObjectID
import org.joda.time._
import org.joda.time.format.ISODateTimeFormat


object Common {
  val emptyObj = __.json.put(Json.obj())

  val addId = __.json.update(
    (__ \ "_id" \ "$oid").json.put(JsString(BSONObjectID.generate.stringify))
  )

  val removeOid = __.json.update((__ \ "_id").json.copyFrom((__ \ "_id" \ "$oid").json.pick[JsString]))

  def addStatus(status: String) = {
    __.json.update(
      (__ \ "status").json.put(JsString(status))
    )
  }

  def addTrailingDates = {
    val ts = JsNumber(System.currentTimeMillis())
    __.json.update((
      (__ \ "createdOn" \ "$date").json.put(ts) and
        (__ \ "modifiedOn" \ "$date").json.put(ts)
      ).reduce
    )
  }

  def idsOrEmptyArray(path: JsPath) =
    (path.json.pick[JsArray] orElse Reads.pure(Json.arr()))


  def toUpdate = (__ \ "_id").json.prune andThen
    (__ \ "$set").json.copyFrom(__.json.pick[JsValue]) andThen updateModifiedDate

  def updateModifiedDate = {
    __.json.update(
      (__ \ "$set" \ "modifiedOn" \ "$date").json.put(JsNumber(System.currentTimeMillis()))
    )
  }

  def convertDate(path: JsPath) = {
    val dateFormatter = ISODateTimeFormat.dateTime()

    (path \ "$date").json.update(
      of[JsNumber].map {
        case JsNumber(n) =>
          JsString(dateFormatter.print(new DateTime(n.toLong)))
      }
    ) andThen
      __.json.update(
        path.json.copyFrom((path \ "$date").json.pick[JsString])
      )
  }
}
