package be.kuleuven.proman.models

//import java.text.SimpleDateFormat
import java.util.Date

import io.circe.{Decoder, Encoder, HCursor, Json}
import cats.syntax.either._

import scalatags.generic.Bundle


class TODOEntry(var id: Int, var project_id: Int, var text: String, var timestamp: Long, var is_done: Boolean) {

  def this(id: Int, project_id: Int, text: String) = this(id, project_id, text, new Date().getTime, false)
  def this(id: Int, project_id: Int, text: String, timestamp: Long) = this(id, project_id, text, timestamp, false)

  override def toString: String = {
    s"Todo on timestamp: ${this.timestamp}, is done: ${this.is_done}, with text: ${this.text}\n"
  }
}


object TODOEntry {
  implicit val encodeTODOEntry: Encoder[TODOEntry] = new Encoder[TODOEntry] {
    final def apply(t: TODOEntry): Json = Json.obj(
      ("id",         Json.fromInt(t.id)),
      ("project_id", Json.fromInt(t.project_id)),
      ("text",       Json.fromString(t.text)),
      ("is_done",    Json.fromBoolean(t.is_done)),
      ("timestamp",  Json.fromLong(t.timestamp))
    )
  }

  implicit val decodeTODOEntry: Decoder[TODOEntry] = new Decoder[TODOEntry] {
    final def apply(cursor: HCursor): Decoder.Result[TODOEntry] =
      for {
        id         <- cursor.downField("id").as[Int]
        project_id <- cursor.downField("project_id").as[Int]
        text       <- cursor.downField("text").as[String]
        is_done    <- cursor.downField("is_done").as[Boolean]
        timestamp  <- cursor.downField("timestamp").as[Long]
      } yield {
        new TODOEntry(id, project_id, text, timestamp, is_done)
      }
  }

  //def formatTimestamp(ts: Long): String = new SimpleDateFormat("yyy-MM-dd HH:mm:ss").format(new Date(ts))
}


class TODOEntryTemplate[Builder, Output <: FragT, FragT](val bundle: Bundle[Builder, Output, FragT]) {
  import bundle.all._

  def singleTemplate(todo: TODOEntry) = {
    tr(attr("data-id") := todo.id)(

      td(todo.id, width := 30, verticalAlign := "middle"),

      td(cls := "todo-text")(todo.text, verticalAlign := "middle"),

      td(cls := "todo-timestamp", attr("data-timestamp") := todo.timestamp, width := 160, verticalAlign := "middle"),

      td(width := 60)(
        button(cls := "btn btn-default todo-edit", title:= "edit")(span(cls := "glyphicon glyphicon-pencil"))
      ),

      td(width := 60)(
        if (todo.is_done) {
          button(cls := "btn btn-default todo-finished", title :="move back to pending todos")(
            span(cls := "glyphicon glyphicon-remove", color := "darkred")
          )
        } else {
          button(cls := "btn btn-default todo-pending", title := "move to finished todos")(
            span(cls := "glyphicon glyphicon-ok", color := "green")
          )
        }
      )
    )
  }

  def multipleTemplate(todos: Seq[TODOEntry]) = {
    table(cls := "table table-striped")(
      tbody(
        todos.map(singleTemplate)
      )
    )
  }
}