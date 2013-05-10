package common.db

import com.mongodb.casbah.Imports._
import java.text.DateFormat
import java.util.Date
import play.api.data.validation.ValidationError
import play.api.libs.json._

object MongoJson {
  def fromJson(json: JsValue) : JsResult[DBObject] = readDBObject.reads(json)

  implicit val readDBObject = new Reads[DBObject] {
    def reads(js: JsValue): JsResult[DBObject] = {
      parsePlainObject(js.asInstanceOf[JsObject], JsPath())
    }

    private def parsePlainObject(obj: JsObject, parent: JsPath): JsResult[DBObject] = {
      parsePlainFields(obj.fields.toList, parent).map(DBObject(_))
    }

    private def parsePlainFields(l: List[(String, JsValue)], parent: JsPath): JsResult[List[(String, Any)]] = {
      l match {
        case Nil => JsSuccess(Nil, parent)
        case head :: tail => cons(
          parse(head._2, (parent \ head._1)).map(head._1 -> _),
          parsePlainFields(tail, parent)
        )
      }
    }

    private def parse(obj: JsObject, parent: JsPath): JsResult[Any] = {
      if(obj.fields.length > 0) {
        obj.fields(0) match {
          case ("$date", v: JsValue) =>
            val path = parent \ "$date"
            try {
              v match {
                case number: JsNumber => JsSuccess(new Date(number.value.toLong), path)
                case _ => JsSuccess(DateFormat.getDateInstance().parse(v.toString))
              }
            } catch {
              case ex: IllegalArgumentException => JsError(path, ValidationError("validation.invalid", "$date"))
            }
          case ("$oid", v :JsString) =>
            val path = parent \ "$oid"
            try {
              JsSuccess(new ObjectId(v.value), path)
            } catch  {
              case ex: IllegalArgumentException => JsError(path, ValidationError("validation.invalid", "$oid"))
            }
          case _ => parsePlainObject(obj, parent)
        }
      } else parsePlainObject(obj, parent)
    }

    private def parse(arr: JsArray, parent: JsPath): JsResult[List[Any]] = {
      parse(arr.value.toList, parent, 0)
    }

    private def parse(l: List[JsValue], parent: JsPath, i: Int): JsResult[List[Any]] = {
      l match {
        case Nil => JsSuccess(Nil)
        case head :: tail => cons(parse(head, parent(i)), parse(tail, parent, i + 1))
      }
    }

    private def cons[T](head: JsResult[T], tail: JsResult[List[T]]): JsResult[List[T]] = {
      (head, tail) match {
        case (h: JsError, t: JsError) => h ++ t
        case (JsSuccess(h, _), JsSuccess(t, _)) => JsSuccess(h :: t)
        case (h: JsError, _) => h
        case _ => tail
      }
    }

    private def parse(js: JsValue, parent: JsPath): JsResult[Any] = {
      js match {
        case v: JsObject => parse(v, parent)
        case v: JsArray => parse(v, parent)
        case v: JsString => JsSuccess(v.value, parent)
        case v: JsNumber => JsSuccess(v.value, parent)
        case v: JsBoolean => JsSuccess(v.value, parent)
        case JsNull => JsSuccess(null)
        case _: JsUndefined => JsSuccess(null)
      }
    }
  }
}
