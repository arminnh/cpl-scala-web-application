package be.kuleuven.proman.models

import io.circe.{Decoder, Encoder, HCursor, Json}
import cats.syntax.either._ // magically fixes "value map is not a member of io.circe.Decoder.Result[String]"
import scalatags.generic.Bundle


class TODOProject(var name: String, var todos: List[TODOEntry]=List()) {
  val id: Int = TODOProject.nextID

  override def toString: String = {
    s"TODO Project with name: ${this.name} and todos: \n${this.todos.map(_.toString)}\n"
  }
}


object TODOProject {
  private var id = 0

  private def nextID = {
    id += 1
    id
  }

  implicit val encodeTODOProject: Encoder[TODOProject] = new Encoder[TODOProject] {
    final def apply(p: TODOProject): Json = Json.obj(
      ("id", Json.fromInt(p.id)),
      ("name", Json.fromString(p.name))
    )
  }

  implicit val decodeTODOProject: Decoder[TODOProject] = new Decoder[TODOProject] {
    final def apply(cursor: HCursor): Decoder.Result[TODOProject] =
      for {
        name <- cursor.downField("name").as[String]
      } yield {
        new TODOProject(name)
      }
  }
}


class TODOProjectTemplate[Builder, Output <: FragT, FragT](val bundle: Bundle[Builder, Output, FragT]) {

  import bundle.all._

  def singleTemplate(project: TODOProject) = p(
    a(href := "projects/"+project.id, cls := "project-link")(s"${project.name}")
  )
  def multipleTemplate(projects: Seq[TODOProject]) = div(projects.map(singleTemplate))
}