package security

import play.api.cache.Cache
import play.api.mvc._
import be.objectify.deadbolt.scala.{DynamicResourceHandler, DeadboltHandler}
import be.objectify.deadbolt.core.models.Subject
import play.api.Play.current
import play.api.libs.Crypto._

class SecurityHandler extends DeadboltHandler {

  val cookieAuthAttribute = "auth"

  def beforeAuthCheck[A](request: Request[A]) = None

  override def getDynamicResourceHandler[A](request: Request[A]): Option[DynamicResourceHandler] = {
    None
//    if (dynamicSecurityHandler.isDefined) dynamicResourceHandler
//    else Some(new MyDynamicResourceHandler())
  }

  override def getSubject[A](request: Request[A]): Option[Subject] = {
    request.cookies.get(cookieAuthAttribute) match {
      case Some(s) => Cache.getAs[User]("User." + s.value)
      case _       => None
    }
  }

  def onAuthFailure[A](request: Request[A]): Result = {
    Results.Forbidden
  }
}
