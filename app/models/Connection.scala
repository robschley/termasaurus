package models

import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import play.api.mvc.QueryStringBindable.Parsing

/**
 * Connection Kind Trait
 */
trait ConnectionKind extends EntityKind {

  // The entity types.
  type K = ConnectionKind
  type E = Connection
  type R = ConnectionReference
  type C = Connection.type
  type F = ConnectionFrom
  type P = ConnectionPatch
  type S = ConnectionSearch
  type M = ConnectionMapper

  // The entity companion.
  val companion = Connection

  // Convert a connection to a reference.
  def toReference(connection: Connection): ConnectionReference = ConnectionReference(connection.id)
}

/**
 * Connection Like Trait
 */
sealed trait ConnectionLike extends EntityLike with ConnectionKind

/**
 * Connection Like Companion
 */
object ConnectionLike {

  // Convert to and from the database.
  implicit val mapType = MappedColumnType.base[ConnectionLike, Long](
    ref => ref.id.getOrElse(throw EntityReferenceException("Empty entity reference cannot be converted to long.")),
    id => ConnectionReference(Some(id))
  )

  // Convert connections to and from the database.
  implicit val mapTypeSet = MappedColumnType.base[Set[ConnectionLike], String](
    obj => obj.map(_.id).flatten.toSet.mkString(","),
    str => str.split(",").filter(_.size > 0).map(_.toLong).map(id => ConnectionReference(Some(id))).toSet
  )

  // Convert to and from the query string.
  implicit object bindType extends Parsing[ConnectionLike](
    str => ConnectionReference(id = Some(str.toLong)),
    ref => ref.id.get.toString(),
    (key: String, e: Exception) => "Cannot parse parameter %s as ConnectionLike: %s".format(key, e.getMessage)
  )

  // Convert to JSON.
  val jsonWrite = new Writes[ConnectionLike] {
    def writes(v: ConnectionLike) = v match {
      case ref: ConnectionReference => Json.toJson(ref)(ConnectionReference.jsonFormat).as[JsObject]
      case full: Connection => Json.toJson(full)(Connection.jsonFormat).as[JsObject]
    }
  }

  // Convert from JSON.
  val jsonRead = new Reads[ConnectionLike] {
    def reads(json: JsValue) = {
      json.asOpt[Connection].orElse(json.asOpt[ConnectionReference]).map(v => JsSuccess(v)).getOrElse(JsError("Could not read ConnectionLike"))
    }
  }

  // Convert to and from JSON.
  implicit val jsonFormat: Format[ConnectionLike] = Format(jsonRead, jsonWrite)
}

/**
 * Connection From Class
 *
 * @type {ConnectionFrom}
 */
case class ConnectionFrom(val term: TermLike, val context: ContextLike, val votes: Long = 0L) extends EntityFrom with ConnectionKind

/**
 * Connection From Companion
 */
object ConnectionFrom {

  // Create a connection from a vote.
  def fromVote(vote: Vote): ConnectionFrom = {
    ConnectionFrom(term = vote.term, context = vote.context, votes = 1L)
  }

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[ConnectionFrom]
}

/**
 * Connection Patch Class
 *
 * @type {ConnectionPatch}
 */
case class ConnectionPatch(
  val visible: Option[Boolean] = None,
  val deleted: Option[Boolean] = None,
  val votes: Option[Long] = None
) extends EntityPatch with ConnectionKind

/**
 * Connection Patch Companion
 */
object ConnectionPatch {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[ConnectionPatch]
}

/**
 * Connection Search Class
 *
 * @type {ConnectionSearch}
 */
case class ConnectionSearch(
  val id: Option[Long] = None,
  val ids: Option[Set[Long]] = None,
  val term: Option[TermLike] = None,
  val context: Option[ContextLike] = None,
  val visible: Option[Boolean] = Some(true),
  val deleted: Option[Boolean] = Some(false),
  val createdBy: Option[UserLike] = None,
  val votes: Option[Long] = None
) extends EntitySearch with ConnectionKind

/**
 * Connection Search Companion
 */
object ConnectionSearch {

  // Create a connection search from a vote.
  def fromVote(vote: Vote): ConnectionSearch = {
    ConnectionSearch(term = Some(vote.term), context = Some(vote.context))
  }
}

/**
 * Connection Reference Class
 *
 * @type {ConnectionReference}
 */
case class ConnectionReference(val id: Option[Long] = None, val from: Option[ConnectionFrom] = None) extends EntityReference with ConnectionLike

/**
 * Connection Reference Companion
 */
object ConnectionReference {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[ConnectionReference]
}

/**
 * Connection Class
 *
 * @type {Connection}
 */
case class Connection(
  val id: Option[Long] = None,
  val term: TermLike,
  val context: ContextLike,
  val visible: Boolean = true,
  val deleted: Boolean = false,
  val createdAt: DateTime,
  val createdBy: UserLike,
  val votes: Long = 0L
) extends Entity with ConnectionLike with HasCreator with ConnectionKind

/**
 * Connection Companion
 */
object Connection extends EntityCompanion with ConnectionKind {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[Connection]

  // Create a reference from a connection.
  def toRef(connection: Connection): ConnectionReference = ConnectionReference(connection.id)
}
