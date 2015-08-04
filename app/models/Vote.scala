package models

import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import play.api.mvc.QueryStringBindable.Parsing

/**
 * Vote Kind Trait
 */
trait VoteKind extends EntityKind {

  // The entity types.
  type K = VoteKind
  type E = Vote
  type R = VoteReference
  type C = Vote.type
  type F = VoteFrom
  type P = VotePatch
  type S = VoteSearch
  type M = VoteMapper

  // The entity companion.
  val companion = Vote

  // Convert a vote to a reference.
  def toReference(vote: Vote): VoteReference = VoteReference(vote.id)
}

/**
 * Vote Like Trait
 */
trait VoteLike extends EntityLike with VoteKind

/**
 * Vote Like Companion
 */
object VoteLike {

  // Convert to and from the database.
  implicit val mapType = MappedColumnType.base[VoteLike, Long](
    ref => ref.id.getOrElse(throw EntityReferenceException("Empty entity reference cannot be converted to long.")),
    id => VoteReference(Some(id))
  )

  // Convert to and from the query string.
  implicit object bindType extends Parsing[VoteLike](
    str => VoteReference(id = Some(str.toLong)),
    ref => ref.id.get.toString(),
    (key: String, e: Exception) => "Cannot parse parameter %s as VoteLike: %s".format(key, e.getMessage)
  )

  // Convert to JSON.
  val jsonWrite = new Writes[VoteLike] {
    def writes(v: VoteLike) = v match {
      case ref: VoteReference => Json.toJson(ref)(VoteReference.jsonFormat).as[JsObject]
      case full: Vote => Json.toJson(full)(Vote.jsonFormat).as[JsObject]
    }
  }

  // Convert from JSON.
  val jsonRead = new Reads[VoteLike] {
    def reads(json: JsValue) = {
      json.asOpt[Vote].orElse(json.asOpt[VoteReference]).map(v => JsSuccess(v)).getOrElse(JsError("Could not read VoteLike"))
    }
  }

  // Convert to and from JSON.
  implicit val jsonFormat: Format[VoteLike] = Format(jsonRead, jsonWrite)
}

/**
 * Vote From Class
 *
 * @type {VoteFrom}
 */
case class VoteFrom(val term: TermReference, val context: ContextReference) extends EntityFrom with VoteKind

/**
 * Vote From Companion
 */
object VoteFrom {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[VoteFrom]
}

/**
 * Vote Patch Class
 *
 * @type {VotePatch}
 */
case class VotePatch(
  val visible: Option[Boolean] = None,
  val deleted: Option[Boolean] = None
) extends EntityPatch with VoteKind

/**
 * Vote Patch Companion
 */
object VotePatch {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[VotePatch]
}

/**
 * Vote Search Class
 *
 * @type {VoteSearch}
 */
case class VoteSearch(
  val id: Option[Long] = None,
  val ids: Option[Set[Long]] = None,
  val term: Option[TermLike] = None,
  val context: Option[ContextLike] = None,
  val visible: Option[Boolean] = Some(true),
  val deleted: Option[Boolean] = Some(false),
  val createdBy: Option[UserLike] = None
) extends EntitySearch with VoteKind

/**
 * Vote Search Companion
 */
object VoteSearch {

  // Create a vote search from a connection.
  def fromConnection(connection: Connection): VoteSearch = {
    VoteSearch(term = Some(connection.term), context = Some(connection.context), visible = Some(connection.visible), deleted = Some(connection.deleted))
  }
}

/**
 * Vote Reference Class
 *
 * @type {VoteReference}
 */
case class VoteReference(val id: Option[Long] = None, val from: Option[VoteFrom] = None) extends EntityReference with VoteLike

/**
 * Vote Reference Companion
 */
object VoteReference {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[VoteReference]
}

/**
 * Vote Class
 *
 * @type {Vote}
 */
case class Vote(
  val id: Option[Long] = None,
  val term: TermLike,
  val context: ContextLike,
  val visible: Boolean = true,
  val deleted: Boolean = false,
  val createdAt: DateTime,
  val createdBy: UserLike
) extends Entity with VoteLike with HasCreator with VoteKind

/**
 * Vote Companion
 */
object Vote extends EntityCompanion with VoteKind {

  // Create a reference from a vote.
  def toRef(vote: Vote): VoteReference = VoteReference(vote.id)

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[Vote]
}

