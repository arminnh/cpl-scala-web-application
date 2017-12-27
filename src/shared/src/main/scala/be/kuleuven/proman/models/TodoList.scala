package be.kuleuven.proman.models

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax._
import cats.syntax.either._
import scalatags.generic.Bundle


class TodoList(var id: Long, var project_id: Long, var name: String) {
  def this(project_id: Long, name: String) = this(-999, project_id, name)

  override def toString: String =
    s"TodoList ${this.id}, name: ${this.name}\n"
}

object TodoList {
  implicit val encodeTodoList: Encoder[TodoList] = new Encoder[TodoList] {
    final def apply(l: TodoList): Json = Json.obj(
      ("id",         Json.fromLong(l.id)),
      ("project_id", Json.fromLong(l.project_id)),
      ("name",       Json.fromString(l.name))
    )
  }

  implicit val decodeTodoList: Decoder[TodoList] = new Decoder[TodoList] {
    final def apply(cursor: HCursor): Decoder.Result[TodoList] =
      for {
        id         <- cursor.downField("id").as[Long]
        project_id <- cursor.downField("project_id").as[Long]
        name       <- cursor.downField("name").as[String]
      } yield {
        new TodoList(id, project_id, name)
      }
  }
}

class TodoListTemplate[Builder, Output <: FragT, FragT](val bundle: Bundle[Builder, Output, FragT]) {
  import bundle.all._

  def singleTemplate(list: TodoList) = {
    div(attr("data-json") := list.asJson.noSpaces)(
      h3(cls := "clearfix")(
        div(cls := "pull-left")(
          span(cls := "todo-list-name", attr("data-id") := list.id)(list.name),
          input(tpe := "text", name := "name", cls := "form-control", placeholder := "Name", display := "none")
        ),
        div(cls := "pull-right list-button-container")(
          button(cls := "btn btn-sm btn-default todo-list-edit", title := "Edit list name", marginRight := 15)(
            span(cls := "glyphicon glyphicon-pencil")
          ),
          button(cls := "btn btn-sm btn-default todo-list-toggle", title := "Hide this list's table")(
            span(cls := "caret caret-up")
          )
        )
      ),
      div(cls := "table-responsive")(
        table(cls := "table table-condensed table-striped table-hover")(
          tbody(cls := "todo-list-tbody", attr("data-id") := list.id)
        )
      )
    )
  }
}