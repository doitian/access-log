package controllers

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Controller
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api._
import reactivemongo.bson.BSONDateTime
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.core.commands.LastError
import reactivemongo.core.errors.DatabaseException
import scala.concurrent.Future
import scala.util.Random

object Events extends Controller with MongoController {
  import play.modules.reactivemongo.json.BSONFormats._

  def collection = db.collection[JSONCollection]("events")

  val rand = new Random(System.currentTimeMillis())

  val readNewBSONDocument = new Reads[BSONDocument] {
    def reads(js: JsValue) =
      Json.fromJson[BSONDocument](js).flatMap { doc =>
        doc.get("_id") match {
          case Some(_) => JsError(__ \ "_id", ValidationError("validate.error.forbidden"))
          case _ =>
            val id = BSONObjectID.generate
            JsSuccess(doc.add("_id" -> id))
        }
      }
  }

  def stats(by: String, unit: String) = Action { implicit request =>
    val collection = db.collection[JSONCollection](s"stats.$by.$unit")
    Async {
      val cursor = collection.find(BSONDocument()).cursor[JsObject]
      cursor.toList.map { objects =>
        Ok(Json.arr(objects))
      }
    }
  }

  val create = Action(parse.json) { implicit request =>
    implicit val reads = readNewBSONDocument

    request.body.validate[BSONDocument].map {
      case doc =>
        Async {
          collection.insert(doc).map { _ =>
            Created(Json.obj("_id" -> toJSON(doc.get("_id").get)))
          }
        }
    } recoverTotal {
      e => BadRequest(Json.obj("error" -> JsError.toFlatJson(e)))
    }
  }

  val fill = Action(parse.json) { implicit request =>
    implicit val reads = (__ \ "count").readNullable[Int]

    val paths = Array("/dashboard", "/account", "/profile", "/events")
    val hosts = Array("192.168.0.1", "192.168.0.2", "192.168.0.3", "192.168.0.4")
    val referers = Array("http://www.google.com", "http://www.baidu.com", "http://bing.com", "http://twitter.com")
    val from = System.currentTimeMillis()

    request.body.validate[Option[Int]].map {
      case c =>
        val count = c getOrElse 1000
        Async {
          val futures = for (i <- 1 to count) yield {
            val doc = BSONDocument(
              "host" -> sample(hosts),
              "path" -> sample(paths),
              "referer" -> sample(referers),
              "ts" -> BSONDateTime(from - rand.nextLong().abs % (60 * 60 * 24 * 30 * 1000)),
              "length" -> rand.nextInt(1000)
            )

            collection.insert(doc)
          }

          futures.foldLeft(Future(List[LastError]())) { (a, b) =>
            for {
              t <- a
              h <- b
            } yield h :: t
          }.map { errors =>
            Ok(Json.obj("count" -> count))
          }
        }
    } recoverTotal {
      e => BadRequest(Json.obj("error" -> JsError.toFlatJson(e)))
    }
  }

  private def sample[A](arr: Array[A]) = arr(rand.nextInt(arr.size))
}
