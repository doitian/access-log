package module.db

import com.mongodb.casbah.Imports._
import java.util.Date
import org.specs2.mutable._
import play.api.libs.json._
import play.api.test.Helpers._

class MongoJsonSpec extends Specification {
  "fromJson" should {
    "convert JsValue(String -> String) to DBObject" in {
      val json = Json.obj("name" -> "ian")
      val dbobject = MongoJson.fromJson(json).get

      dbobject.as[String]("name") must_== "ian"
    }
    "convert $id -> String to ObjectId" in {
      val json = Json.obj("_id" -> Json.obj("$oid" -> new ObjectId().toString))
      val dbobject = MongoJson.fromJson(json).get

      dbobject("_id") must beAnInstanceOf[ObjectId]
    }
    "convert $date -> String to Date" in {
      val json = Json.obj("due" -> Json.obj("$date" -> 1000))
      val dbobject = MongoJson.fromJson(json).get

      dbobject("due") must beAnInstanceOf[Date]
    }
    "return error for invalid $id" in {
      val json = Json.obj("_id" -> Json.obj("$oid" -> "foobar"))
      val dbobject = MongoJson.fromJson(json)

      dbobject must beAnInstanceOf[JsError]
    }
    "return error for invalid $id with path" in {
      val json = Json.obj("_id" -> Json.obj("$oid" -> "foobar"))
      val dbobject = MongoJson.fromJson(json).asInstanceOf[JsError]

      dbobject.errors(0)._1.toString must_== "/_id/$oid"
    }
  }
}
