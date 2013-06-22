package models

import be.objectify.deadbolt.core.models.Role

case class UserRight(roleName: String) extends Role {
  def getName: String = roleName
}
