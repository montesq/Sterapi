package test

import org.specs2.mutable._
import play.api.test._
import actions.Security._

class SecuritySpec extends Specification {

  "Profile ACCOUNT_MANAGER" should {
    "return the rights ACCOUNT_MANAGER, READ_ACCOUNT, WRITE_ACCOUNT" in new WithApplication {
      val rights = convertProfilesToRights(List("ACCOUNT_MANAGER"))
      rights must contain("ACCOUNT_MANAGER")
      rights must contain("READ_ACCOUNT")
      rights must contain("WRITE_ACCOUNT")
    }
  }

  "Profile ADMIN" should {
    "return ADMIN, ACCOUNT_MANAGER, READ_ACCOUNT, WRITE_ACCOUNT, READ_STERILIZATION, WRITE_STERILIZATION" in
      new WithApplication {
      val rights = convertProfilesToRights(List("ADMIN"))
      rights must contain("ADMIN")
      rights must contain("ACCOUNT_MANAGER")
      rights must contain("READ_ACCOUNT")
      rights must contain("WRITE_ACCOUNT")
      rights must contain("READ_STERILIZATION")
    }
  }
}
