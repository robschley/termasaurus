package models

import akka.actor._
import akka.routing.RoundRobinGroup
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.Future

/**
 * Term Mapper Service
 */
trait TermMapperService extends EntityMapperService[TermKind] {

  // The terms mapper.
  val mapper = Terms

  // The connection service.
  val connectionService: ConnectionMapperService

  // Populate the connections for a term.
  def populateConnections(term: Term): Future[Term] = {
    connectionService.find(term.connections).flatMap { connections =>
      connectionService.populateDefaults(connections.values.toList).map { connections =>
        term.copy(connections = connections.toSet)
      }
    }
  }

  // Populate the connections user for a list of terms.
  def populateConnections(terms: List[Term]): Future[List[Term]] = {
    connectionService.find(terms.map(_.connections.map(_.id).flatten).flatten.toSet).flatMap { connections =>
      connectionService.populateDefaults(connections.values.toList).map { connections =>
        val connectionsMap = connectionService.toIdMap(connections)
        terms.map(term => term.copy(connections = term.connections.map(c => connectionsMap.get(c.id.get).getOrElse(c))))
      }
    }
  }

  // Populate the createdBy user for a term.
  def populateCreatedBy(term: Term): Future[Term] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateCreatedBy(term)
      }
    }
  }

  // Populate the createdBy user for a list of terms.
  def populateCreatedBy(terms: List[Term]): Future[List[Term]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateCreatedBy(terms)
      }
    }
  }

  // Populate the default references for a term.
  def populateDefaults(term: Term): Future[Term] = {

    // Get the individual objects.
    val futureConnections = populateConnections(term)
    val futureCreatedBy = populateCreatedBy(term)

    // Populate the objects.
    futureConnections.flatMap { termWithConnections =>
      futureCreatedBy.map { termWithCreatedBy =>
        term.copy(connections = termWithConnections.connections, createdBy = termWithCreatedBy.createdBy)
      }
    }
  }

  // Populate the default references for a list of terms.
  def populateDefaults(terms: List[Term]): Future[List[Term]] = {

    // Load the references.
    val futureConnections = populateConnections(terms)
    val futureCreatedBy = populateCreatedBy(terms)

    // Populate the objects.
    futureConnections.flatMap { termsWithConnections =>
      futureCreatedBy.map { termsWithCreatedBy =>

        // Create maps of all the values.
        val termMap = toIdMap(terms)
        val connectionMap = toIdMap(termsWithConnections)
        val createdByMap = toIdMap(termsWithCreatedBy)

        // Combine the values.
        termMap.map({
          case (id, term) => term.copy(
            connections = connectionMap.get(id).map(_.connections).getOrElse(term.connections),
            createdBy = createdByMap.get(id).map(_.createdBy).getOrElse(term.createdBy)
          )
        }).toList
      }
    }
  }
}

/**
 * Term Service
 */
class TermService(val system: ActorSystem) extends TermMapperService {

  // The execution context.
  implicit val ec = system.dispatcher

  // The connection service.
  val connectionService = ConnectionService.create(system)
}

/**
 * Term Service Companion
 */
object TermService {

  // Create a term service backed by a single actor.
  def create(system: ActorSystem): TermMapperService = {
    TypedActor(system).typedActorOf(TypedProps(classOf[TermMapperService], new TermService(system)))
  }

  // Create a term service backed by a pool of actors.
  def pool(system: ActorSystem, count: Int = 5) = {

    // Prepare the actors in the pool.
    val routees: List[TermMapperService] = List.fill(count) { create(system) }
    val routeePaths = routees map { r =>
      TypedActor(system).getActorRefFor(r).path.toStringWithoutAddress
    }

    // Prepare the pool router.
    val router: ActorRef = system.actorOf(RoundRobinGroup(routeePaths).props())
    val typedRouter: TermMapperService = TypedActor(system).typedActorOf(TypedProps(classOf[TermMapperService], new TermService(system)), actorRef = router)
    typedRouter
  }
}
