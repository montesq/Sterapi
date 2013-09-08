package actions

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DBConnection
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.mvc.BodyParsers._
import scala.concurrent.Await
import scala.concurrent.duration._

object Security {

  val usersColl = DBConnection.db.collection[JSONCollection]("users")
  val accountsColl = DBConnection.db.collection[JSONCollection]("accounts")

  case class User(email: String, rights: List[String], clients: List[String])

  def Authenticated[A](p: BodyParser[A])(f: User => Request[A] => Result) = {
    CORSAction {
      Action(p) { request =>
        val result = for {
          id <- request.session.get("email")
          user <- Await.result(usersColl.find(Json.obj("email" -> id)).one[JsObject], Duration(10, SECONDS))
          email <- (user \ "email").asOpt[String]
          profiles <- (user \ "profiles").asOpt[List[String]]
        } yield f(User(
            email,
            convertProfilesToRights(profiles),
            List()
          ))(request)
        result getOrElse Unauthorized
      }
    }
  }

  // Overloaded method to use the default body parser
  def Authenticated(f: User => Request[AnyContent] => Result): Action[AnyContent]  = {
    Authenticated(parse.anyContent)(f)
  }

  def convertProfilesToRights(profiles: List[String]) : List[String] = {
    profiles match {
      case Nil => Nil
      case t::q => t :: convertProfilesToRights(profilesMatrix.getOrElse(t, Nil) ::: q)
    }
  }

  val profilesMatrix: Map[String, List[String]] = Map(
    "ADMIN" -> List("STERILIZATION_MANAGER", "ACCOUNT_MANAGER", "FABRICATION_MANAGER"),
    "STERILIZATION_MANAGER" -> List("READ_STERILIZATION", "WRITE_STERILIZATION"),
    "FABRICATION_MANAGER" -> List("READ_FABRICATION", "WRITE_FABRICATION"),
    "ACCOUNT_MANAGER" -> List("READ_ACCOUNT", "WRITE_ACCOUNT"),
    "STERILIZATION_CLIENT" -> List("READ_STERILIZATION")
  )
}
