package actions

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DBConnection
import play.modules.reactivemongo.json.collection.JSONCollection

object Security {

  val usersColl = DBConnection.db.collection[JSONCollection]("users")

  case class UserHasRight[A](right: String = "")(action: Action[A]) extends Action[A]{

  def apply(request: Request[A]): Result = {
    request.session.get("email") match {
      case None => Forbidden(Json.obj("Error" -> "You're not authenticated"))
      case Some(value) => {
        if (right.isEmpty) action(request)
        else {
          Async{
            usersColl.find(Json.obj("email" -> value)).one[JsObject].map {
              case None => Forbidden(Json.obj("Error" -> "The user is not present in the database"))
              case Some(json) => {
                val profiles = (json \ "profiles").asOpt[List[String]].getOrElse(Nil)
                if (convertProfilesToRights(profiles).exists(_ == right))
                  action(request)
                else Forbidden("The user has not the right " + right)
              }
            }
          }
        }
      }
    }
  }

  lazy val parser = action.parser

  def convertProfilesToRights(profiles: List[String]) : List[String] = {
    profiles match {
      case Nil => Nil
      case t::q => t :: convertProfilesToRights(profilesMatrix.getOrElse(t, Nil) ::: q)
    }
  }

  val profilesMatrix: Map[String, List[String]] = Map(
    "ADMIN" -> List("STERILIZATION_MANAGER", "ACCOUNT_MANAGER"),
    "STERILIZATION_MANAGER" -> List("READ_STERILIZATION", "WRITE_STERILIZATION"),
    "ACCOUNT_MANAGER" -> List("READ_ACCOUNT", "WRITE_ACCOUNT"),
    "STERILIZATION_CLIENT" -> List("READ_STERILIZATION")
  )
}
}
