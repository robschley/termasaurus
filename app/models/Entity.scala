package models

import play.api.libs.json._

/**
 * Term Like Trait
 */
trait EntityLike extends HasPrimaryId {
  this: EntityKind =>
}

/**
 * Entity Reference Trait
 */
trait EntityReference extends EntityLike {
  this: EntityKind =>

  // Create a reference to a future entity.
  val from: Option[F]
}

/**
 * Entity Reference Exception Class
 *
 * @type {EntityReferenceException}
 */
case class EntityReferenceException(message: String) extends RuntimeException(message)

/**
 * Entity From Trait
 */
trait EntityFrom {
  this: EntityKind =>
}

/**
 * Entity Patch Trait
 */
trait EntityPatch {
  this: EntityKind =>
}

/**
 * Entity Search Trait
 */
trait EntitySearch {
  this: EntityKind =>
}

/**
 * Entity Revision Trait
 */
trait EntityRevision extends HasPrimaryId with HasCreator {
  this: EntityKind =>

  // The target entity.
  val target: E

  // The patch used to apply changes.
  val apply: P

  // The patch used to revert changes.
  val revert: P
}

/**
 * Entity Trait
 */
trait Entity extends EntityLike {
  this: EntityKind =>

  val visible: Boolean
  val deleted: Boolean
}

/**
 * Entity Companion Trait
 */
trait EntityCompanion {
  this: EntityKind =>

  // Create a reference from an entity.
  def toRef(entity: E): R

  // Convert to and from JSON.
  val jsonFormat: Format[E]
}

/**
 * Entity Kind Trait
 */
trait EntityKind {

  // The entity types.
  type E <: Entity
  type K <: EntityKind
  type R <: EntityReference
  type C <: EntityCompanion
  type F <: EntityFrom
  type P <: EntityPatch
  type S <: EntitySearch
  type M <: EntityMapper[K]

  // The entity companion.
  val companion: C

  // Create a reference from an entity.
  def toReference(entity: E): R
}
