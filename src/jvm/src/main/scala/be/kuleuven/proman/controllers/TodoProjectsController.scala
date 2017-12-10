package be.kuleuven.proman.controllers

import be.kuleuven.proman.models._
import be.kuleuven.proman.repositories._

import fs2.Task
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._


object TodoProjectsController {

  def index: Task[Response] =
    Ok(TodoProjectRepository.all().asJson)

  def get(id: Long): Task[Response] = {
    val project = TodoProjectRepository.find(id)
    if (project != null) {
      Ok(project.asJson)
    } else {
      NotFound()
    }
  }

  def exists(name: String): Task[Response] =
    Ok(TodoProjectRepository.exists(name).asJson)

  def store(request: Request): Task[Response] =
    for {
      project <- request.as(jsonOf[TodoProject])
      response <- Ok(TodoProjectRepository.create(project.name).asJson)
    } yield {
      response
    }

  def update(request: Request, id: Long): Task[Response] =
    for {
      project <- request.as(jsonOf[TodoProject])
      response <- Ok(TodoProjectRepository.update(id, project).asJson)
    } yield {
      response
    }

  def synchronise(state: Long): Task[Response] = Ok(
    s"""{
      "state": ${TodoProjectRepository.getState},
      "projects": ${TodoProjectRepository.allUpdatedSinceState(state).asJson}
    }"""
  )
}
