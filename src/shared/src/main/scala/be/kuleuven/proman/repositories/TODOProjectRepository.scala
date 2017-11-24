package be.kuleuven.proman.repositories

import be.kuleuven.proman.models.{TODOEntry, TODOProject}

object TODOProjectRepository {

  val test_todo = new TODOEntry("Finish project")
  var projects: Seq[TODOProject] = Seq(
    new TODOProject("SCALA PROJECT", List(test_todo))
  )

  def create(project: TODOProject): Seq[TODOProject] = {
    this.projects = project +: this.projects
    this.projects
  }
}
