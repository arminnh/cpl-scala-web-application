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

  def get(id: Int): Task[Response] = {
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
      println("Storing new project with name " + project.name)
      response
    }

  def exists(name: String): Task[Response] = {
    println("Project exists?: " + name)
    Ok(TODOProjectRepository.exists(name).asJson)
  }

}
