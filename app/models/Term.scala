package models

import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import play.api.mvc.QueryStringBindable.Parsing

/**
 * Term Kind Trait
 */
trait TermKind extends EntityKind {

  // The entity types.
  type K = TermKind
  type E = Term
  type R = TermReference
  type C = Term.type
  type F = TermFrom
  type P = TermPatch
  type S = TermSearch
  type M = TermMapper

  // The entity companion.
  val companion = Term

  // Convert a term to a reference.
  def toReference(term: Term): TermReference = TermReference(term.id)
}

/**
 * Term Like Trait
 */
sealed trait TermLike extends EntityLike with TermKind

/**
 * Term Like Companion
 */
object TermLike {

  // Convert to and from the database.
  implicit val mapType = MappedColumnType.base[TermLike, Long](
    ref => ref.id.getOrElse(throw EntityReferenceException("Empty entity reference cannot be converted to long.")),
    id => TermReference(Some(id))
  )

  // Convert to and from the query string.
  implicit object bindType extends Parsing[TermLike](
    str => TermReference(id = Some(str.toLong)),
    ref => ref.id.get.toString(),
    (key: String, e: Exception) => "Cannot parse parameter %s as TermLike: %s".format(key, e.getMessage)
  )

  // Convert to JSON.
  val jsonWrite = new Writes[TermLike] {
    def writes(v: TermLike) = v match {
      case ref: TermReference => Json.toJson(ref)(TermReference.jsonFormat).as[JsObject]
      case full: Term => Json.toJson(full)(Term.jsonFormat).as[JsObject]
    }
  }

  // Convert from JSON.
  val jsonRead = new Reads[TermLike] {
    def reads(json: JsValue) = {
      json.asOpt[Term].orElse(json.asOpt[TermReference]).map(v => JsSuccess(v)).getOrElse(JsError("Could not read TermLike"))
    }
  }

  // Convert to and from JSON.
  implicit val jsonFormat: Format[TermLike] = Format(jsonRead, jsonWrite)
}

/**
 * Term From Class
 *
 * @type {TermFrom}
 */
case class TermFrom(val name: String) extends EntityFrom with TermKind

/**
 * Term From Companion
 */
object TermFrom {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[TermFrom]
}

/**
 * Term Patch Class
 *
 * @type {TermPatch}
 */
case class TermPatch(
  val name: Option[String] = None,
  val visible: Option[Boolean] = None,
  val deleted: Option[Boolean] = None,
  val connections: Option[Set[ConnectionLike]] = None
) extends EntityPatch with TermKind

/**
 * Term Patch Companion
 */
object TermPatch {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[TermPatch]
}

/**
 * Term Search Class
 *
 * @type {TermSearch}
 */
case class TermSearch(
  val id: Option[Long] = None,
  val ids: Option[Set[Long]] = None,
  val name: Option[String] = None,
  val slug: Option[String] = None,
  val visible: Option[Boolean] = Some(true),
  val deleted: Option[Boolean] = Some(false),
  val createdBy: Option[UserLike] = None
) extends EntitySearch with TermKind

/**
 * Term Reference Class
 *
 * @type {TermReference}
 */
case class TermReference(val id: Option[Long] = None, val from: Option[TermFrom] = None) extends EntityReference with TermLike

/**
 * Term Reference Companion
 */
object TermReference {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[TermReference]
}

/**
 * Term Class
 *
 * @type {Term}
 */
case class Term(
  val id: Option[Long] = None,
  val name: String,
  val slug: String,
  val visible: Boolean = true,
  val deleted: Boolean = false,
  val createdAt: DateTime,
  val createdBy: UserLike,
  val connections: Set[ConnectionLike] = Set.empty[ConnectionLike]
) extends Entity with TermLike with HasName with HasCreator with TermKind

/**
 * Term Companion
 */
object Term extends EntityCompanion with TermKind {

  // Convert a string to a slug.
  def toSlug(str: String): String = {
    str.toLowerCase.trim.replace(" ", "-")
  }

  // Create a reference from a term.
  def toRef(term: Term): TermReference = TermReference(term.id)

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[Term]
}
