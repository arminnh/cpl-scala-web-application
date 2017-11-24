package be.kuleuven.proman.models

import java.util.Date
import java.text.SimpleDateFormat

import io.circe.{Decoder, Encoder, HCursor, Json}
import cats.syntax.either._

import scalatags.generic.Bundle

class TODOEntry(var text: String, var date: Date, var is_done: Boolean) {
  val id: Int = TODOEntry.nextID

  def this(text: String) = this(text, new Date(), false)
  def this(text: String, date: Date) = this(text, date, false)

  def dateAsString: String = new SimpleDateFormat("dd-mm-yyyy").format(this.date)

  override def toString: String = {
    s"TODO on date: ${this.dateAsString}, is done: ${this.is_done}, with text: ${this.text}\n"
  }
}


object TODOEntry {
  private var id = 0

  private def nextID = {
    id += 1
    id
  }

  implicit val encodeTODOEntry: Encoder[TODOEntry] = new Encoder[TODOEntry] {
    final def apply(t: TODOEntry): Json = Json.obj(
      ("text", Json.fromString(t.text)),
      ("is_done", Json.fromBoolean(t.is_done)),
      ("date", Json.fromString(t.dateAsString))
    )
  }

  implicit val decodeTODOEntry: Decoder[TODOEntry] = new Decoder[TODOEntry] {
    final def apply(cursor: HCursor): Decoder.Result[TODOEntry] =
      for {
        text <- cursor.downField("text").as[String]
      } yield {
        new TODOEntry(text)
      }
  }
}


class TODOEntryTemplate[Builder, Output <: FragT, FragT](val bundle: Bundle[Builder, Output, FragT]) {

  import bundle.all._

  def singleTemplate(todo: TODOEntry) = p(s"${todo.toString}")
  def multipleTemplate(todos: Seq[TODOEntry]) = div(todos.map(singleTemplate))
}