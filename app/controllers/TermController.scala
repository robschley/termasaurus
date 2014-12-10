package controllers

import models._
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.Play.current
import scala.concurrent.Future

/**
 * Term Controller
 */
object TermController extends EntityController[TermKind] {

  // The entity service.
  val service = TermService.pool(Akka.system)

  // Convert to and from JSON.
  implicit val jsonFormat = Term.jsonFormat
  implicit val jsonFormatFrom = TermFrom.jsonFormat
  implicit val jsonFormatPatch = TermPatch.jsonFormat

  // Find a list of terms matching the search parameters.
  def query(
    name: Option[String] = None,
    slug: Option[String] = None,
    visible: Boolean = true,
    deleted: Boolean = false,
    createdBy: Option[UserLike] = None
  ): Action[AnyContent] = find(TermSearch(name = name, slug = slug, visible = Some(visible), deleted = Some(deleted), createdBy = createdBy))
}
