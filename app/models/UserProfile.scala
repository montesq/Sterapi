package models

import be.objectify.deadbolt.core.models.Role

case class UserProfile(roleName: String) extends Role {
  def getName: String = roleName
}
