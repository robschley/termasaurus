package models

import com.github.tototoshi.slick.MySQLJodaSupport._
import play.api.db.slick.Config.driver.simple._

/**
 * Users Table Class
 *
 * @type {Users}
 */
class Users(tag: Tag) extends Table[User](tag, "users") {

  // The table columns.
  def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def username = column[String]("username", O.NotNull)
  def password = column[Password]("password", O.NotNull)
  def visible = column[Boolean]("visible", O.NotNull, O.Default[Boolean](true))
  def deleted = column[Boolean]("deleted", O.NotNull, O.Default[Boolean](false))
  def createdAt = column[DateTime]("created_at", O.NotNull)

  // The table projections.
  def * = (id, name, username, password, visible, deleted, createdAt) <> (User.apply _ tupled, User.unapply)
}

/**
 * Users Mapper Class
 */
class UserMapper extends TableQuery(new Users(_)) with EntityMapper[UserKind] with UserKind {

  // User query extensions.
  implicit class UserQueryExtensions(val query: Query[Users, User, Seq]) {

    // Apply filters based on the search parameters.
    def search(search: UserSearch): Query[Users, User, Seq] = {
      query
        .filterMaybe(search.id, _.id === search.id)
        .filterMaybe(search.ids, _.id inSet search.ids.getOrElse(Set.empty[Long]))
        .filterMaybe(search.name, _.name === search.name)
        .filterMaybe(search.username, _.username === search.username)
        .filterMaybe(search.visible, _.visible === search.visible)
        .filterMaybe(search.deleted, _.deleted === search.deleted)
    }
  }

  // Authenticate a user.
  def authenticate(username: String, password: String)(implicit session: Session): Option[User] = {
    findOne(username).flatMap { user =>
      if (user.password.matches(password)) {
        Some(user)
      }
      else {
        None
      }
    }
  }

  // Count the users matching the search parameters.
  def count(search: UserSearch)(implicit session: Session): Long = {
    this.search(search).length.run
  }

  // Create a user.
  def create(from: UserFrom)(implicit session: Session, user: UserLike): User = {

    // Check if the username is already taken.
    this.findOne(UserSearch(username = Some(from.username))).map { user =>
      throw UsernameExistsException(s"""Username "${from.username}" already exists.""")
    }

    // Convert to a user.
    val creating: User = User(
      name = from.name,
      username = from.username,
      password = from.password,
      createdAt = new DateTime(UTC)
    )

    // Create the user.
    val id = this.returning(this.map(_.id)).insert(creating)
    creating.copy(id = id)
  }

  // Delete a user.
  def delete(id: Long)(implicit session: Session, user: UserLike): Option[User] = {
    patch(id, UserPatch(visible = Some(false), deleted = Some(true)))
  }

  // Find a user by ID.
  def find(id: Long)(implicit session: Session): Option[User] = {
    this.filter(_.id === id).firstOption
  }

  // Find the users matching the search parameters.
  def find(search: UserSearch)(implicit session: Session): List[User] = {
    this.search(search).list
  }

  // Find the users matching the set of IDs.
  def find(ids: Set[Long])(implicit session: Session): Map[Long, User] = {
    toIdMap(this.filter(_.id inSet ids).list)
  }

  // Find a user matching the search parameters.
  def findOne(search: UserSearch)(implicit session: Session): Option[User] = {
    this.search(search).firstOption
  }

  // Find a user matching the username.
  def findOne(username: String)(implicit session: Session): Option[User] = {
    this.search(UserSearch(username = Some(username))).firstOption
  }

  // Patch a user by ID.
  def patch(id: Long, patch: UserPatch)(implicit session: Session, user: UserLike): Option[User] = {

    // Load the user.
    find(id).map { user =>

      // Apply the patch to the user.
      val patching = user.copy(
        name = patch.name.getOrElse(user.name),
        password = patch.password.getOrElse(user.password),
        visible = patch.visible.getOrElse(user.visible),
        deleted = patch.deleted.getOrElse(user.deleted)
      )

      // Patch the user.
      this.filter(_.id === id).update(patching)
      patching
    }
  }

  // Populate the default references for a user.
  def populateDefaults(user: User)(implicit session: Session): User = {
    user
  }

  // Populate the default references for a list of users.
  def populateDefaults(users: List[User])(implicit session: Session): List[User] = {
    users
  }

  // Resolve a user reference.
  def resolve(ref: UserReference)(implicit session: Session, user: UserLike): User = {
    ref.id.flatMap(find).orElse(ref.from.map(create)).getOrElse(throw EntityReferenceException("Could not resolve user reference."))
  }
}

/**
 * User Mapper Companion
 */
object Users extends UserMapper
