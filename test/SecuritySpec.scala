import org.specs2.mutable._

import play.api.libs.ws.WS
import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.Some
import utils.DBConnection
import actions.Security._

class SecuritySpec extends Specification {

  "Profile ACCOUNT_MANAGER" should {
    "return List(\"READ_ACCOUNT\", \"WRITE_ACCOUNT\")" in new WithApplication {
      convertProfilesToRights(List("ACCOUNT_MANAGER")) must beEqualTo(List("ACCOUNT_MANAGER", "READ_ACCOUNT", "WRITE_ACCOUNT"))
    }
  }

  "Profile ADMIN" should {
    "return READ_ACCOUNT, WRITE_ACCOUNT, READ_STERILIZATION, WRITE_STERILIZATION" in new WithApplication {
      val convert: List[String] = convertProfilesToRights(List("ADMIN"))
      convert must contain("ADMIN")
      convert must contain("ACCOUNT_MANAGER")
      convert must contain("READ_ACCOUNT")
      convert must contain("WRITE_ACCOUNT")
      convert must contain("READ_STERILIZATION")
    }
  }
}
