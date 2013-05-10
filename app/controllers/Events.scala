package controllers

import com.mongodb.casbah.Imports._
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Controller
import play.api.libs.json.Json
import play.api.libs.json.JsError

import common.db
import common.db.MongoJson.readDBObject

object Events extends Controller {
  val create = Action(parse.json) { implicit request =>
    request.body.validate[DBObject].map{
      case event =>
        db.withDB { db =>
          db("events") += event
        }
        Created(DBObject("_id" -> event("_id")).toString).as(JSON)
    }.recoverTotal{
      e => BadRequest(Json.obj("error" -> JsError.toFlatJson(e)))
    }
  }
}
