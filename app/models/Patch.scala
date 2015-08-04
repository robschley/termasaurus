package models

import play.api.libs.json._

sealed trait PatchOp {
  val path: JsPath
  val name: String
}

case class Add[T](val path: JsPath, val value: T) extends PatchOp {
  val name = "add"
}

case class Remove(val path: JsPath) extends PatchOp {
  val name = "remove"
}

case class Replace[T](val path: JsPath, val value: T) extends PatchOp {
  val name = "replace"
}

case class Move(val path: JsPath, val from: JsPath) extends PatchOp {
  val name = "move"
}

case class Copy(val path: JsPath, val from: JsPath) extends PatchOp {
  val name = "copy"
}

case class Test[T](val path: JsPath, val value: T) extends PatchOp {
  val name = "test"
}

object Test {
  implicit def TestWrites[T](implicit fmt: Writes[T]): Writes[Test[T]] = new Writes[Test[T]] {
    def writes(op: Test[T]) = {
      JsObject(Seq(
        ("op", JsString(op.name)),
        ("path", JsString(op.path.toString())),
        ("value", fmt.writes(op.value))
      ))
    }
  }
}
