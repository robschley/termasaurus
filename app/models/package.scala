
import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import play.api.data.validation.ValidationError
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

  // Convert to JSON.
  val jsonWriteThrowable = new Writes[Throwable] {
    def writes(e: Throwable) = Json.obj(
      "message" -> e.getMessage()
    )
  }

  // Convert from JSON.
  val jsonReadThrowable = new Reads[Throwable] {
    def reads(json: JsValue) = {
      (json \ "message").asOpt[String].map(v => JsSuccess(new RuntimeException(v))).getOrElse(JsError("Could not read Throwable"))
    }
  }

  // Convert to and from JSON.
  implicit val jsonFormatThrowable: Format[Throwable] = Format(jsonReadThrowable, jsonWriteThrowable)

  // Convert to JSON.
  val jsonWriteJsError = new Writes[JsError] {
    def writes(e: JsError) = {

      // Extract the missing validation errors.
      val missing = e.errors.filter { t: (JsPath, Seq[ValidationError]) =>
        t._2 match {
          case Seq(x:ValidationError) if (x.message == "error.path.missing") => true
          case _ => false
        }
      }.map(_._1.path).flatten.map(_.toString)

      // Check if all the messages are missing properties.
      if (missing.size == e.errors.size) {

        // Reformat path names.
        val props = missing.map(str => if (str.charAt(0) == '/') str.substring(1) else str).map(_.replaceAll("/", "."))

        // Display the proper message depending on the number of missing properties.
        Json.obj(
          props.size match {
            case 1 => "message" -> ("Property `" + props.mkString("") + "` is required.")
            case 2 => "message" -> ("Properties `" + props.mkString("` and `") + "` are required.")
            case _ => "message" -> ("Properties `" + props.slice(0, props.size - 1).mkString("`, `") + "` and `" + props.slice(props.size - 1, props.size).mkString("`, `") + "` are required.")
          }
        )
      }
      else {
        JsError.toFlatJson(e)
      }
    }
  }

  // Convert from JSON.
  val jsonReadJsError = new Reads[JsError] {
    def reads(json: JsValue) = JsError("Could not read JsError")
  }

  // Convert to and from JSON.
  implicit val jsonFormatJsError: Format[JsError] = Format(jsonReadJsError, jsonWriteJsError)
}

