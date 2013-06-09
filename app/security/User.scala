package security

import scala.collection.immutable.Map
import be.objectify.deadbolt.core.models.Subject
import play.libs.Scala

case class User(userName: String,
                rolesList: List[UserRole],
                private val _permissionsList: List[UserPermission] = Nil) extends Subject {
  val permissionsList = _permissionsList ::: rolesList.flatMap(getPermissionsByRole(_))

  def getRoles: java.util.List[UserRole] = Scala.asJava(rolesList)

  def getPermissions: java.util.List[UserPermission] = Scala.asJava(permissionsList)

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

