package models

import akka.actor._
import akka.routing.RoundRobinGroup
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.Future

/**
 * Context Mapper Service
 */
trait ContextMapperService extends EntityMapperService[ContextKind] {

  // The contexts mapper.
  val mapper = Contexts

  // The connection service.
  val connectionService: ConnectionMapperService

  // Populate the connections for a context.
  def populateConnections(context: Context): Future[Context] = {
    connectionService.find(context.connections).flatMap { connections =>
      connectionService.populateDefaults(connections.values.toList).map { connections =>
        context.copy(connections = connections.toSet)
      }
    }
  }

  // Populate the connections user for a list of contexts.
  def populateConnections(contexts: List[Context]): Future[List[Context]] = {
    connectionService.find(contexts.map(_.connections.map(_.id).flatten).flatten.toSet).flatMap { connections =>
      connectionService.populateDefaults(connections.values.toList).map { connections =>
        val connectionsMap = connectionService.toIdMap(connections)
        contexts.map(context => context.copy(connections = context.connections.map(c => connectionsMap.get(c.id.get).getOrElse(c))))
      }
    }
  }

  // Populate the createdBy user for a context.
  def populateCreatedBy(context: Context): Future[Context] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateCreatedBy(context)
      }
    }
  }

  // Populate the createdBy user for a list of contexts.
  def populateCreatedBy(contexts: List[Context]): Future[List[Context]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateCreatedBy(contexts)
      }
    }
  }

  // Populate the default references for a context.
  def populateDefaults(context: Context): Future[Context] = {

    // Get the individual objects.
    val futureConnections = populateConnections(context)
    val futureCreatedBy = populateCreatedBy(context)

    // Populate the objects.
    futureConnections.flatMap { contextWithConnections =>
      futureCreatedBy.map { contextWithCreatedBy =>
        context.copy(connections = contextWithConnections.connections, createdBy = contextWithCreatedBy.createdBy)
      }
    }
  }

  // Populate the default references for a list of contexts.
  def populateDefaults(contexts: List[Context]): Future[List[Context]] = {

    // Load the references.
    val futureConnections = populateConnections(contexts)
    val futureCreatedBy = populateCreatedBy(contexts)

    // Populate the objects.
    futureConnections.flatMap { contextsWithConnections =>
      futureCreatedBy.map { contextsWithCreatedBy =>

        // Create maps of all the values.
        val contextMap = toIdMap(contexts)
        val connectionMap = toIdMap(contextsWithConnections)
        val createdByMap = toIdMap(contextsWithCreatedBy)

        // Combine the values.
        contextMap.map({
          case (id, context) => context.copy(
            connections = connectionMap.get(id).map(_.connections).getOrElse(context.connections),
            createdBy = createdByMap.get(id).map(_.createdBy).getOrElse(context.createdBy)
          )
        }).toList
      }
    }
  }
}

/**
 * Context Service
 */
class ContextService(val system: ActorSystem) extends ContextMapperService {

  // The execution context.
  implicit val ec = system.dispatcher

  // The connection service.
  val connectionService = ConnectionService.create(system)
}

/**
 * Context Service Companion
 */
object ContextService {

  // Create a context service backed by a single actor.
  def create(system: ActorSystem): ContextMapperService = {
    TypedActor(system).typedActorOf(TypedProps(classOf[ContextMapperService], new ContextService(system)))
  }

  // Create a context service backed by a pool of actors.
  def pool(system: ActorSystem, count: Int = 5) = {

    // Prepare the actors in the pool.
    val routees: List[ContextMapperService] = List.fill(count) { create(system) }
    val routeePaths = routees map { r =>
      TypedActor(system).getActorRefFor(r).path.toStringWithoutAddress
    }

    // Prepare the pool router.
    val router: ActorRef = system.actorOf(RoundRobinGroup(routeePaths).props())
    val typedRouter: ContextMapperService = TypedActor(system).typedActorOf(TypedProps(classOf[ContextMapperService], new ContextService(system)), actorRef = router)
    typedRouter
  }
}
