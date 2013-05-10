package controllers

import org.specs2.mutable._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._

class EventsSpec extends Specification {
  val jsonHeaders = Seq(
    CONTENT_TYPE -> Seq("application/json"),
    ACCEPT -> Seq("application/json")
  )

  val json = Json.toJson(Map("host" -> "127.0.0.1"))

  "create" should {
    "insert json to database" in new WithApplication {
      val req = FakeRequest("POST", "/events", FakeHeaders(jsonHeaders), json)
      val result = Events.create()(req)

      status(result) must equalTo(CREATED)
      contentType(result) must beSome("application/json")
      contentAsString(result) must contain("$oid")
    }
  }
}
