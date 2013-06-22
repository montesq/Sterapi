package security

import scala.collection.immutable.Map
import be.objectify.deadbolt.core.models.{Role, Subject}
import play.libs.Scala

case class User(userName: String,
                _rolesList: List[Role] = Nil,
                clients: List[String] = Nil) extends Subject {
  val rolesList = convertRolesToRights(_rolesList)

  def getRoles: java.util.List[Role] = Scala.asJava(rolesList)

  //Permissions are not used
  def getPermissions = Scala.asJava(Nil)

  def getIdentifier: String = userName

  def getClients: List[String] = clients

  def convertRolesToRights(roles: List[Role]): List[UserRight] = {
    roles match {
      case Nil => Nil
      case UserRight(r) :: t => UserRight(r) :: convertRolesToRights(t)
      case UserProfile(p) :: t => convertRolesToRights(
          ProfilesRights.matrix.getOrElse(UserProfile(p), Nil)
        ) ::: convertRolesToRights(t)
      case _ => Nil
    }
  }
}

object ProfilesRights {
  val matrix: Map[Role, List[Role]] = Map(
    UserProfile("ADMIN") -> List(
      UserProfile("STERILIZATION_MANAGER"),
      UserProfile("STERILIZATION_CLIENT")),
    UserProfile("STERILIZATION_MANAGER") -> List(
      UserRight("MANAGE_ACCOUNTS"),
      UserRight("MANAGE_STERILIZATIONS")),
    UserProfile("STERILIZATION_CLIENT") -> List(
      UserRight("READ_STERILIZATIONS"))
  )
}

