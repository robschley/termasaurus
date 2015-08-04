package models

import akka.actor._
import akka.routing.RoundRobinGroup
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.Future

/**
 * Connection Mapper Service
 */
trait ConnectionMapperService extends EntityMapperService[ConnectionKind] {

  // The terms mapper.
  val mapper = Connections

  // Populate the context for a connection.
  def populateContext(connection: Connection): Future[Connection] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateContext(connection)
      }
    }
  }

  // Populate the context user for a list of connections.
  def populateContext(connections: List[Connection]): Future[List[Connection]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateContext(connections)
      }
    }
  }

  // Populate the createdBy user for a connection.
  def populateCreatedBy(connection: Connection): Future[Connection] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateCreatedBy(connection)
      }
    }
  }

  // Populate the createdBy user for a list of connections.
  def populateCreatedBy(connections: List[Connection]): Future[List[Connection]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateCreatedBy(connections)
      }
    }
  }

  // Populate the default references for a connection.
  def populateDefaults(connection: Connection): Future[Connection] = {

    // Get the individual objects.
    val futureContext = populateContext(connection)
    val futureCreatedBy = populateCreatedBy(connection)

    // Populate the objects.
    futureContext.flatMap { connectionWithContext =>
      futureCreatedBy.map { connectionWithCreatedBy =>
        connection.copy(context = connectionWithContext.context, createdBy = connectionWithCreatedBy.createdBy)
      }
    }
  }

  // Populate the default references for a list of connections.
  def populateDefaults(connections: List[Connection]): Future[List[Connection]] = {

    // Load the references.
    val futureContext = populateContext(connections)
    val futureTerm = populateTerm(connections)
    val futureCreatedBy = populateCreatedBy(connections)

    // Populate the objects.
    futureContext.flatMap { connectionsWithContext =>
      futureTerm.flatMap { connectionsWithTerm =>
        futureCreatedBy.map { connectionsWithCreatedBy =>

          // Create maps of all the values.
          val connectionMap = toIdMap(connections)
          val contextMap = toIdMap(connectionsWithContext)
          val termMap = toIdMap(connectionsWithTerm)
          val createdByMap = toIdMap(connectionsWithCreatedBy)

          // Combine the values.
          connectionMap.map { case (id, connection) => connection.copy(
              context = contextMap.get(id).map(_.context).getOrElse(connection.context),
              term = termMap.get(id).map(_.term).getOrElse(connection.term),
              createdBy = createdByMap.get(id).map(_.createdBy).getOrElse(connection.createdBy)
            )
          } toList
        }
      }
    }
  }

  // Populate the term for a connection.
  def populateTerm(connection: Connection): Future[Connection] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateTerm(connection)
      }
    }
  }

  // Populate the term user for a list of connections.
  def populateTerm(connections: List[Connection]): Future[List[Connection]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateTerm(connections)
      }
    }
  }
}

/**
 * Connection Service Class
 */
class ConnectionService(val system: ActorSystem) extends ConnectionMapperService {

  // The execution connection.
  implicit val ec = system.dispatcher
}

/**
 * Connection Service Companion
 */
object ConnectionService {

  // Create a connection service backed by a single actor.
  def create(system: ActorSystem): ConnectionMapperService = {
    TypedActor(system).typedActorOf(TypedProps(classOf[ConnectionMapperService], new ConnectionService(system)))
  }

  // Create a connection service backed by a pool of actors.
  def pool(system: ActorSystem, count: Int = 5) = {

    // Prepare the actors in the pool.
    val routees: List[ConnectionMapperService] = List.fill(count) { create(system) }
    val routeePaths = routees map { r =>
      TypedActor(system).getActorRefFor(r).path.toStringWithoutAddress
    }

    // Prepare the pool router.
    val router: ActorRef = system.actorOf(RoundRobinGroup(routeePaths).props())
    val typedRouter: ConnectionMapperService = TypedActor(system).typedActorOf(TypedProps(classOf[ConnectionMapperService], new ConnectionService(system)), actorRef = router)
    typedRouter
  }
}
