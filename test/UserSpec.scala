import java.util
import org.specs2.mutable._
import play.libs.Scala
import security.{ProfilesRights, UserProfile, UserRight, User}
import scala.collection.JavaConverters._


class UserSpec extends Specification {
  "User class" should {
    "contain neither profile nor role by default" in {
      val user = User("toto@test.fr")
      user.rolesList must beEqualTo(Nil)
    }

    "contain only initial roles if there are only UserRight" in {
      val user = User("toto@test.fr", List(UserRight("MANAGE_ACCOUNTS")))
      user.rolesList must contain(UserRight("MANAGE_ACCOUNTS"))
    }

    "convert a UserProfile to UserRights" in {
      val user = User("toto@test.fr", List(UserProfile("STERILIZATION_CLIENT")))
      user.rolesList must contain(UserRight("READ_STERILIZATIONS"))
    }

    "convert a UserProfile when it's a profile aggregation" in {
      val user = User("toto@test.fr", List(UserProfile("ADMIN")))
      user.rolesList must contain(UserRight("READ_STERILIZATIONS"))
    }
  }
}
