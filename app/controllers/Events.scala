package controllers

import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import reactivemongo.bson.BSONDocument
import reactivemongo.bson.BSONObjectID

object Events extends Controller with MongoController {
  import play.modules.reactivemongo.json.BSONFormats._

  def collection = db.collection[JSONCollection]("events")

  val create = Action(parse.json) { implicit request =>
    request.body.validate[BSONDocument].map {
      case doc =>
        val id = BSONObjectID.generate
        Async {
          collection.insert(doc.add("_id" -> id)).map { _ =>
            Created(Json.obj("_id" -> Json.toJson(id)))
          }
        }
    }.recoverTotal {
      e => BadRequest(Json.obj("error" -> JsError.toFlatJson(e)))
    }
  }
}
