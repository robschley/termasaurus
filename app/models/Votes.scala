package models

import com.github.tototoshi.slick.MySQLJodaSupport._
import play.api.db.slick.Config.driver.simple._

/**
 * Votes Table Class
 *
 * @type {Votes}
 */
class Votes(tag: Tag) extends Table[Vote](tag, "votes") {

  // The table columns.
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def term = column[TermLike]("term", O.NotNull)
  def context = column[ContextLike]("context", O.NotNull)
  def visible = column[Boolean]("visible", O.NotNull, O.Default[Boolean](true))
  def deleted = column[Boolean]("deleted", O.NotNull, O.Default[Boolean](false))
  def createdAt = column[DateTime]("created_at", O.NotNull)
  def createdBy = column[UserLike]("created_by", O.NotNull)

  // The table projections.
  def * = (id, term, context, visible, deleted, createdAt, createdBy) <> (Vote.apply _ tupled, Vote.unapply)
}

/**
 * Votes Mapper
 */
object Votes extends TableQuery(new Votes(_)) with EntityMapper[VoteKind] with EntityCreatorMapper[VoteKind] with VoteKind {

  // Vote query extensions.
  implicit class VoteQueryExtensions(val query: Query[Votes, Vote, Seq]) {

    // Apply filters based on the search parameters.
    def search(search: VoteSearch): Query[Votes, Vote, Seq] = {
      query
        .filterMaybe(search.id, _.id === search.id)
        .filterMaybe(search.ids, _.id inSet search.ids.get)
        .filterMaybe(search.term, _.term === search.term)
        .filterMaybe(search.context, _.context === search.context)
        .filterMaybe(search.visible, _.visible === search.visible)
        .filterMaybe(search.deleted, _.deleted === search.deleted)
        .filterMaybe(search.createdBy, _.createdBy === search.createdBy)
    }
  }

  // Count the votes matching the search parameters.
  def count(search: VoteSearch)(implicit session: Session): Long = {
    Votes.search(search).length.run
  }

  // Create a vote.
  def create(from: VoteFrom)(implicit session: Session, user: UserLike): Vote = {

    // Resolve the references.
    val term = Terms.resolve(from.term)
    val context = Contexts.resolve(from.context)

    // Try to find an active vote for this term and context.
    val existing = findOne(VoteSearch(term = Some(term), context = Some(context), visible = Some(true), deleted = Some(false), createdBy = Some(user)))

    // If an active vote exists, return it. Otherwise, record the vote.
    existing.getOrElse {

      // Convert to a vote.
      val creating: Vote = Vote(
        term = term,
        context = context,
        createdAt = new DateTime(UTC),
        createdBy = user
      )

      // Create the vote.
      val id = Votes.returning(Votes.map(_.id)).insert(creating)
      creating.copy(id = id)

      // If a connection exists for this term and context, update the vote count. Otherwise, create a connection.
      val connection = Connections.findOne(ConnectionSearch.fromVote(creating)).flatMap { connection =>
        Connections.patch(connection.id, ConnectionPatch(votes = Some(Votes.count(VoteSearch.fromConnection(connection)))))
      } getOrElse {
        Connections.create(ConnectionFrom.fromVote(creating))
      }

      // Update the term's connections.
      val updatedTerm = Terms.patch(
        term.id, TermPatch(connections = Some((term.connections + connection)))
      ).getOrElse(throw new RuntimeException("Could not update term when creating vote."))

      // Update the context's connections.
      val updatedContext = Contexts.patch(
        context.id, ContextPatch(connections = Some((context.connections + connection)))
      ).getOrElse(throw new RuntimeException("Could not update context when creating vote."))

      // Update the vote.
      creating.copy(term = updatedTerm, context = updatedContext)
    }
  }

  // Delete a vote.
  def delete(id: Long)(implicit session: Session, user: UserLike): Option[Vote] = {

    // Mark the vote as deleted.
    patch(id, VotePatch(visible = Some(false), deleted = Some(true))).map { vote =>

      // Update the vote count for the connection.
      Connections.findOne(ConnectionSearch.fromVote(vote)).flatMap { connection =>

        // Count the votes.
        val votes = Votes.count(VoteSearch.fromConnection(connection))

        // If the connection still has votes, update the vote count. Otherwise, update and delete.
        if (votes > 0) {
          Connections.patch(connection.id, ConnectionPatch(votes = Some(votes)))
        } else {
          Connections.delete(connection.id)
        }
      }

      vote
    }
  }

  // Find a vote by ID.
  def find(id: Long)(implicit session: Session): Option[Vote] = {
    Votes.filter(_.id === id).firstOption
  }

  // Find the votes matching the set of IDs.
  def find(ids: Set[Long])(implicit session: Session): Map[Long, Vote] = {
    toIdMap(Votes.filter(_.id inSet ids).list)
  }

  // Find the votes matching the search parameters.
  def find(search: VoteSearch)(implicit session: Session): List[Vote] = {
    Votes.search(search).list
  }

  // Find a vote matching the search parameters.
  def findOne(search: VoteSearch)(implicit session: Session): Option[Vote] = {
    Votes.search(search).firstOption
  }

  // Patch a vote by ID.
  def patch(id: Long, patch: VotePatch)(implicit session: Session, user: UserLike): Option[Vote] = {

    // Load the vote.
    find(id).map { vote =>

      // Apply the patch to the vote.
      val patching = vote.copy(
        visible = patch.visible.getOrElse(vote.visible),
        deleted = patch.deleted.getOrElse(vote.deleted)
      )

      // Patch the vote.
      Votes.filter(_.id === id).update(patching)
      patching
    }
  }

  // Populate the context for a vote.
  def populateContext(vote: Vote)(implicit session: Session): Vote = {
    val context = Contexts.find(vote.context.id).getOrElse(vote.context)
    vote.copy(context = context)
  }

  // Populate the context for a list of votes.
  def populateContext(votes: List[Vote])(implicit session: Session): List[Vote] = {
    val contexts = Contexts.find(votes.map(_.context.id).flatten.toSet)
    votes.map(c => c.copy(context = contexts.get(c.context.id.get).getOrElse(c.context)))
  }

  // Populate the createdBy user for a vote.
  def populateCreatedBy(vote: Vote)(implicit session: Session): Vote = {
    val createdBy = Users.find(vote.createdBy.id).getOrElse(vote.createdBy)
    vote.copy(createdBy = createdBy)
  }

  // Populate the createdBy for a list of votes.
  def populateCreatedBy(votes: List[Vote])(implicit session: Session): List[Vote] = {
    val createdBy = Users.find(votes.map(_.createdBy.id).flatten.toSet)
    votes.map(vote => vote.copy(createdBy = createdBy.get(vote.createdBy.id.get).getOrElse(vote.createdBy)))
  }

  // Populate the default references for a vote.
  def populateDefaults(vote: Vote)(implicit session: Session): Vote = {
    populateCreatedBy(populateContext(populateTerm(vote)))
  }

  // Populate the default references for a list of votes.
  def populateDefaults(votes: List[Vote])(implicit session: Session): List[Vote] = {
    populateCreatedBy(populateContext(populateTerm(votes)))
  }

  // Populate the term for a vote.
  def populateTerm(vote: Vote)(implicit session: Session): Vote = {
    val term = Terms.find(vote.term.id).getOrElse(vote.term)
    vote.copy(term = term)
  }

  // Populate the term for a list of votes.
  def populateTerm(votes: List[Vote])(implicit session: Session): List[Vote] = {
    val terms = Terms.find(votes.map(_.term.id).flatten.toSet)
    votes.map(c => c.copy(term = terms.get(c.term.id.get).getOrElse(c.term)))
  }

  // Resolve a vote reference.
  def resolve(ref: VoteReference)(implicit session: Session, user: UserLike): Vote = {
    ref.id.flatMap(find).orElse(ref.from.map(create)).getOrElse(throw EntityReferenceException("Could not resolve vote reference."))
  }
}
