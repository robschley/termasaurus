package models

import akka.actor._
import akka.routing.RoundRobinGroup
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.Future

/**
 * User Mapper Service
 */
trait UserMapperService extends EntityMapperService[UserKind] {

  // The users mapper.
  val mapper = Users

  // Authenticate a user.
  def authenticate(username: String, password: String): Future[Option[User]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.authenticate(username, password)
      }
    }
  }

  // Find a user matching the username.
  def findOne(username: String): Future[Option[User]] = {
    findOne(UserSearch(username = Some(username)))
  }

  // Populate the default references for a user.
  def populateDefaults(user: User): Future[User] = {
    Future.successful(user)
  }

  // Populate the default references for a list of users.
  def populateDefaults(users: List[User]): Future[List[User]] = {
    Future.successful(users)
  }
}

/**
 * User Service
 */
class UserService(val system: ActorSystem) extends UserMapperService {

  // The execution context.
  implicit val ec = system.dispatcher
}

/**
 * User Service Companion
 */
object UserService {

  // Create a user service backed by a single actor.
  def create(system: ActorSystem): UserMapperService = {
    TypedActor(system).typedActorOf(TypedProps(classOf[UserMapperService], new UserService(system)))
  }

  // Create a user service backed by a pool of actors.
  def pool(system: ActorSystem, count: Int = 5) = {

    // Prepare the actors in the pool.
    val routees: List[UserMapperService] = List.fill(count) { create(system) }
    val routeePaths = routees map { r =>
      TypedActor(system).getActorRefFor(r).path.toStringWithoutAddress
    }

    // Prepare the pool router.
    val router: ActorRef = system.actorOf(RoundRobinGroup(routeePaths).props())
    val typedRouter: UserMapperService = TypedActor(system).typedActorOf(TypedProps(classOf[UserMapperService], new UserService(system)), actorRef = router)
    typedRouter
  }
}
