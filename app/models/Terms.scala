package models

import com.github.tototoshi.slick.MySQLJodaSupport._
import play.api.db.slick.Config.driver.simple._

/**
 * Terms Table Class
 *
 * @type {Terms}
 */
class Terms(tag: Tag) extends Table[Term](tag, "terms") {

  // The table columns.
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def slug = column[String]("slug", O.NotNull)
  def visible = column[Boolean]("visible", O.NotNull, O.Default[Boolean](true))
  def deleted = column[Boolean]("deleted", O.NotNull, O.Default[Boolean](false))
  def createdAt = column[DateTime]("created_at", O.NotNull)
  def createdBy = column[UserLike]("created_by", O.NotNull)
  def connections = column[Set[ConnectionLike]]("connections", O.NotNull, O.Default[Set[ConnectionLike]](Set.empty[ConnectionLike]))

  // The table projections.
  def * = (id, name, slug, visible, deleted, createdAt, createdBy, connections) <> (Term.apply _ tupled, Term.unapply)
}

/**
 * Terms Mapper Class
 */
class TermMapper extends TableQuery(new Terms(_)) with EntityMapper[TermKind] with EntityCreatorMapper[TermKind] with TermKind {

  // Term query extensions.
  implicit class TermQueryExtensions(val query: Query[Terms, Term, Seq]) {

    // Apply filters based on the search parameters.
    def search(search: TermSearch): Query[Terms, Term, Seq] = {
      query
        .filterMaybe(search.id, _.id === search.id)
        .filterMaybe(search.ids, _.id inSet search.ids.getOrElse(Set.empty[Long]))
        .filterMaybe(search.name, _.name === search.name)
        .filterMaybe(search.visible, _.visible === search.visible)
        .filterMaybe(search.deleted, _.deleted === search.deleted)
        .filterMaybe(search.createdBy, _.createdBy === search.createdBy)
    }
  }

  // Count the terms matching the search parameters.
  def count(search: TermSearch)(implicit session: Session): Long = {
    this.search(search).length.run
  }

  // Create a term.
  def create(from: TermFrom)(implicit session: Session, user: UserLike): Term = {

    // Convert to a term.
    val creating: Term = Term(
      name = from.name,
      slug = Term.toSlug(from.name),
      createdAt = new DateTime(UTC),
      createdBy = user
    )

    // Create the term.
    val id = this.returning(this.map(_.id)).insert(creating)
    creating.copy(id = id)
  }

  // Delete a term.
  def delete(id: Long)(implicit session: Session, user: UserLike): Option[Term] = {
    // TODO: Update connections to not reference this term.
    patch(id, TermPatch(visible = Some(false), deleted = Some(true)))
  }

  // Find a term by ID.
  def find(id: Long)(implicit session: Session): Option[Term] = {
    this.filter(_.id === id).firstOption
  }

  // Find the terms matching the set of IDs.
  def find(ids: Set[Long])(implicit session: Session): Map[Long, Term] = {
    toIdMap(this.filter(_.id inSet ids).list)
  }

  // Find the terms matching the search parameters.
  def find(search: TermSearch)(implicit session: Session): List[Term] = {
    this.search(search).list
  }

  // Find a term matching the search parameters.
  def findOne(search: TermSearch)(implicit session: Session): Option[Term] = {
    this.search(search).firstOption
  }

  // Patch a term by ID.
  def patch(id: Long, patch: TermPatch)(implicit session: Session, user: UserLike): Option[Term] = {

    // Load the term.
    find(id).map { term =>

      // Apply the patch to the term.
      val patching = term.copy(
        name = patch.name.getOrElse(term.name),
        slug = Term.toSlug(patch.name.getOrElse(term.name)),
        visible = patch.visible.getOrElse(term.visible),
        deleted = patch.deleted.getOrElse(term.deleted),
        connections = patch.connections.getOrElse(term.connections)
      )

      // Patch the term.
      this.filter(_.id === id).update(patching)
      patching
    }
  }

  // Populate the connections for a term.
  def populateConnections(term: Term)(implicit session: Session): Term = {
    val connections = Connections.find(toIdSet(term.connections))
    term.copy(connections = connections.values.toSet)
  }

  // Populate the connections for a list of terms.
  def populateConnections(terms: List[Term])(implicit session: Session): List[Term] = {
    val connections = Connections.find(terms.map(_.connections.map(_.id).flatten).flatten.toSet)
    terms.map(term => term.copy(connections = term.connections.map(c => connections.get(c.id.get).getOrElse(c))))
  }

  // Populate the createdBy user for a term.
  def populateCreatedBy(term: Term)(implicit session: Session): Term = {
    val createdBy = Users.find(term.createdBy.id).getOrElse(term.createdBy)
    term.copy(createdBy = createdBy)
  }

  // Populate the createdBy for a list of terms.
  def populateCreatedBy(terms: List[Term])(implicit session: Session): List[Term] = {
    val createdBy = Users.find(terms.map(_.createdBy.id).flatten.toSet)
    terms.map(term => term.copy(createdBy = createdBy.get(term.createdBy.id.get).getOrElse(term.createdBy)))
  }

  // Populate the default references for a term.
  def populateDefaults(term: Term)(implicit session: Session): Term = {
    populateCreatedBy(populateConnections(term))
  }

  // Populate the default references for a list of terms.
  def populateDefaults(terms: List[Term])(implicit session: Session): List[Term] = {
    populateCreatedBy(populateConnections(terms))
  }

  // Resolve a term reference.
  def resolve(ref: TermReference)(implicit session: Session, user: UserLike): Term = {
    ref.id.flatMap(find)
      .orElse(ref.from.flatMap(from => findOne(TermSearch(slug = Some(Term.toSlug(from.name))))))
      .orElse(ref.from.map(create))
      .getOrElse(throw EntityReferenceException("Could not resolve term reference."))
  }
}

/**
 * Term Mapper Companion
 */
object Terms extends TermMapper
