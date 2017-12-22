package be.kuleuven.proman.controllers

import be.kuleuven.proman.repositories._

import fs2.Task
import io.circe.syntax._
import org.http4s.dsl._
import org.http4s.Response


object SynchronisationController {

  def projects(state: Long): Task[Response] = Ok(
    s"""{
      "state": ${TodoProjectRepository.getState},
      "projects": ${TodoProjectRepository.allUpdatedSinceState(state).asJson.noSpaces}
    }"""
  )

  def projectWithListsAndTodos(project_id: Long, state_projects: Long, state_lists: Long, state_todos: Long): Task[Response] = Ok(
    s"""{
      "project": ${
        val p = TodoProjectRepository.getIfUpdatedSinceState(state_projects, project_id)
        if (p != null) p.asJson.noSpaces else null
      },
      "state_projects": ${TodoProjectRepository.getState},
      "lists": ${TodoListRepository.allUpdatedSinceState(state_lists, project_id).asJson.noSpaces},
      "state_lists": ${TodoListRepository.getState},
      "todos": ${TodoEntryRepository.allUpdatedSinceState(state_todos, project_id).asJson.noSpaces},
      "state_todos": ${TodoEntryRepository.getState}
    }"""
  )
}
