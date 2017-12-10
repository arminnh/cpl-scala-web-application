package be.kuleuven.proman.models

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.syntax._
import cats.syntax.either._

import scalatags.generic.Bundle


class TodoEntry(var id: Long, var list_id: Long, var text: String, var is_done: Boolean, var timestamp: Long) {
  def this(id: Long, list_id: Long, text: String, is_done: Boolean) = this(id, list_id, text, is_done, System.currentTimeMillis())
  def this(list_id: Long, text: String) = this(-999, list_id, text, false)

  override def toString: String =
    s"TodoEntry ${this.id}, timestamp: ${this.timestamp}, is done: ${this.is_done}, with text: ${this.text}\n"
}


object TodoEntry {
  implicit val encodeTodoEntry: Encoder[TodoEntry] = new Encoder[TodoEntry] {
    final def apply(t: TodoEntry): Json = Json.obj(
      ("id",        Json.fromLong(t.id)),
      ("list_id",   Json.fromLong(t.list_id)),
      ("text",      Json.fromString(t.text)),
      ("timestamp", Json.fromLong(t.timestamp)),
      ("is_done",   Json.fromBoolean(t.is_done))
    )
  }

  implicit val decodeTodoEntry: Decoder[TodoEntry] = new Decoder[TodoEntry] {
    final def apply(cursor: HCursor): Decoder.Result[TodoEntry] =
      for {
        id         <- cursor.downField("id").as[Long]
        list_id    <- cursor.downField("list_id").as[Long]
        text       <- cursor.downField("text").as[String]
        timestamp  <- cursor.downField("timestamp").as[Long]
        is_done    <- cursor.downField("is_done").as[Boolean]
      } yield {
        new TodoEntry(id, list_id, text, is_done, timestamp)
      }
  }
}


class TodoEntryTemplate[Builder, Output <: FragT, FragT](val bundle: Bundle[Builder, Output, FragT]) {
  import bundle.all._

  def singleTemplate(todo: TodoEntry) = {
    tr(attr("data-id") := todo.id, attr("data-is_done") := todo.is_done, attr("data-json") := todo.asJson.noSpaces)(

      td(todo.id, width := 30, verticalAlign := "middle"),

      td(cls := "todo-text")(todo.text, verticalAlign := "middle"),

      td(attr("data-timestamp") := todo.timestamp, cls := "todo-timestamp", width := 160, verticalAlign := "middle"),

      td(width := 60)(
        button(cls := "btn btn-sm btn-default todo-edit", title:= "edit")(span(cls := "glyphicon glyphicon-pencil"))
      ),

      td(width := 60)(
        if (todo.is_done) {
          button(cls := "btn btn-sm btn-default todo-finished", title :="move back to pending todos")(
            span(cls := "glyphicon glyphicon-remove", color := "darkred")
          )
        } else {
          button(cls := "btn btn-sm btn-default todo-pending", title := "move to finished todos")(
            span(cls := "glyphicon glyphicon-ok", color := "green")
          )
        }
      )
    )
  }

  def multipleTemplate(todos: Seq[TodoEntry]) = {
    div(cls := "table-responsive")(
      table(cls := "table table-condensed table-striped table-hover")(
        tbody(
          todos.map(singleTemplate)
        )
      )
    )
  }
}