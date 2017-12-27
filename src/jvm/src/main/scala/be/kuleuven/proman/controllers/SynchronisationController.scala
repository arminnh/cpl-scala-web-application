package be.kuleuven.proman.controllers

import be.kuleuven.proman.repositories._

import fs2.Task
import io.circe.syntax._
import org.http4s.dsl._
import org.http4s.Response


object SynchronisationController {

  def projects(timestamp: Long): Task[Response] = {
    try {
      Ok(
        s"""{
      "timestamp": ${System.currentTimeMillis()},
      "projects": ${TodoProjectRepository.allUpdatedSince(timestamp).asJson.noSpaces}
    }"""
      )
    } catch {
      case e => println(e) ; e.printStackTrace() ; Ok()
    }
  }

  def projectWithListsAndTodos(project_id: Long, timestamp: Long): Task[Response] = Ok(
    s"""{
      "project": ${
        val p = TodoProjectRepository.getIfUpdatedSince(timestamp, project_id)
        if (p != null) p.asJson.noSpaces else null
      },
      "timestamp": ${System.currentTimeMillis()},
      "lists": ${TodoListRepository.allUpdatedSince(timestamp, project_id).asJson.noSpaces},
      "todos": ${TodoEntryRepository.allUpdatedSince(timestamp, project_id).asJson.noSpaces}
    }"""
  )
}
