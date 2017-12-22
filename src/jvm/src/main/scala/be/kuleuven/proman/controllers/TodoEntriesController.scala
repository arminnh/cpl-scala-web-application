package be.kuleuven.proman.controllers

import be.kuleuven.proman.models._
import be.kuleuven.proman.repositories._

import fs2.Task
import io.circe.syntax._
import org.http4s.{Request, Response}
import org.http4s.circe._
import org.http4s.dsl._


object TodoEntriesController {

  def store(request: Request): Task[Response] = {
    request.as(jsonOf[TodoEntry]).flatMap(todo => Ok(TodoEntryRepository.create(todo.list_id, todo.text).asJson))
  }

  def update(request: Request, todo_id: Long): Task[Response] = {
    for {
      todo <- request.as(jsonOf[TodoEntry])
      response <- Ok(TodoEntryRepository.update(todo_id, todo).asJson)
    } yield {
      response
    }
  }
}
