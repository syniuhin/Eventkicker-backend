package com.startdown.models

import com.startdown.utils.PostgresSupport
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}
import spray.json._
import com.github.tototoshi.slick.PostgresJodaSupport._

/**
  * infm created it with love on 11/7/15. Enjoy ;)
  */

case class Event(id: Option[Long],
                 name: Option[String],
                 timeCreated: Option[DateTime],
                 timeScheduled: Option[DateTime],
                 description: Option[String],
                 pictureUrl: Option[String],
                 userCreatedId: Option[Long]/*,
                 participantIds: Option[List[Long]],
                 itemsNeeded: Option[List[String]]*/)

object JodaTimeJsonProtocol extends DefaultJsonProtocol {
  val formatter = ISODateTimeFormat.basicDateTimeNoMillis
  implicit object JodaTimeFormat extends RootJsonFormat[DateTime] {
    def write(obj: DateTime): JsValue =
      JsString(formatter.print(obj))
    def read(json: JsValue): DateTime = json match {
      case JsString(s) => try {
        formatter.parseDateTime(s)
      } catch {
        case t: Throwable => error(s)
      }
      case _ =>
        error(json.toString())
    }
    def error(v: Any): DateTime = {
      val example = formatter.print(0)
      deserializationError(s"'$v' is not a valid date value. Dates must " +
        s"be in compact ISO-8601 format, e.g. '$example'")
    }
  }
}

object EventJsonProtocol extends DefaultJsonProtocol {
  import JodaTimeJsonProtocol._
  implicit val eventFormat = jsonFormat(Event, "id", "name", "timeCreated",
    "timeScheduled", "description", "pictureUrl", "userCreatedId")
/*
  implicit object EventJsonFormat extends RootJsonFormat[Event] {
    def write(c: Event) = {

    }
  }
*/
}

object EventDao extends PostgresSupport {

  class Events(tag: Tag) extends Table[Event](tag, "events") {
    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)
    def name = column[String]("name")
    def timeCreated = column[DateTime]("timeCreated")
    def timeScheduled = column[DateTime]("timeScheduled")
    def description = column[Option[String]]("description")
    def pictureUrl = column[Option[String]]("pictureUrl")
    def userCreatedId = column[Long]("userCreatedId")
    // def participantIds = column[Option[List[Long]]]("participantIds")
    // def itemsNeeded = column[Option[List[String]]]("itemsNeeded")

    def * = (id.?, name.?, timeCreated.?, timeScheduled.?, description,
      pictureUrl, userCreatedId.?/*, participantIds, itemsNeeded*/) <>
      (Event.tupled, Event.unapply)

    def userCreated = foreignKey("userCreatedFk", userCreatedId,
      UserDao.users)(_.id)
  }

  val events = TableQuery[Events]

  def createTable =
    db.run(events.schema.create)

  def dropTable =
    db.run(events.schema.drop)

  def listAllEvents =
    db.run(events.result)

  def addEvent(e: Event) =
    db.run(events += e.copy(timeCreated = Some(DateTime.now())))

  def findEvent(id: Long) = {
    db.run(events.filter(_.id === id).result
      map {
      case Seq(x, _*) => Some(x)
      case _ => None
    })
  }

  def getUpdatableColumns(es: Events) =
    (es.name, es.timeCreated, es.timeScheduled, es.description,
      es.pictureUrl, es.userCreatedId)
  def getUpdatableValues(e: Event) =
    (e.name.orNull, e.timeCreated.orNull,
      e.timeScheduled.orNull, e.description,
      e.pictureUrl, e.userCreatedId.get)

  def updateEvent(e: Event) = {
    val ensure = e.id.get
    val columns = for {
      es <- events.filter(_.id === ensure)
    } yield getUpdatableColumns(es)
    db.run(columns.update(getUpdatableValues(e)))
  }

  def deleteEvent(id: Long) = {
    val filterQ = events.filter(_.id === id)
    db.run(filterQ.result zip filterQ.delete map { case (res, _) =>
      res match {
        case Seq(x, _*) => Some(x)
        case _ => None
      }
    })
  }

  def deleteAll =
    db.run(events.delete)
}