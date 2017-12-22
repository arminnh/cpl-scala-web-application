package be.kuleuven.proman.repositories

import be.kuleuven.proman.models.TodoList


object TodoListRepository {
  private var id: Long = 0
  private var lists: Seq[TodoList] = Seq()

  // Keep state as just a number that increments with every change to state.
  // Will be good enough for the given requirements.
  private var state: Long = 0
  // Map to keep track which project changed at which state. A smarter policy would remove all entries which
  // are not relevant anymore (this is when all current clients are up to date with a certain common state).
  private var state_changes: Map[Long, Long] = Map()

  private def nextID: Long = { id += 1; id }
  private def nextState: Long = { state += 1; state }

  def create(project_id: Long, name: String): TodoList = {
    val todo = new TodoList(this.nextID, project_id, name)
    this.lists +:= todo
    this.state_changes += (this.nextState -> todo.id)
    todo
  }

  def allForProject(project_id: Long): Seq[TodoList] =
    this.lists.filter(_.project_id == project_id).sortWith(_.id > _.id)

  def find(id: Long): TodoList =
    this.lists.find(_.id == id).orNull

  def update(id: Long, list: TodoList): TodoList = {
    this.lists = this.lists.updated(this.lists.indexWhere(_.id == id), list)
    this.state_changes += (this.nextState -> list.id)
    list
  }

  def getState: Long =
    this.state

  def allUpdatedSinceState(state: Long, project_id: Long): List[TodoList] =
    this.state_changes.filterKeys(key => key > state).values
      .toList.distinct.map(id => this.find(id))
      .filter(list => list.project_id == project_id)
}
