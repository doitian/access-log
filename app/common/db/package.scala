package common

import com.mongodb.casbah.{MongoClientURI, MongoClient, MongoDB, MongoCollection}
import play.api.Play

package object db {
  private val DEFAULT_CONFIG = Map(
    "uri" -> "mongodb://localhost",
    "db" -> "access-log-development"
  )

  private def getConfig(name: String): String = {
    Play.current.configuration.getString("mongo." + name).getOrElse(DEFAULT_CONFIG(name))
  }

  private lazy val uri = getConfig("uri")
  private lazy val dbName = getConfig("db")

  def getClient = MongoClient(MongoClientURI(uri))

  def getDB = getClient.apply(dbName)

  def withClient(callback: MongoClient => Unit) {
    val c = getClient
    try {
      callback(c)
    } finally {
      c.close()
    }
  }

  def withDB(callback: MongoDB => Unit) {
    withClient { client =>
      callback(client(dbName))
    }
  }
}
