package security

import be.objectify.deadbolt.core.models.Permission

case class UserPermission (value: String) extends Permission {
  def getValue: String = value
}
