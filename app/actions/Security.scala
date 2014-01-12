package actions

import play.api.Logger._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DBConnection
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.mvc.BodyParsers._
import play.api.Play._
import play.api.Logger.logger
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.Some
import actions.CORSAction
import play.api.libs.Crypto._
import org.joda.time.DateTime
import scala.concurrent.Future
import play.api.libs.functional.syntax._


object Security {

  val usersColl = DBConnection.db.collection[JSONCollection]("users")
  val accountsColl = DBConnection.db.collection[JSONCollection]("accounts")
  val authRequestHeader = current.configuration.getString("auth.requestHeader").get


  case class Profile(account: String, role: String) {
    val rights = convertProfilesToRights(List(role))
  }
  implicit val profileReads = (
    (__ \ "account").read[String] ~
    (__ \ "role").read[String]
  )(Profile)

  implicit val profileWrites = new Writes[Profile] {
    def writes(p: Profile): JsValue = {
      Json.obj(
        "account" -> p.account,
        "role" -> p.role,
        "rights" -> p.rights
      )
    }
  }

  case class User(email: String, profiles: List[Profile])
  implicit val userReads = (
      (__ \ "email").read[String] ~
      (__ \ "profiles").read[List[Profile]]
  )(User)

  def Authenticated[A](p: BodyParser[A])(requiredRight: Option[String])(f: User => Request[A] => Result) = {
    CORSAction {
      Action(p) { request =>
        readAuthToken(request) match {
          case None => Unauthorized
          case Some(email) => Async {
            usersColl.find(Json.obj("email" -> email.toLowerCase)).one[JsObject].map {
              case None => {
                logger.debug(s"User with email : $email doesn't exist in the database")
                Unauthorized
              }
              case Some(jsonUser) => {
                val user = jsonUser.as[User]
                logger.debug(s"user is : $user")
                requiredRight match {
                  case None => f(user)(request)
                  case Some(authRight) => {
                    if (user.profiles.exists(_.rights.contains(authRight))) f(user)(request)
                    else Unauthorized
                  }
                }
              }
            }.recover {
              case e => {
                logger.debug(s"Internal server error in authentication process : $e")
                InternalServerError
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

  def readAuthToken[A](request: Request[A]) = {
    request.headers.get("X-Auth-Token") match {
      case Some(token) => {
        logger.debug(s"X-Auth-Token: $token")
        val jsonToken = Json.parse(decryptAES(token))
        logger.debug(s"jsonToken: $jsonToken")
        val email = (jsonToken \ "email").asOpt[String].getOrElse("")
        logger.debug("email: " + email)
        val stringEndDate = (jsonToken \ "endDate").asOpt[String].getOrElse("")
        logger.debug("endDate: " + stringEndDate)
        val endDate: DateTime = new DateTime(stringEndDate)
        val currentDate: DateTime = new DateTime()
        if (endDate.isBefore(currentDate)) {
          logger.debug(s"Token has expired : endDate = $endDate")
          None
        } else Some(email)
      }
      case None => None
    }
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
    "STERILIZATION_CLIENT" -> List("READ_STERILIZATION"),
    "FABRICATION_CLIENT" -> List("READ_FABRICATION")
  )
}
