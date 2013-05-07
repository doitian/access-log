import com.mongodb.casbah.{MongoClientURI, MongoClient, MongoDB, MongoCollection}
import play.api.Play

package object db {
  private lazy val uri = (Play.current.configuration.getString("mongo.uri"): @unchecked) match {
    case Some(s) => s
  }
  private lazy val dbName = (Play.current.configuration.getString("mongo.db"): @unchecked) match {
    case Some(s) => s
  }

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
