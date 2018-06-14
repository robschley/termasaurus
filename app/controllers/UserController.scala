package controllers

import actions._
import models._
import play.api.data.validation.ValidationError
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * User Controller
 */
object UserController extends EntityController[UserKind] {

  // The user service.
  val service = UserService.pool(Akka.system)

  // Convert to and from JSON.
  implicit val jsonFormat = User.jsonFormat
  implicit val jsonFormatFrom = UserFrom.jsonFormat
  implicit val jsonFormatPatch = UserPatch.jsonFormat

  // Create a user.
  override def create() = Action.async(parse.json) { request =>

    // Creating a user does not require a user reference.
    implicit val user: UserLike = UserReference(None)

    // Create the user.
    request.body
      .validate[UserFrom]
      // .filter(ValidationError("Invalid email address"))((a: UserFrom) => false)
      .map { from =>
        service.create(from).flatMap(e => populateDefaults(e).map(e => Ok(Json.toJson(e)))) recover {
          case err: UsernameExistsException => Conflict(Json.toJson(err))
        }
      } recoverTotal { errors =>
        Future.successful(BadRequest(Json.toJson(errors)))
      }
  }

  // Get the authenticated user.
  def find() = Authenticated.async { request =>
    service.find(request.user.id).flatMap(_.map(e => populateDefaults(e).map(e => Ok(Json.toJson(e)))).getOrElse(Future.successful(NotFound)))
  }

  // Get an entity.
  override def find(id: Long) = Authenticated.async { request =>
    Future.successful(Forbidden)
  }

  // Get a list of entities that match the search parameters.
  override def find(search: UserSearch) = Authenticated.async { request =>
    Future.successful(Forbidden)
  }

  // Delete the authenticated user.
  def delete() = Authenticated.async(parse.json) { request =>
    implicit val user = request.user
    service.delete(user.id).map(_.map(e => NoContent).getOrElse(NotFound))
  }

  // Patch the authenticated user.
  def patch() = Authenticated.async(parse.json) { request =>
    implicit val user = request.user
    request.body.validate[UserPatch].map { patch =>
      service.patch(user.id, patch).flatMap(_.map(e => populateDefaults(e).map(e => Ok(Json.toJson(e)))).getOrElse(Future.successful(NotFound)))
    } recoverTotal { errors =>
      Future.successful(BadRequest(Json.toJson(errors)))
    }
  }

  // Find a list of users matching the search parameters.
  def query(
    visible: Boolean = true,
    deleted: Boolean = false
  ): Action[AnyContent] = find(UserSearch(visible = Some(visible), deleted = Some(deleted)))
}
