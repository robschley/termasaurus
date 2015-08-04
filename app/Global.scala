
import models._
import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.mvc.Results._
import scala.concurrent.Future

object Global extends GlobalSettings {

  // Handle thrown exceptions.
  override def onError(request: RequestHeader, ex: Throwable) = {
    ex match {
      case e: UsernameExistsException => Future.successful(Conflict(Json.toJson(e)))
      case e => Future.successful(InternalServerError(Json.toJson(e)))
    }
  }
}
