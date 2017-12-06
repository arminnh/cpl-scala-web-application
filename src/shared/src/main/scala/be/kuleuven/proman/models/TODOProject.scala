package be.kuleuven.proman.models

import io.circe.{Decoder, Encoder, HCursor, Json}
import cats.syntax.either._

import scalatags.generic.Bundle


class TODOProject(var id: Long, var name: String) {
  def this(name: String) = this(-999, name)
  override def toString: String = s"Todo project: ${this.name}, id ${this.id}\n"
}


object TODOProject {
  implicit val encodeTODOProject: Encoder[TODOProject] = new Encoder[TODOProject] {
    final def apply(p: TODOProject): Json = Json.obj(
      ("id", Json.fromLong(p.id)),
      ("name", Json.fromString(p.name))
    )
  }

  implicit val decodeTODOProject: Decoder[TODOProject] = new Decoder[TODOProject] {
    final def apply(cursor: HCursor): Decoder.Result[TODOProject] =
      for {
        id <- cursor.downField("id").as[Long]
        name <- cursor.downField("name").as[String]
      } yield {
        new TODOProject(id, name)
      }
  }
}


class TODOProjectTemplate[Builder, Output <: FragT, FragT](val bundle: Bundle[Builder, Output, FragT]) {
  import bundle.all._

  def singleTemplate(project: TODOProject) = {
    tr(
      td(verticalAlign := "middle")(project.id),
      td(verticalAlign := "middle")(
        button(attr("data-id") := project.id, cls := "project-anchor btn-link")(project.name)
      )
    )
  }

  def multipleTemplate(projects: Seq[TODOProject]) = {
    div(cls := "table-responsive")(
      table(cls := "table table-condensed table-striped table-hover")(
        tbody(
          projects.map(singleTemplate)
        )
      )
    )
  }
}