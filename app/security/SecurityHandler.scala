package security

import play.api.cache.Cache
import play.api.mvc._
import be.objectify.deadbolt.scala.{DynamicResourceHandler, DeadboltHandler}
import be.objectify.deadbolt.core.models.Subject
import play.api.Play.current
import models.User

class SecurityHandler extends DeadboltHandler {

  val authAttribute = current.configuration.getString("auth.attribute").get

  def beforeAuthCheck[A](request: Request[A]) = None

  override def getDynamicResourceHandler[A](request: Request[A]): Option[DynamicResourceHandler] = {
    None
//    if (dynamicSecurityHandler.isDefined) dynamicResourceHandler
//    else Some(new MyDynamicResourceHandler())
  }

  override def getSubject[A](request: Request[A]): Option[Subject] = {
    request.session.get(authAttribute) match {
      case Some(s) => Cache.getAs[User]("User." + s) //TODO: launch the query to get the user from database
      case _       => None
    }
  }

  def onAuthFailure[A](request: Request[A]): Result = {
    Results.Forbidden
  }
}
