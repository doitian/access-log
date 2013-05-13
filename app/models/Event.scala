package models
import play.api.libs.json._
import play.modules.reactivemongo.json.BSONFormats

import play.api.Play
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.bson.BSONDateTime
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.LastError
import reactivemongo.core.commands.RawCommand
import scala.StringContext
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

case class MapReduce(
  val by: String,
  val key: String
) {
  import play.api.libs.concurrent.Execution.Implicits._

  private def db = Play.maybeApplication.map(ReactiveMongoPlugin.db(_))

  private def readJs(name: String): String =
    io.Source.fromInputStream(getClass.getResourceAsStream("/js/events/" + name)).mkString

  private lazy val hourlyMapf = readJs("hourlyMap.js")
  private lazy val hierarchyMapf = readJs("hierarchyMap.js")
  private lazy val reducef = readJs("reduce.js")
  private lazy val finalizef = readJs("finalize.js")

  def run(lastRun: Long = 0, cutoff: Long = Long.MaxValue): Future[List[BSONDocument]] = {
    for {
      h <- hourlyMapReduce(lastRun, cutoff)
      d <- dailyMapReduce(lastRun, cutoff)
      w <- weeklyMapReduce(lastRun, cutoff)
      m <- monthlyMapReduce(lastRun, cutoff)
      y <- yearlyMapReduce(lastRun, cutoff)
    } yield h :: d :: w :: m :: y :: Nil
  }

  def hourlyMapReduce(lastRun: Long = 0, cutoff: Long = Long.MaxValue): Future[BSONDocument] = {
    val query = BSONDocument(
      "ts" -> BSONDocument(
        "$gt" -> BSONDateTime(lastRun),
        "$lt" -> BSONDateTime(cutoff)
      )
    )

    val comm = BSONDocument(
      "mapReduce" -> "events",
      "query" -> query,
      "map" -> hourlyMapf.format(key),
      "reduce" -> reducef,
      "finalize" -> finalizef,
      "out" -> BSONDocument(
        "reduce" -> s"stats.$by.hourly"
      )
    )

    db.get.command(RawCommand(comm))
  }

  def hierarchyMapReduce(from: String, to: String, date: String)(lastRun: Long = 0, cutoff: Long = Long.MaxValue): Future[BSONDocument] = {
    val query = BSONDocument(
      "value.ts" -> BSONDocument(
        "$gt" -> BSONDateTime(lastRun),
        "$lt" -> BSONDateTime(cutoff)
      )
    )

    val comm = BSONDocument(
      "mapReduce" -> s"stats.$by.$from",
      "query" -> query,
      "map" -> hierarchyMapf.format(date),
      "reduce" -> reducef,
      "finalize" -> finalizef,
      "out" -> BSONDocument(
        "reduce" -> s"stats.$by.$to"
      )
    )

    db.get.command(RawCommand(comm))
  }

  val dailyMapReduce = hierarchyMapReduce("hourly", "daily", """new Date(Date.UTC(
    |  this._id.d.getFullYear(),
    |  this._id.d.getMonth(),
    |  this._id.d.getDate(),
    |  0, 0, 0, 0
    |))""".stripMargin)_

  val weeklyMapReduce = hierarchyMapReduce("daily", "weekly",
    "new Date(this._id.d.valueOf() - this._id.d.getDay()*24*60*60*1000)")_

  val monthlyMapReduce = hierarchyMapReduce("daily", "monthly", """new Date(Date.UTC(
    |  this._id.d.getFullYear(),
    |  this._id.d.getMonth(),
    |  1, 0, 0, 0, 0
    |))""".stripMargin)_

  val yearlyMapReduce = hierarchyMapReduce("monthly", "yearly", """new Date(Date.UTC(
    |  this._id.d.getFullYear(),
    |  0, 1, 0, 0, 0, 0
    |))""".stripMargin)_

  private def command(doc: BSONDocument): Future[BSONDocument] = {
    db.get.command(new RawCommand(doc))
  }
}

object Event {
  val pathMapReduce = MapReduce("path", "this.path")

  def mapReduce(lastRun: Long = 0, cutoff: Long = Long.MaxValue): Future[List[BSONDocument]] = {
    pathMapReduce.run(lastRun, cutoff)
  }
}
