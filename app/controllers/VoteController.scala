package controllers

import models._
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.Future

/**
 * Vote Controller
 */
object VoteController extends EntityController[VoteKind] {

  // The entity service.
  val service = VoteService.pool(Akka.system)

  // Convert to and from JSON.
  implicit val jsonFormat = Vote.jsonFormat
  implicit val jsonFormatFrom = VoteFrom.jsonFormat
  implicit val jsonFormatPatch = VotePatch.jsonFormat

  // Find a list of votes matching the search parameters.
  def query(
    term: Option[TermLike] = None,
    context: Option[ContextLike] = None,
    visible: Boolean = true,
    deleted: Boolean = false,
    createdBy: Option[UserLike] = None
  ): Action[AnyContent] = find(VoteSearch(term = term, context = context, visible = Some(visible), deleted = Some(deleted), createdBy = createdBy))
}
