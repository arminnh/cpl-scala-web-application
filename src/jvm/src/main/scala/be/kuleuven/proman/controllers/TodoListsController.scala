package be.kuleuven.proman.controllers

import be.kuleuven.proman.models._
import be.kuleuven.proman.repositories._

import fs2.Task
import io.circe.syntax._
import org.http4s.{Request, Response}
import org.http4s.circe._
import org.http4s.dsl._


object TodoListsController {

  def store(request: Request): Task[Response] =
    for {
      list <- request.as(jsonOf[TodoList])
      response <- Ok(TodoListRepository.create(list.project_id, list.name).asJson)
    } yield {
      response
    }

  def update(request: Request, id: Long): Task[Response] = {
    for {
      list <- request.as(jsonOf[TodoList])
      response <- Ok(TodoListRepository.update(id, list).asJson)
    } yield {
      response
    }
  }
}
