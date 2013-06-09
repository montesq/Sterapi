import java.util
import org.specs2.mutable._
import play.libs.Scala
import security.{UserRole, UserPermission, User}
import scala.collection.JavaConverters._


class UserSpec extends Specification {
  "User class" should {
    "contain neither role nor permission by default" in {
      val user = User("toto@test.fr", Nil)
      user.permissionsList must beEqualTo(Nil)
    }

    "contain permissionsList used to create the user" in {
      val user = User("toto@test.fr", Nil, List(UserPermission("A"), UserPermission("B")))
      user.permissionsList must contain(UserPermission("A"))
      user.permissionsList must contain(UserPermission("B"))
    }

    "contain initial permissions + permissions from Roles" in {
      val user = User("toto@test.fr", List(UserRole("ADMIN")), List(UserPermission("A"), UserPermission("B")))
      val permissionsBuffer = asScalaBufferConverter(user.getPermissions).asScala
      permissionsBuffer must contain(UserPermission("A"))
      permissionsBuffer must contain(UserPermission("B"))
      permissionsBuffer must contain(UserPermission("MANAGE_ACCOUNTS"))
    }
  }
}
