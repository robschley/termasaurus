package models

import com.roundeights.hasher.Implicits._
import play.api.db.slick.Config.driver.simple._
import play.api.libs.json._
import play.api.Play

/**
 * Password Trait
 */
sealed trait Password {

  // The password value.
  val value: String

  // Compare a plaintext string with the hashed password.
  def matches(str: String): Boolean = {
    str.bcrypt hash= value
  }

  // Hash the password if plaintext.
  def hash(): HashedPassword = {
    this match {
      case v: HashedPassword => v
      case v: PlaintextPassword => {
        HashedPassword(v.value.bcrypt.hex)
      }
    }
  }
}

/**
 * Password Companion
 */
object Password {

  // Convert password to and from the database.
  implicit val mapTypeSet = MappedColumnType.base[Password, String](
    pw => pw.hash().value,
    str => HashedPassword(str)
  )

  // Read a plaintext password from JSON.
  val jsonRead: Reads[Password] = new Reads[Password] {
    def reads(json: JsValue) = json.asOpt[String].map(str => JsSuccess(PlaintextPassword(str))).getOrElse(JsError("Could not read Password"))
  }
    // (__ \ "password").read[String].map(str => PlaintextPassword(str))

  // Write a null to JSON.
  val jsonWrite: Writes[Password] = new Writes[Password] {
    def writes(password: Password): JsValue = JsNull
  }

  // Convert to and from JSON.
  implicit val jsonFormat: Format[Password] = Format[Password](jsonRead, jsonWrite)
}

/**
 * Plaintext Password Class
 */
case class PlaintextPassword(val value: String) extends Password

/**
 * Hashed Password Class
 */
case class HashedPassword(val value: String) extends Password
