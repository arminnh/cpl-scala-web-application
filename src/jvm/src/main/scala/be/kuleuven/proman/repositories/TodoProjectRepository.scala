package be.kuleuven.proman.repositories

import be.kuleuven.proman.models.TodoProject


object TodoProjectRepository {
  private var id: Long = 0
  private var projects: Seq[TodoProject] = Seq()

  // Keep state as just a number that increments with every change to state.
  // Will be good enough for the given requirements.
  private var state: Long = 0
  // Map to keep track which project changed at which state. A smarter policy would remove all entries which
  // are not relevant anymore (this is when all current clients are up to date with a certain common state).
  private var state_changes: Map[Long, Long] = Map()

  private def nextID: Long = { id += 1; id }
  private def nextState: Long = { state += 1; state }

  def create(name: String): TodoProject = {
    val project = new TodoProject(nextID, name)
    this.projects = project +: this.projects
    this.state_changes += (nextState -> project.id)
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
    this.state_changes += (nextState -> project.id)
    project
  }

  def getState: Long =
    this.state

  def allUpdatedSinceState(state: Long): List[TodoProject] =
    this.state_changes.filterKeys(key => key > state).values
      .toList.distinct.map(id => this.find(id))
      .sortWith((p1, p2) => p1.id > p2.id)

  def getIfUpdatedSinceState(state: Long, id: Long): TodoProject =
    this.find(
      this.state_changes.filterKeys(_ > state).find(_._2 == id).getOrElse((0L, 0L))._2
    )
}

