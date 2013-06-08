package security

import be.objectify.deadbolt.core.models.Role

case class UserRole (roleName: String) extends Role {
  def getName: String = roleName
}
