import akka.actor.{Actor, Props}
import com.typesafe.config.ConfigFactory
import java.io.File
import play.api._
import play.api.libs.concurrent.Akka
import play.modules.reactivemongo.ReactiveMongoPlugin
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.{GetLastError, LastError}
import scala.concurrent.Future
import scala.concurrent.duration._

object Global extends GlobalSettings {

  class EventsMapReduce extends Actor {
    import play.modules.reactivemongo.json.BSONFormats._
    import play.api.libs.concurrent.Execution.Implicits._

    private def db = Play.maybeApplication.map(ReactiveMongoPlugin.db(_))

    private def conf = db.get.collection[BSONCollection]("conf")

    private val lastRunQuery = BSONDocument("k" -> "events.mapReduce.lastRun")

    def receive = {
      case _ => {
        getLastRun().map { lastRun =>
          val cutOff = System.currentTimeMillis() - (60 * 1000)
          models.Event.mapReduce(lastRun, cutOff).map { _ =>
            setLastRun(cutOff)
          }
        }
      }
    }

    def getLastRun(): Future[Long] = {
      conf.find(lastRunQuery).one.map {
        case Some(doc) => doc.getAs[Long]("v") getOrElse 0L
        case _ => 0L
      }
    }

    def setLastRun(lastRun: Long): Future[LastError] = {
      conf.update(lastRunQuery, BSONDocument("$set" ->
        BSONDocument("v" -> lastRun)
      ), GetLastError(), true)
    }
  }

  override def onStart(app: Application) {
    import play.api.libs.concurrent.Execution.Implicits._
    import play.api.Play.current

    val myActor = Akka.system.actorOf(Props[EventsMapReduce], name = "EventsMapReduce")

    Akka.system.scheduler.schedule(
      0.seconds,
      5.minutes,
      myActor,
      "EventsMapReduce"
    )
  }

  override def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode.Mode): Configuration = {
    val modeSpecificConfig = config ++ Configuration(ConfigFactory.load(s"application.${mode.toString.toLowerCase}.conf"))
    super.onLoadConfig(modeSpecificConfig, path, classloader, mode)
  }
}
