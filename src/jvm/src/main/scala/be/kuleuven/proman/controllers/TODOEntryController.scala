package be.kuleuven.proman.controllers

import be.kuleuven.proman.models.TODOEntry
import be.kuleuven.proman.repositories._

import fs2.Task
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._


object TODOEntryController {

  def index(project_id: Int): Task[Response] = Ok(TODOEntryRepository.allForProject(project_id).asJson)

  def store(request: Request, project_id: Int): Task[Response] = {
    request.as[String].flatMap(text => {
      println("Storing new todo entry with text: " + text)
      Ok(TODOEntryRepository.create(project_id, text).asJson)
    })
  }

//  def store(request: Request, project_id: Int): Task[Response] =
//    for {
//      name <- request.as[String]
//      response <- Ok(TODOEntryRepository.create(project_id, name).asJson)
//    } yield {
//      response
//    }

  def update(request: Request, todo_id: Int): Task[Response] = {
    for {
      todo <- request.as(jsonOf[TODOEntry])
      response <- Ok(TODOEntryRepository.update(todo_id, todo.text).asJson)
    } yield {
      println("Updated todo: " + todo)
      response
    }
  }
}
