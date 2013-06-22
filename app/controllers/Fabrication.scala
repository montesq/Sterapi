package controllers

import play.api.libs.json.JsObject
import play.autosource.reactivemongo.ReactiveMongoAutoSourceController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global

object Fabrication extends ReactiveMongoAutoSourceController[JsObject]{
  val coll = db.collection[JSONCollection]("fabrications")
}
