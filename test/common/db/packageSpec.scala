package common

import org.specs2.mutable._

import com.mongodb.casbah.Imports.{MongoDB, MongoClient}

import play.api.test._
import play.api.test.Helpers._

class HelloWorldSpec extends Specification {

  "getDB" should {
    "be an instance of MongoDB" in new WithApplication {
      db.getDB must beAnInstanceOf[MongoDB]
    }
  }

  "getClient" should {
    "be an instance of MongoClient" in new WithApplication {
      db.getClient must beAnInstanceOf[MongoClient]
    }
  }
}
