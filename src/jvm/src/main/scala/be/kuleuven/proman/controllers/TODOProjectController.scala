package be.kuleuven.proman.controllers

import be.kuleuven.proman.models._
import be.kuleuven.proman.repositories._

import fs2.Task
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._


object TODOProjectController {

  def index: Task[Response] = Ok(TODOProjectRepository.all().asJson)

  def get(id: Long): Task[Response] = {
    val project = TODOProjectRepository.find(id)
    if (project != null) {
      Ok(project.asJson)
    } else {
      NotFound()
    }
  }

  def store(request: Request): Task[Response] =
    for {
      project <- request.as(jsonOf[TODOProject])
      response <- Ok(TODOProjectRepository.create(project.name).asJson)
    } yield {
      response
    }

  def exists(name: String): Task[Response] = Ok(TODOProjectRepository.exists(name).asJson)

  def synchronise(state: Long): Task[Response] = Ok(
    s"""{
      "state": ${TODOProjectRepository.getState},
      "projects": ${TODOProjectRepository.allUpdatedSinceState(state).asJson}
    }"""
  )
}
