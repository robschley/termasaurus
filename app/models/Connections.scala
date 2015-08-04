 package models

import com.github.tototoshi.slick.MySQLJodaSupport._
import play.api.db.slick.Config.driver.simple._

/**
 * Connections Table Class
 *
 * @type {Connections}
 */
class Connections(tag: Tag) extends Table[Connection](tag, "connections") {

  // The table columns.
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def term = column[TermLike]("term", O.NotNull)
  def context = column[ContextLike]("context", O.NotNull)
  def visible = column[Boolean]("visible", O.NotNull, O.Default[Boolean](true))
  def deleted = column[Boolean]("deleted", O.NotNull, O.Default[Boolean](false))
  def createdAt = column[DateTime]("created_at", O.NotNull)
  def createdBy = column[UserLike]("created_by", O.NotNull)
  def votes = column[Long]("votes", O.NotNull, O.Default[Long](0L))

  // The table projections.
  def * = (id, term, context, visible, deleted, createdAt, createdBy, votes) <> (Connection.apply _ tupled, Connection.unapply)
}

/**
 * Connections Mapper Class
 */
class ConnectionMapper extends TableQuery(new Connections(_)) with EntityMapper[ConnectionKind] with EntityCreatorMapper[ConnectionKind] {

  // Connection query extensions.
  implicit class ConnectionQueryExtensions(val query: Query[Connections, Connection, Seq]) {

    // Apply filters based on the search parameters.
    def search(search: ConnectionSearch): Query[Connections, Connection, Seq] = {
      query
        .filterMaybe(search.id, _.id === search.id)
        .filterMaybe(search.ids, _.id inSet search.ids.getOrElse(Set.empty[Long]))
        .filterMaybe(search.term, _.term === search.term)
        .filterMaybe(search.context, _.context === search.context)
        .filterMaybe(search.visible, _.visible === search.visible)
        .filterMaybe(search.deleted, _.deleted === search.deleted)
        .filterMaybe(search.createdBy, _.createdBy === search.createdBy)
    }
  }

  // Count the connections matching the search parameters.
  def count(search: ConnectionSearch)(implicit session: Session): Long = {
    this.search(search).length.run
  }

  // Create a connection.
  def create(from: ConnectionFrom)(implicit session: Session, user: UserLike): Connection = {

    // Convert to a connection.
    val creating: Connection = Connection(
      term = from.term,
      context = from.context,
      createdAt = new DateTime(UTC),
      createdBy = user,
      votes = from.votes
    )

    // Create the connection.
    val id = this.returning(this.map(_.id)).insert(creating)
    creating.copy(id = id)
  }

  // Delete a connection.
  def delete(id: Long)(implicit session: Session, user: UserLike): Option[Connection] = {
    patch(id, ConnectionPatch(visible = Some(false), deleted = Some(true)))
  }

  // Find a connection by ID.
  def find(id: Long)(implicit session: Session): Option[Connection] = {
    this.filter(_.id === id).firstOption
  }

  // Find the connections matching the set of IDs.
  def find(ids: Set[Long])(implicit session: Session): Map[Long, Connection] = {
    toIdMap(this.filter(_.id inSet ids).list)
  }

  // Find the connections matching the search parameters.
  def find(search: ConnectionSearch)(implicit session: Session): List[Connection] = {
    this.search(search).list
  }

  // Find a connection matching the search parameters.
  def findOne(search: ConnectionSearch)(implicit session: Session): Option[Connection] = {
    this.search(search).firstOption
  }

  // Patch a connection by ID.
  def patch(id: Long, patch: ConnectionPatch)(implicit session: Session, user: UserLike): Option[Connection] = {

    // Load the connection.
    find(id).map { connection =>

      // Apply the patch to the connection.
      val patching = connection.copy(
        visible = patch.visible.getOrElse(connection.visible),
        deleted = patch.deleted.getOrElse(connection.deleted),
        votes = patch.votes.getOrElse(connection.votes)
      )

      // Patch the connection.
      this.filter(_.id === id).update(patching)
      patching
    }
  }

  // Populate the context for a connection.
  def populateContext(connection: Connection)(implicit session: Session): Connection = {
    val context = Contexts.find(connection.context.id).getOrElse(connection.context)
    connection.copy(context = context)
  }

  // Populate the context for a list of connections.
  def populateContext(connections: List[Connection])(implicit session: Session): List[Connection] = {
    val contexts = Contexts.find(connections.map(_.context.id).flatten.toSet)
    connections.map(c => c.copy(context = contexts.get(c.context.id.get).getOrElse(c.context)))
  }

  // Populate the createdBy user for a connection.
  def populateCreatedBy(connection: Connection)(implicit session: Session): Connection = {
    val createdBy = Users.find(connection.createdBy.id).getOrElse(connection.createdBy)
    connection.copy(createdBy = createdBy)
  }

  // Populate the createdBy for a list of connections.
  def populateCreatedBy(connections: List[Connection])(implicit session: Session): List[Connection] = {
    val createdBys = Users.find(connections.map(_.createdBy.id).flatten.toSet)
    connections.map(c => c.copy(createdBy = createdBys.get(c.createdBy.id.get).getOrElse(c.createdBy)))
  }

  // Populate the default references for a connection.
  def populateDefaults(connection: Connection)(implicit session: Session): Connection = {
    populateCreatedBy(populateContext(populateTerm(connection)))
  }

  // Populate the default references for a list of connections.
  def populateDefaults(connections: List[Connection])(implicit session: Session): List[Connection] = {
    populateCreatedBy(populateContext(populateTerm(connections)))
  }

  // Populate the term for a connection.
  def populateTerm(connection: Connection)(implicit session: Session): Connection = {
    val term = Terms.find(connection.term.id).getOrElse(connection.term)
    connection.copy(term = term)
  }

  // Populate the term for a list of connections.
  def populateTerm(connections: List[Connection])(implicit session: Session): List[Connection] = {
    val terms = Terms.find(connections.map(_.term.id).flatten.toSet)
    connections.map(c => c.copy(term = terms.get(c.term.id.get).getOrElse(c.term)))
  }

  // Resolve a connection reference.
  def resolve(ref: ConnectionReference)(implicit session: Session, user: UserLike): Connection = {
    ref.id.flatMap(find).orElse(ref.from.map(create)).getOrElse(throw EntityReferenceException("Could not resolve connection reference."))
  }
}

/**
 * Connection Mapper Companion
 */
object Connections extends ConnectionMapper
