package security

import play.api.cache.Cache
import play.api.mvc.{Results, Result, Request}
import be.objectify.deadbolt.scala.{DynamicResourceHandler, DeadboltHandler}
import be.objectify.deadbolt.core.models.Subject
import play.api.Play.current
import play.api.libs.Crypto._

class SecurityHandler extends DeadboltHandler {

  def beforeAuthCheck[A](request: Request[A]) = None

  override def getDynamicResourceHandler[A](request: Request[A]): Option[DynamicResourceHandler] = {
    None
//    if (dynamicSecurityHandler.isDefined) dynamicResourceHandler
//    else Some(new MyDynamicResourceHandler())
  }

  override def getSubject[A](request: Request[A]): Option[Subject] = {
    Cache.getAs[User]("User." + decryptAES(request.session.get("email").toString))
  }

  def onAuthFailure[A](request: Request[A]): Result = {
    Results.Forbidden
  }
}
