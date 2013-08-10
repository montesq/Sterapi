package jsonFormaters

import play.api.libs.json._
import play.api.libs.json.Reads._
import jsonFormaters.CommonFormaters._
import play.api.libs.functional.syntax._


object FabricationFormaters {


  val validateFabrication : Reads[JsObject] = (
    //TODO return an error if the date format is not good instead of removing the date
    ( __ \ "client" \ "_id").json.pickBranch and
      (( __ \ "clientOrderId").json.pickBranch or emptyObj) and
      (( __ \ "clientInfo").json.pickBranch or emptyObj) and
      (( __ \ "fabStartDate").json.pickBranch(Reads.of[JsString]
        keepAnd Reads.DefaultDateReads) or emptyObj) and
      (( __ \ "fabEndDate").json.pickBranch(Reads.of[JsString]
          keepAnd Reads.DefaultDateReads) or emptyObj) and
      (( __ \ "steDate").json.pickBranch(Reads.of[JsString]
          keepAnd Reads.DefaultDateReads) or emptyObj) and
      (( __ \ "attachment").json.pickBranch or emptyObj)
    ).reduce
}
