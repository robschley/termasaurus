package models

import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import play.api.mvc.QueryStringBindable.Parsing

/**
 * Context Kind Trait
 */
trait ContextKind extends EntityKind {

  // The entity types.
  type K = ContextKind
  type E = Context
  type R = ContextReference
  type C = Context.type
  type F = ContextFrom
  type P = ContextPatch
  type S = ContextSearch
  type M = ContextMapper

  // The entity companion.
  val companion = Context

  // Convert a context to a reference.
  def toReference(context: Context): ContextReference = ContextReference(context.id)
}

/**
 * Context Like Trait
 */
sealed trait ContextLike extends EntityLike with ContextKind

/**
 * Context Like Companion
 */
object ContextLike {

  // Convert to and from the database.
  implicit val mapType = MappedColumnType.base[ContextLike, Long](
    ref => ref.id.getOrElse(throw EntityReferenceException("Empty entity reference cannot be converted to long.")),
    id => ContextReference(Some(id))
  )

  // Convert to and from the query string.
  implicit object bindType extends Parsing[ContextLike](
    str => ContextReference(id = Some(str.toLong)),
    ref => ref.id.get.toString(),
    (key: String, e: Exception) => "Cannot parse parameter %s as ContextLike: %s".format(key, e.getMessage)
  )

  // Convert to JSON.
  val jsonWrite = new Writes[ContextLike] {
    def writes(v: ContextLike) = v match {
      case ref: ContextReference => Json.toJson(ref)(ContextReference.jsonFormat).as[JsObject]
      case full: Context => Json.toJson(full)(Context.jsonFormat).as[JsObject]
    }
  }

  // Convert from JSON.
  val jsonRead = new Reads[ContextLike] {
    def reads(json: JsValue) = {
      json.asOpt[Context].orElse(json.asOpt[ContextReference]).map(v => JsSuccess(v)).getOrElse(JsError("Could not read ContextLike"))
    }
  }

  // Convert to and from JSON.
  implicit val jsonFormat: Format[ContextLike] = Format(jsonRead, jsonWrite)
}

/**
 * Context From Class
 *
 * @type {ContextFrom}
 */
case class ContextFrom(val name: String) extends EntityFrom with ContextKind

/**
 * Context From Companion
 */
object ContextFrom {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[ContextFrom]
}

/**
 * Context Patch Class
 *
 * @type {ContextPatch}
 */
case class ContextPatch(
  val name: Option[String] = None,
  val visible: Option[Boolean] = None,
  val deleted: Option[Boolean] = None,
  val connections: Option[Set[ConnectionLike]] = None
) extends EntityPatch with ContextKind

/**
 * Context Patch Companion
 */
object ContextPatch {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[ContextPatch]
}

/**
 * Context Search Class
 *
 * @type {ContextSearch}
 */
case class ContextSearch(
  val id: Option[Long] = None,
  val ids: Option[Set[Long]] = None,
  val name: Option[String] = None,
  val visible: Option[Boolean] = Some(true),
  val deleted: Option[Boolean] = Some(false),
  val createdBy: Option[UserLike] = None
) extends EntitySearch with ContextKind

/**
 * Context Reference Class
 *
 * @type {ContextReference}
 */
case class ContextReference(val id: Option[Long] = None, val from: Option[ContextFrom] = None) extends EntityReference with ContextLike

/**
 * Context Reference Companion
 */
object ContextReference {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[ContextReference]
}

/**
 * Context Class
 *
 * @type {Context}
 */
case class Context(
  val id: Option[Long] = None,
  val name: String,
  val visible: Boolean = true,
  val deleted: Boolean = false,
  val createdAt: DateTime,
  val createdBy: UserLike,
  val connections: Set[ConnectionLike] = Set.empty[ConnectionLike]
) extends Entity with ContextLike with HasName with HasCreator with ContextKind

/**
 * Context Companion
 */
object Context extends EntityCompanion with ContextKind {

  // Create a reference from a context.
  def toRef(context: Context): ContextReference = ContextReference(context.id)

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[Context]
}
