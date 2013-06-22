package test

import org.specs2.mutable._
import play.api.test.Helpers._
import scala.Some
import play.api.test.{WithApplication, FakeRequest}
import play.api.cache.Cache
import play.api.Play.current
import models.{User, UserRight}


class TestSpec extends Specification {
  "Test controller" should {
    "return 403 if the user has not the right" in new WithApplication {
      val Some(result) = route(FakeRequest(GET, "/"))
      status(result) must equalTo(FORBIDDEN)
    }

    "return 200 if the user has the right" in new WithApplication {
      val emailUser = "test@test.fr"
      def session = ("email", emailUser)
      Cache.set("User." + emailUser, User(emailUser, List(UserRight("foo"))))

      val fakeRequest = FakeRequest(GET, "/").withSession(session)
      val Some(result) = route(fakeRequest)

      status(result) must equalTo(OK)

    }
  }
}
