package actions

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DBConnection
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.MongoController


case class AuthenticatedUser[A](action: Action[A]) extends Action[A]{

  val usersColl = DBConnection.db.collection[JSONCollection]("users")

  def apply(request: Request[A]): Result = {
    System.out.println(request.session)
    System.out.println(request.session.get("email"))
    request.session.get("email") match {
      case None => Forbidden(Json.obj("Error" -> "You're not authenticated"))
      case Some(value) => {
        Async{
          usersColl.find(Json.obj("email" -> value)).one[JsObject].map {
            case None => Forbidden(Json.obj("Error" -> "The user is not present in the database"))
            case Some(json) => action(request)
          }
        }
      }
    }
  }

  lazy val parser = action.parser
}