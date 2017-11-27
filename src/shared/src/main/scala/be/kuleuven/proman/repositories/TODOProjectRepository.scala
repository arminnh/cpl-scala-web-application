package be.kuleuven.proman.repositories

import be.kuleuven.proman.models.TODOProject

object TODOProjectRepository {
  private var id = 0
  private var projects: Seq[TODOProject] = Seq()

  private def nextID: Int = {
    id += 1
    id
  }

  def create(name: String): TODOProject = {
    val project = new TODOProject(nextID, name)
    this.projects = project +: this.projects
    project
  }

  def all(): Seq[TODOProject] = this.projects

  def find(id: Int): TODOProject = this.projects.find(_.id == id).orNull

  def exists(name: String): Boolean = this.projects.find(_.name == name).orNull != null
}

