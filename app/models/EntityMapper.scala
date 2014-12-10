package models

import play.api.db.slick.Config.driver.simple._

/**
 * Entity Mapper Trait
 */
trait EntityMapper[K <: EntityKind] {

  // Count the entities matching the search parameters.
  def count(search: K#S)(implicit session: Session): Long

  // Create an entity.
  def create(from: K#F)(implicit session: Session, user: UserLike): K#E

  // Delete an entity.
  def delete(id: Long)(implicit session: Session, user: UserLike): Option[K#E]

  // Delete an entity.
  def delete(id: Option[Long])(implicit session: Session, user: UserLike): Option[K#E] = {
    id.flatMap(delete)
  }

  // Find an entity by ID.
  def find(id: Long)(implicit session: Session): Option[K#E]

  // Find an entity by ID.
  def find(id: Option[Long])(implicit session: Session): Option[K#E] = {
    id.flatMap(find)
  }

  // Find the entities matching the set of IDs.
  def find(ids: Set[Long])(implicit session: Session): Map[Long, K#E]

  // Find the entities matching the search parameters.
  def find(search: K#S)(implicit session: Session): List[K#E]

  // Find an entity matching the search parameters.
  def findOne(search: K#S)(implicit session: Session): Option[K#E]

  // Patch an entity by ID.
  def patch(id: Long, patch: K#P)(implicit session: Session, user: UserLike): Option[K#E]

  // Patch an entity by ID.
  def patch(id: Option[Long], patch: K#P)(implicit session: Session, user: UserLike): Option[K#E] = {
    id.flatMap(this.patch(_, patch))
  }

  // Populate the default references for an entity.
  def populateDefaults(entity: K#E)(implicit session: Session): K#E

  // Populate the default references for a list of entities.
  def populateDefaults(entities: List[K#E])(implicit session: Session): List[K#E]

  // Resolve an entity reference.
  def resolve(ref: K#R)(implicit session: Session, user: UserLike): K#E

  // Convert a collection of entities to a map.
  def toIdMap(col: Iterable[K#E]): Map[Long, K#E] = {
    col.map(e => (e.id.get, e)).toMap
  }
}

/**
 * Entity Creator Mapper Trait
 */
trait EntityCreatorMapper[K <: EntityKind] {
  this: EntityMapper[K] =>

  // Populate the createdBy user for an entity.
  def populateCreatedBy(entity: K#E)(implicit session: Session): K#E
}
