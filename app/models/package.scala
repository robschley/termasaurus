
import play.api.db.slick.Config.driver.simple._
import scala.slick.lifted.CanBeQueryCondition

package object models {

  // Alias the DateTime type.
  type DateTime = org.joda.time.DateTime
  type DateTimeZone = org.joda.time.DateTimeZone
  val UTC = org.joda.time.DateTimeZone.UTC

  // Extract the IDs from a collection of entities.
  implicit def toIdSet[EL <: EntityLike](col: Iterable[EL]): Set[Long] = {
    col.map(_.id).flatten.toSet
  }

  // Generic query extensions.
  implicit class GenericExtensions[E, U, C[_]](val query: Query[E, U, C]) {

    // Apply a filter condition if not empty.
    def filterMaybe[T <: Column[_]](opt: Option[Any], f: E => T)(implicit wt: CanBeQueryCondition[T]): Query[E, U, C] = {
      opt.map(o => query.filter(f)).getOrElse(query)
    }

    // Limit the query to a specific page offset.
    def page(page: Int, size: Int = 10): Query[E, U, C] = query.drop((page - 1) * size).take(size)
  }
}
