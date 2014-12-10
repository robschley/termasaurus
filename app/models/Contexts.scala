package models

import com.github.tototoshi.slick.MySQLJodaSupport._
import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._

/**
 * Contexts Table Class
 *
 * @type {Contexts}
 */
class Contexts(tag: Tag) extends Table[Context](tag, "contexts") {

  // The table columns.
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def visible = column[Boolean]("visible", O.NotNull, O.Default[Boolean](true))
  def deleted = column[Boolean]("deleted", O.NotNull, O.Default[Boolean](false))
  def createdAt = column[DateTime]("created_at", O.NotNull)
  def createdBy = column[UserLike]("created_by", O.NotNull)
  def connections = column[Set[ConnectionLike]]("connections", O.NotNull, O.Default[Set[ConnectionLike]](Set.empty[ConnectionLike]))

  // The table projections.
  def * = (id, name, visible, deleted, createdAt, createdBy, connections) <> (Context.apply _ tupled, Context.unapply)
}

/**
 * Contexts Mapper
 */
object Contexts extends TableQuery(new Contexts(_)) with EntityMapper[ContextKind] with EntityCreatorMapper[ContextKind] with ContextKind {

  // Context query extensions.
  implicit class ContextQueryExtensions(val query: Query[Contexts, Context, Seq]) {

    // Apply filters based on the search parameters.
    def search(search: ContextSearch): Query[Contexts, Context, Seq] = {
      query
        .filterMaybe(search.id, _.id === search.id)
        .filterMaybe(search.ids, _.id inSet search.ids.get)
        .filterMaybe(search.name, _.name === search.name)
        .filterMaybe(search.visible, _.visible === search.visible)
        .filterMaybe(search.deleted, _.deleted === search.deleted)
        .filterMaybe(search.createdBy, _.createdBy === search.createdBy)
    }
  }

  // Count the contexts matching the search parameters.
  def count(search: ContextSearch)(implicit session: Session): Long = {
    Contexts.search(search).length.run
  }

  // Create a context.
  def create(from: ContextFrom)(implicit session: Session, user: UserLike): Context = {

    // Convert to a context.
    val creating: Context = Context(
      name = from.name,
      createdAt = new DateTime(UTC),
      createdBy = user
    )

    // Create the context.
    val id = Contexts.returning(Contexts.map(_.id)).insert(creating)
    creating.copy(id = id)
  }

  // Delete a context.
  def delete(id: Long)(implicit session: Session, user: UserLike): Option[Context] = {

    patch(id, ContextPatch(visible = Some(false), deleted = Some(true)))
  }

  // Find a context by ID.
  def find(id: Long)(implicit session: Session): Option[Context] = {
    Contexts.filter(_.id === id).firstOption
  }

  // Find the contexts matching the set of IDs.
  def find(ids: Set[Long])(implicit session: Session): Map[Long, Context] = {
    toIdMap(Contexts.filter(_.id inSet ids).list)
  }

  // Find the contexts matching the search parameters.
  def find(search: ContextSearch)(implicit session: Session): List[Context] = {
    Contexts.search(search).list
  }

  // Find a context matching the search parameters.
  def findOne(search: ContextSearch)(implicit session: Session): Option[Context] = {
    Contexts.search(search).firstOption
  }

  // Patch a context by ID.
  def patch(id: Long, patch: ContextPatch)(implicit session: Session, user: UserLike): Option[Context] = {

    // Load the context.
    find(id).map { context =>

      // Apply the patch to the context.
      val patching = context.copy(
        name = patch.name.getOrElse(context.name),
        visible = patch.visible.getOrElse(context.visible),
        deleted = patch.deleted.getOrElse(context.deleted),
        connections = patch.connections.getOrElse(context.connections)
      )

      // Patch the context.
      Contexts.filter(_.id === id).update(patching)
      patching
    }
  }

  // Populate the connections for a context.
  def populateConnections(context: Context)(implicit session: Session): Context = {
    val connections = Connections.find(toIdSet(context.connections))
    context.copy(connections = connections.values.toSet)
  }

  // Populate the connections for a list of contexts.
  def populateConnections(contexts: List[Context])(implicit session: Session): List[Context] = {
    val connections = Connections.find(contexts.map(_.connections.map(_.id).flatten).flatten.toSet)
    contexts.map(context => context.copy(connections = context.connections.map(c => connections.get(c.id.get).getOrElse(c))))
  }

  // Populate the createdBy user for a context.
  def populateCreatedBy(context: Context)(implicit session: Session): Context = {
    val createdBy = Users.find(context.createdBy.id).getOrElse(context.createdBy)
    context.copy(createdBy = createdBy)
  }

  // Populate the createdBy for a list of contexts.
  def populateCreatedBy(contexts: List[Context])(implicit session: Session): List[Context] = {
    val createdBy = Users.find(contexts.map(_.createdBy.id).flatten.toSet)
    contexts.map(context => context.copy(createdBy = createdBy.get(context.createdBy.id.get).getOrElse(context.createdBy)))
  }

  // Populate the default references for a context.
  def populateDefaults(context: Context)(implicit session: Session): Context = {
    populateCreatedBy(populateConnections(context))
  }

  // Populate the default references for a list of contexts.
  def populateDefaults(contexts: List[Context])(implicit session: Session): List[Context] = {
    populateCreatedBy(populateConnections(contexts))
  }

  // Resolve a context reference.
  def resolve(ref: ContextReference)(implicit session: Session, user: UserLike): Context = {
    ref.id.flatMap(find).orElse(ref.from.map(create)).getOrElse(throw EntityReferenceException("Could not resolve context reference."))
  }
}
