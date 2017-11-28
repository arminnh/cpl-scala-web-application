package be.kuleuven.proman.controllers

import be.kuleuven.proman.models._
import be.kuleuven.proman.repositories._

import fs2.Task
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._

object TODOEntryController {

  def index(project_id: Int): Task[Response] = Ok(TODOEntryRepository.allForProject(project_id).asJson)

//  def store(request: Request, project_id: Int): Task[Response] = {
//    request.as[String].flatMap(name => Ok(TODOEntryRepository.create(project_id, name).asJson))
//  }

  def store(request: Request, project_id: Int): Task[Response] =
    for {
      name <- request.as[String]
      response <- Ok(TODOEntryRepository.create(project_id, name).asJson)
    } yield {
      response
    }

  def update(request: Request, project_id: Int): Task[Response] = {
    request.as[String].flatMap(name => Ok(TODOEntryRepository.update(project_id, name).asJson))
  }
}
