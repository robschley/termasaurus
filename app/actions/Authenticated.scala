package actions

import models._
import play.api.libs.concurrent.Akka
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Play.current
import scala.concurrent.Future
import util.Base64
// import play.api.mvc.Security._

/**
 * Authenticated Request Class
 */
case class AuthenticatedRequest[A](val user: User, val request: Request[A]) extends WrappedRequest[A](request)

/**
 * Authenticated Action Builder
 */
object Authenticated extends ActionBuilder[AuthenticatedRequest] {

  // Create a user service pool to handle authentication.
  val service = UserService.pool(Akka.system, 10)

  // Import an execution context.
  implicit val ec = Akka.system.dispatcher

  // Authentication patterns.
  val basic = """Basic (.*)""".r

  def invokeBlock[A](request: Request[A], block: (AuthenticatedRequest[A]) => Future[Result]) = {

    // Get the authorization header.
    request.headers.get("Authorization").map { auth =>

      // Match the authentication mechanism.
      auth match {

        // Check for Basic authentication.
        case basic(encoded) => {

          // Decode the credentials and split.
          Base64.decode(encoded).split(':') match {
            case Array(username: String, password: String) => {

              // Authenticate the supplied credentials and either execute the block or return an unauthorized response.
              service.authenticate(username, password).flatMap { maybeUser =>
                maybeUser match {
                  case Some(user) => block(AuthenticatedRequest(user, request))
                  case None => Future.successful(Unauthorized("Invalid username/password."))
                }
              }
            }
            case _ => Future.successful(Unauthorized("Could not parse Basic Authentication header"))
          }
        }

        case _ => Future.successful(Unauthorized("Unsupported authentication mechanism"))
      }

    } getOrElse {
      // TODO: Make error message JSON?
      Future.successful(Unauthorized("You must log in."))
    }
  }
}
