import play.api._
import play.api.libs.json.Json
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.indexes.{IndexType, Index}
import utils.DBConnection
import scala.concurrent.ExecutionContext.Implicits.global


object Global extends GlobalSettings {
  val usersColl = DBConnection.db.collection[JSONCollection]("users")

  usersColl.indexesManager.ensure(Index(Seq("email" -> IndexType.Ascending), None, unique = true))

  usersColl.update(Json.obj("email" -> "montesq@aliceadsl.fr"),
    Json.obj("email" -> "montesq@aliceadsl.fr", "profiles" -> Seq(Json.obj("role" -> "ADMIN"))),
    upsert = true)

  val accountsColl = DBConnection.db.collection[JSONCollection]("accounts")

  accountsColl.indexesManager.ensure(
    Index(Seq("status" -> IndexType.Ascending), None))

}
