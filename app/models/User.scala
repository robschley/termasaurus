package models

import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import play.api.mvc.QueryStringBindable.Parsing

/**
 * User Kind Trait
 */
trait UserKind extends EntityKind {

  // The entity types.
  type E = User
  type R = UserReference
  type C = User.type
  type F = UserFrom
  type P = UserPatch
  type S = UserSearch

  // The entity companion.
  val companion = User

  // Convert a user to a reference.
  def toReference(user: User): UserReference = UserReference(user.id)
}

/**
 * User Like Trait
 */
trait UserLike extends EntityLike with UserKind

/**
 * User Like Companion
 */
object UserLike {

  // Convert to and from the database.
  implicit val mapType = MappedColumnType.base[UserLike, Long](
    ref => ref.id.getOrElse(throw EntityReferenceException("Empty entity reference cannot be converted to long.")),
    id => UserReference(Some(id))
  )

  // Convert to and from the query string.
  implicit object bindType extends Parsing[UserLike](
    str => UserReference(id = Some(str.toLong)),
    ref => ref.id.get.toString(),
    (key: String, e: Exception) => "Cannot parse parameter %s as UserLike: %s".format(key, e.getMessage)
  )

  // Convert to JSON.
  val jsonWrite = new Writes[UserLike] {
    def writes(v: UserLike) = v match {
      case ref: UserReference => Json.toJson(ref)(UserReference.jsonFormat).as[JsObject]
      case full: User => Json.toJson(full)(User.jsonFormat).as[JsObject]
    }
  }

  // Convert from JSON.
  val jsonRead = new Reads[UserLike] {
    def reads(json: JsValue) = {
      json.asOpt[User].orElse(json.asOpt[UserReference]).map(v => JsSuccess(v)).getOrElse(JsError("Could not read UserLike"))
    }
  }

  // Convert to and from JSON.
  implicit val jsonFormat: Format[UserLike] = Format(jsonRead, jsonWrite)
}

/**
 * User From Class
 *
 * @type {UserFrom}
 */
case class UserFrom(val name: String, val username: String) extends EntityFrom with UserKind

/**
 * User From Companion
 */
object UserFrom {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[UserFrom]
}

/**
 * User Patch Class
 *
 * @type {UserPatch}
 */
case class UserPatch(
  val name: Option[String] = None,
  val visible: Option[Boolean] = None,
  val deleted: Option[Boolean] = None
) extends EntityPatch with UserKind

/**
 * User Patch Companion
 */
object UserPatch {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[UserPatch]
}

/**
 * User Search Class
 *
 * @type {UserSearch}
 */
case class UserSearch(
  val id: Option[Long] = None,
  val ids: Option[Set[Long]] = None,
  val name: Option[String] = None,
  val username: Option[String] = None,
  val visible: Option[Boolean] = Some(true),
  val deleted: Option[Boolean] = Some(false)
) extends EntitySearch with UserKind

/**
 * User Reference Class
 *
 * @type {UserReference}
 */
case class UserReference(val id: Option[Long] = None, val from: Option[UserFrom] = None) extends EntityReference with UserLike

/**
 * User Reference Companion
 */
object UserReference {

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[UserReference]
}

/**
 * User Class
 *
 * @type {User}
 */
case class User(
  val id: Option[Long] = None,
  val name: String,
  val username: String,
  val visible: Boolean = true,
  val deleted: Boolean = false,
  val createdAt: DateTime
) extends Entity with UserLike with UserKind

/**
 * User Companion
 */
object User extends EntityCompanion with UserKind {

  // Create a reference from a user.
  def toRef(user: User): UserReference = UserReference(user.id)

  // Convert to and from JSON.
  implicit val jsonFormat = Json.format[User]
}
