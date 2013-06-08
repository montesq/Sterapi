package security

import scala.collection.immutable.Map
import be.objectify.deadbolt.core.models.Subject
import play.libs.Scala

case class User(userName: String, rolesList: List[UserRole]) extends Subject {
  def getRoles: java.util.List[UserRole] = Scala.asJava(rolesList)

  def getPermissions: java.util.List[UserPermission] = {
    Scala.asJava(
      rolesList.flatMap(getPermissionsByRole(_))
    )
  }

  def getIdentifier: String = userName

  def getPermissionsByRole(role: UserRole): List[UserPermission] = {
    val roleToPermissions = Map(
      UserRole("ADMIN") -> List(
        UserPermission("MANAGE_ACCOUNTS"),
        UserPermission("MANAGE_STERILIZATIONS")
      ),
      UserRole("STERILIZATION_MANAGER") -> List(
        UserPermission("MANAGE_ACCOUNTS"),
        UserPermission("MANAGE_STERILIZATIONS")
      ),
      UserRole("STERILIZATION_CLIENT") -> List(
        UserPermission("READ_STERILIZATIONS")
      )
    )

    roleToPermissions.getOrElse(role, Nil)
  }
}

