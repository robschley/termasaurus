package models

import akka.actor._
import akka.routing.RoundRobinGroup
import play.api.db.slick.Config.driver.simple._
import play.api.db.slick.DB
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.Future

/**
 * Vote Mapper Service
 */
trait VoteMapperService extends EntityMapperService[VoteKind] {

  // The votes mapper.
  val mapper = Votes

  // Populate the context for a vote.
  def populateContext(vote: Vote): Future[Vote] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateContext(vote)
      }
    }
  }

  // Populate the context user for a list of votes.
  def populateContext(votes: List[Vote]): Future[List[Vote]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateContext(votes)
      }
    }
  }

  // Populate the createdBy user for a vote.
  def populateCreatedBy(vote: Vote): Future[Vote] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateCreatedBy(vote)
      }
    }
  }

  // Populate the createdBy user for a list of votes.
  def populateCreatedBy(votes: List[Vote]): Future[List[Vote]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateCreatedBy(votes)
      }
    }
  }

  // Populate the default references for a vote.
  def populateDefaults(vote: Vote): Future[Vote] = {

    // Get the individual objects.
    val futureContext = populateContext(vote)
    val futureTerm = populateTerm(vote)
    val futureCreatedBy = populateCreatedBy(vote)

    // Populate the objects.
    futureContext.flatMap { voteWithContext =>
      futureTerm.flatMap { voteWithTerm =>
        futureCreatedBy.map { voteWithCreatedBy =>
          vote.copy(context = voteWithContext.context, term = voteWithTerm.term, createdBy = voteWithCreatedBy.createdBy)
        }
      }
    }
  }

  // Populate the default references for a list of votes.
  def populateDefaults(votes: List[Vote]): Future[List[Vote]] = {

    // Load the references.
    val futureContext = populateContext(votes)
    val futureTerm = populateTerm(votes)
    val futureCreatedBy = populateCreatedBy(votes)

    // Populate the objects.
    futureContext.flatMap { votesWithContext =>
      futureTerm.flatMap { votesWithTerm =>
        futureCreatedBy.map { votesWithCreatedBy =>

          // Create maps of all the values.
          val voteMap = toIdMap(votes)
          val contextMap = toIdMap(votesWithContext)
          val termMap = toIdMap(votesWithTerm)
          val createdByMap = toIdMap(votesWithCreatedBy)

          // Combine the values.
          voteMap.map({
            case (id, vote) => vote.copy(
              context = contextMap.get(id).map(_.context).getOrElse(vote.context),
              term = termMap.get(id).map(_.term).getOrElse(vote.term),
              createdBy = createdByMap.get(id).map(_.createdBy).getOrElse(vote.createdBy)
            )
          }).toList
        }
      }
    }
  }

  // Populate the term for a vote.
  def populateTerm(vote: Vote): Future[Vote] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateTerm(vote)
      }
    }
  }

  // Populate the term user for a list of votes.
  def populateTerm(votes: List[Vote]): Future[List[Vote]] = {
    Future {
      DB.withSession { implicit session =>
        mapper.populateTerm(votes)
      }
    }
  }
}

/**
 * Vote Service
 */
class VoteService(val system: ActorSystem) extends VoteMapperService {

  // The execution context.
  implicit val ec = system.dispatcher
}

/**
 * Vote Service Companion
 */
object VoteService {

  // Create a vote service backed by a single actor.
  def create(system: ActorSystem): VoteMapperService = {
    TypedActor(system).typedActorOf(TypedProps(classOf[VoteMapperService], new VoteService(system)))
  }

  // Create a vote service backed by a pool of actors.
  def pool(system: ActorSystem, count: Int = 5) = {

    // Prepare the actors in the pool.
    val routees: List[VoteMapperService] = List.fill(count) { create(system) }
    val routeePaths = routees map { r =>
      TypedActor(system).getActorRefFor(r).path.toStringWithoutAddress
    }

    // Prepare the pool router.
    val router: ActorRef = system.actorOf(RoundRobinGroup(routeePaths).props())
    val typedRouter: VoteMapperService = TypedActor(system).typedActorOf(TypedProps(classOf[VoteMapperService], new VoteService(system)), actorRef = router)
    typedRouter
  }
}
