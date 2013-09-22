package actions

import play.api.Logger._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DBConnection
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.mvc.BodyParsers._

object Security {

  val usersColl = DBConnection.db.collection[JSONCollection]("users")
  val accountsColl = DBConnection.db.collection[JSONCollection]("accounts")

  case class User(email: String, rights: List[String] = List(), clients: List[String] = List())

  def Authenticated[A](p: BodyParser[A])(requiredRight: Option[String])(f: User => Request[A] => Result) = {
    CORSAction {
      Action(p) { request =>
        logger.debug("-- Enter Authenticated action --")
        request.session.get("email") match {
          case None => Unauthorized(Json.obj("error" -> "The user is not authenticated"))
          case Some(email) => Async {
            logger.debug("email: " + email)

            usersColl.find(Json.obj("email" -> email)).one[JsObject].map {
              case None => Unauthorized(Json.obj("error" -> "This user doesn't exist in the database"))
              case Some(user) => {
                logger.debug("User found in the database : " + user)

                requiredRight match {
                  case None => f(User(email))(request)
                  case Some(right) => {
                    val profiles = (user \ "profiles").asOpt[List[String]].getOrElse(Nil)
                    logger.debug("Profiles: " + profiles)

                    val rights = convertProfilesToRights(profiles)
                    logger.debug("Rights: " + rights)

                    if (!(rights contains right))
                      Unauthorized(Json.obj("error" -> JsString(email + " doesn't have the right " + right)))
                    else Async {
                      val clientList = accountsColl.find(Json.obj("contacts" -> Json.obj("email" -> email))).
                        projection(Json.obj("_id" -> 1)).
                        cursor[JsObject].
                        toList
                      clientList.map {
                        clients => {
                          val clientIdList = clients.flatMap(client => (client \ "_id" \ "$oid").asOpt[String])
                          logger.debug("ClientIdList: " + clientIdList)

                          f(User(email, rights, clientIdList))(request)
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
