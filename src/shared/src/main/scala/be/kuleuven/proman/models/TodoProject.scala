package be.kuleuven.proman.models

import io.circe.{Decoder, Encoder, HCursor, Json}
import cats.syntax.either._

import scalatags.generic.Bundle


class TodoProject(var id: Long, var name: String, var description: String, var version: Int) {
  def this(id: Long, name: String) = this(id, name, "Description of project " + name, 1)
  def this(name: String) = this(-999, name)

  override def toString: String = s"TodoProject ${this.id}, name: ${this.name}, version: ${this.version}\n"
}


object TodoProject {
  implicit val encodeTodoProject: Encoder[TodoProject] = new Encoder[TodoProject] {
    final def apply(p: TodoProject): Json = Json.obj(
      ("id", Json.fromLong(p.id)),
      ("name", Json.fromString(p.name)),
      ("description", Json.fromString(p.description)),
      ("version", Json.fromInt(p.version))
    )
  }

  implicit val decodeTodoProject: Decoder[TodoProject] = new Decoder[TodoProject] {
    final def apply(cursor: HCursor): Decoder.Result[TodoProject] =
      for {
        id <- cursor.downField("id").as[Long]
        name <- cursor.downField("name").as[String]
        description <- cursor.downField("description").as[String]
        version <- cursor.downField("version").as[Int]
      } yield {
        new TodoProject(id, name, description, version)
      }
  }
}


class TodoProjectTemplate[Builder, Output <: FragT, FragT](val bundle: Bundle[Builder, Output, FragT]) {
  import bundle.all._

  def singleTemplate(project: TodoProject) = {
    tr(
      td(verticalAlign := "middle")(project.id),
      td(verticalAlign := "middle")(
        button(attr("data-id") := project.id, cls := "project-anchor btn-link")(project.name)
      )
    )
  }

  def multipleTemplate(projects: Seq[TodoProject]) = {
    div(cls := "table-responsive")(
      table(cls := "table table-condensed table-striped table-hover")(
        tbody(
          projects.map(singleTemplate)
        )
      )
    )
  }
}