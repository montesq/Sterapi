package actions

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DBConnection
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.mvc.BodyParsers._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.api.Cursor
import play.Logger

object Security {

  val usersColl = DBConnection.db.collection[JSONCollection]("users")
  val accountsColl = DBConnection.db.collection[JSONCollection]("accounts")

  case class User(email: String, rights: List[String] = List(), clients: List[String] = List())

  def Authenticated[A](p: BodyParser[A])(requiredRight: Option[String])(f: User => Request[A] => Result) = {
    CORSAction {
      Action(p) { request =>
        request.session.get("email") match {
          case None => Unauthorized(Json.obj("error" -> "The user is not authenticated"))
          case Some(email) => Async {
            val futureUser = usersColl.find(Json.obj("email" -> email)).one[JsObject]
            futureUser map { mayUser =>
              mayUser match {
                case None => Unauthorized(Json.obj("error" -> "This user doesn't exist in the database"))
                case Some(user) => {
                  requiredRight match {
                    case None => f(User(email))(request)
                    case Some(right) => Async {
                      val futureRelatedClients = accountsColl.find(Json.obj("contacts" -> Json.obj("email" -> email))).
                        projection(Json.obj("_id" -> 1)).
                        cursor[JsObject].
                        toList
                      futureRelatedClients map { clients =>
                        val clientIdList = clients.flatMap(client => (client \ "_id" \ "$oid").asOpt[String])
                        val result = for {
                          email <- (user \ "email").asOpt[String]
                          profiles <- (user \ "profiles").asOpt[List[String]]
                          rights = convertProfilesToRights(profiles)
                          if rights contains right
                        } yield
                          f(User(
                            email,
                            rights,
                            clientIdList
                          ))(request)
                        result getOrElse Unauthorized(Json.obj("error" -> JsString("You don't have the right " + right)))
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Overloaded method to use the default body parser
  def Authenticated(requiredRight: Option[String])(f: User => Request[AnyContent] => Result): Action[AnyContent]  = {
    Authenticated[AnyContent](parse.anyContent)(requiredRight)(f)
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
