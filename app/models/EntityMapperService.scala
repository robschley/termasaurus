package models

import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.Play.current
import scala.concurrent.{ ExecutionContext, Future }

/**
 * Entity Mapper Service Trait
 */
trait EntityMapperService[K <: EntityKind] {

  // The execution context for the futures.
  implicit val ec: ExecutionContext

  // The entity mapper.
  val mapper: EntityMapper[K]

  // Count the entities matching the search parameters.
  def count(search: K#S): Future[Long] = {
    Future {
      DB.withSession { implicit session =>
        mapper.count(search)
      }
    }
  }

  // Create an entity.
  def create(from: K#F)(implicit user: UserLike): Future[K#E] = {
    Future {
      DB.withTransaction { implicit session =>
        mapper.create(from)
      }
    }
  }

  // Delete an entity.
  def delete(id: Long)(implicit user: UserLike): Future[Option[K#E]] = {
    Future {
      DB.withTransaction { implicit session =>
        mapper.delete(id)
      }
    }
  }

  // Delete an entity.
  def delete(id: Option[Long])(implicit user: UserLike): Future[Option[K#E]] = {
    Future {
      DB.withTransaction { implicit session =>
        mapper.delete(id)
      }
    }
  }

  // Find an entity by ID.
  def find(id: Long): Future[Option[K#E]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.find(id)
      }
    }
  }

  // Find an entity by ID.
  def find(id: Option[Long]): Future[Option[K#E]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.find(id)
      }
    }
  }

  // Find the entities matching the set of IDs.
  def find(ids: Set[Long]): Future[Map[Long, K#E]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.find(ids)
      }
    }
  }

  // Find the entities matching the search parameters.
  def find(search: K#S): Future[List[K#E]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.find(search)
      }
    }
  }

  // Find an entity matching the search parameters.
  def findOne(search: K#S): Future[Option[K#E]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.findOne(search)
      }
    }
  }

  // Patch an entity by ID.
  def patch(id: Long, patch: K#P)(implicit user: UserLike): Future[Option[K#E]] = {
    Future {
      DB.withTransaction { implicit session =>
        mapper.patch(id, patch)
      }
    }
  }

  // Patch an entity by ID.
  def patch(id: Option[Long], patch: K#P)(implicit user: UserLike): Future[Option[K#E]] = {
    Future {
      DB.withTransaction { implicit session =>
        mapper.patch(id, patch)
      }
    }
  }

  // Populate the default references for an entity.
  def populateDefaults(entity: K#E): Future[K#E]

  // Populate the default references for a list of entities.
  def populateDefaults(entities: List[K#E]): Future[List[K#E]]

  // Convert a collection of entities to a map.
  def toIdMap(col: Iterable[K#E]): Map[Long, K#E] = mapper.toIdMap(col)
}
