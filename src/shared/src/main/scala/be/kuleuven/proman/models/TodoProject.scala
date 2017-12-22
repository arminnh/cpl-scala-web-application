package be.kuleuven.proman.models

import io.circe.{Decoder, Encoder, HCursor, Json}
import cats.syntax.either._
import scalatags.generic.Bundle


class TodoProject(var id: Long, var name: String, var description: String) {
  def this(id: Long, name: String) = this(id, name, "Description of project " + name)
  def this(name: String) = this(-999, name)

  override def toString: String = s"TodoProject ${this.id}, name: ${this.name}\n"
}


object TodoProject {
  implicit val encodeTodoProject: Encoder[TodoProject] = new Encoder[TodoProject] {
    final def apply(p: TodoProject): Json = Json.obj(
      ("id", Json.fromLong(p.id)),
      ("name", Json.fromString(p.name)),
      ("description", Json.fromString(p.description))
    )
  }

  implicit val decodeTodoProject: Decoder[TodoProject] = new Decoder[TodoProject] {
    final def apply(cursor: HCursor): Decoder.Result[TodoProject] =
      for {
        id <- cursor.downField("id").as[Long]
        name <- cursor.downField("name").as[String]
        description <- cursor.downField("description").as[String]
      } yield {
        new TodoProject(id, name, description)
      }
  }
}


class TodoProjectTemplate[Builder, Output <: FragT, FragT](val bundle: Bundle[Builder, Output, FragT]) {
  import bundle.all._

  def singleTemplate(project: TodoProject) = {
    tr(
      td(verticalAlign := "middle", width := 55)(project.id),
      td(verticalAlign := "middle")(
        button(attr("data-id") := project.id, cls := "project-anchor btn-link")(project.name)
      )
    )
  }
}