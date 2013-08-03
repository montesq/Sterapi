package controllers

import play.api.libs.json.JsObject
import play.autosource.reactivemongo.ReactiveMongoAutoSourceController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController

object Fabrication extends Controller with MongoController{
  val coll = db.collection[JSONCollection]("fabrications")

  def create = TODO

  def list = TODO

  def getOne(id: String) = TODO

  def updateOne(id: String) = TODO
}
