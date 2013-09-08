package controllers

import play.api.mvc._
import play.api.libs.ws.WS
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.data._
import play.api.libs.json._
import play.api.cache.Cache
import play.api.Play.current
import utils.DBConnection
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import actions.CORSAction

object Auth extends Controller with MongoController {

  val usersColl = DBConnection.db.collection[JSONCollection]("users")
  val authSessionAttribute = current.configuration.getString("auth.attribute").get
  val authVerifyURL = current.configuration.getString("auth.verifyURL").get

  def login = CORSAction {
    Action (parse.json) {  json =>
      json.body.transform((__ \ "assertion").json.pickBranch).map { assertion =>
        Async {
          val postBody = Map(
            "assertion" -> Seq((assertion \ "assertion").as[String]),
            "audience" -> Seq(json.headers.get("Origin").getOrElse("http://localhost:9000"))
          )
          WS.url(authVerifyURL).post(postBody).map { result =>
            if (result.status == OK) {
              val status = (result.json \ "status").as[String]
              if (status == "okay") {
                val email = (result.json \ "email").as[String]
                Async{
                  usersColl.find(result.json.transform((__ \ "email").json.pickBranch).get).one[JsObject].map {
                    //TODO: create the cookie with a couple email + timestamp
                    case Some(user) => Ok(user).withSession(authSessionAttribute -> email)
                    case None => BadRequest("This user doesn't exist")
                  }.recover{ case e =>
                    InternalServerError(JsString("exception %s".format(e.getMessage)))
                  }
                }
              }
              else BadRequest("Invalid assertion")
            }
            else InternalServerError("Impossible to check the assertion")
          }
        }
      }.getOrElse(BadRequest("Expecting JSON data"))
    }
  }

  def logout = CORSAction {
    Action { request =>
      request.session.get(authSessionAttribute) match {
        case Some(s) => Cache.remove("User." + s)
        case _ => ()
      }
      Ok.withNewSession
    }
  }
}
