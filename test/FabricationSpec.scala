import org.specs2.mutable._

import play.api.libs.json._
import play.api.cache.Cache
import play.api.Play.current
import play.api.test.Helpers._
import play.api.test.{FakeRequest, WithApplication}
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import utils.DBConnection

class FabricationSpec extends Specification {
  "GET /fabrication" should {
    "test" in new WithApplication {
      route(FakeRequest(GET, "/fabrication"))
    }
  }
  "GET /fabrication" should {
    "test" in new WithApplication {
      route(FakeRequest(GET, "/fabrication"))
    }
  }
}
