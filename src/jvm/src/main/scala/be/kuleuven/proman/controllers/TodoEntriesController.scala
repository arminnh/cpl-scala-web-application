package be.kuleuven.proman.controllers

import be.kuleuven.proman.models._
import be.kuleuven.proman.repositories._

import fs2.Task
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._


object TodoEntriesController {

  def index(project_id: Long): Task[Response] = Ok(TodoEntryRepository.allForProject(project_id).asJson)

  def store(request: Request): Task[Response] = {
    //request.as[String].flatMap(text => {
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

  def synchronise(state: Long, project_id: Long, version: Int): Task[Response] = Ok(
    s"""{
      "state": ${TodoEntryRepository.getState},
      "todos": ${TodoEntryRepository.allUpdatedSinceState(state, project_id).asJson},
      "project": ${
        val p = TodoProjectRepository.getLaterVersionOrNull(project_id, version)
        if (p != null) p.asJson else null
      }
    }""")
}
