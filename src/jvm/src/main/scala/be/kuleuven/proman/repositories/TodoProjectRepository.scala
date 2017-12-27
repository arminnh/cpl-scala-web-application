package be.kuleuven.proman.repositories

import be.kuleuven.proman.models.TodoProject
import scala.collection.mutable.{ HashMap, MultiMap, Set }


object TodoProjectRepository {
  private var id: Long = 0
  private var projects: Seq[TodoProject] = Seq()

  // Map to keep track which project changed at which timestamp.
  private val changes = new HashMap[Long, Set[Long]] with MultiMap[Long, Long]

  private def nextID: Long = { id += 1; id }

  def create(name: String): TodoProject = {
    val project = new TodoProject(nextID, name)
    this.projects = project +: this.projects
    this.changes.addBinding(System.currentTimeMillis(), project.id)
    project
  }

  def all(): Seq[TodoProject] =
    this.projects

  def exists(name: String): Boolean =
    this.projects.find(_.name == name).orNull != null

  def find(id: Long): TodoProject =
    this.projects.find(_.id == id).orNull

  def update(id: Long, project: TodoProject): TodoProject = {
    this.projects = this.projects.updated(this.projects.indexWhere(_.id == id), project)
    this.changes.addBinding(System.currentTimeMillis(), project.id)
    project
  }

  def allUpdatedSince(timestamp: Long): List[TodoProject] =
    this.changes.filterKeys(_ >= timestamp).values.toList
      .flatten.distinct.sorted(Ordering[Long].reverse).map(id => this.find(id))

  def getIfUpdatedSince(timestamp: Long, id: Long): TodoProject =
    this.find(
//      this.changes.filterKeys(_ >= timestamp).find(_._2 == id).getOrElse((0L, 0L))._2
      this.changes.filterKeys(_ >= timestamp).values.toList.flatten.find(_ == id).getOrElse(0)
    )
}

