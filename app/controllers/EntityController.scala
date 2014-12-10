package controllers

import models._
import play.api._
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Entity Controller Trait
 */
trait EntityController[K <: EntityKind] extends Controller {

  // The entity service.
  val service: EntityMapperService[K]

  // Convert to and from JSON.
  implicit val jsonFormat: Format[K#E]
  implicit val jsonFormatFrom: Format[K#F]
  implicit val jsonFormatPatch: Format[K#P]

  // Create an entity.
  def create() = Action.async(parse.json) { request =>

    implicit val user: UserLike = UserReference(Some(42)) // TODO: Fix this.

    request.body.validate[K#F].map { from =>
      service.create(from).flatMap(e => populateDefaults(e).map(e => Ok(Json.toJson(e))))
    } recoverTotal { errors =>
      Future.successful(BadRequest(JsError.toFlatJson(errors)))
    }
  }

  // Delete an entity.
  def delete(id: Long) = Action.async(parse.json) { request =>

    implicit val user: UserLike = UserReference(Some(42)) // TODO: Fix this.

    service.delete(id: Long).map(_.map(e => NoContent).getOrElse(NotFound))
  }

  // Get an entity.
  def find(id: Long) = Action.async { request =>
    service.find(id).flatMap(_.map(e => populateDefaults(e).map(e => Ok(Json.toJson(e)))).getOrElse(Future.successful(NotFound)))
  }

  // Get a list of entities that match the search parameters.
  def find(search: K#S) = Action.async { request =>
    service.find(search).flatMap { list =>
      populateDefaults(list).map { list =>
        Ok(Json.toJson(list))
      }
    }
  }

  // Patch an entity.
  def patch(id: Long) = Action.async(parse.json) { request =>

    implicit val user: UserLike = UserReference(Some(42)) // TODO: Fix this.

    request.body.validate[K#P].map { patch =>
      service.patch(id, patch).flatMap(_.map(e => populateDefaults(e).map(e => Ok(Json.toJson(e)))).getOrElse(Future.successful(NotFound)))
    } recoverTotal { errors =>
      Future.successful(BadRequest(JsError.toFlatJson(errors)))
    }
  }

  // Populate the default references.
  def populateDefaults(entity: K#E): Future[K#E] = {
    service.populateDefaults(entity)
  }

  // Populate the default references.
  def populateDefaults(entities: List[K#E]): Future[List[K#E]] = {
    service.populateDefaults(entities)
  }
}
