package be.kuleuven.proman.repositories

import be.kuleuven.proman.models.TodoEntry


object TodoEntryRepository {
  private var id: Long = 0
  private var entries: Seq[TodoEntry] = Seq()

  // Keep state as just a number that increments with every change to state.
  private var state: Long = 0
  // Map to keep track which entry changed at which state.
  private var state_changes: Map[Long, Long] = Map()

  private def nextID: Long = { id += 1; id }
  private def nextState: Long = { state += 1; state }

  def create(list_id: Long, text: String, is_done: Boolean=false): TodoEntry = {
    val todo = new TodoEntry(nextID, list_id, text, is_done)
    this.entries +:= todo
    this.state_changes += (nextState -> todo.id)
    todo
  }

  def allForProject(project_id: Long): Seq[TodoEntry] = {
    val list_ids: Seq[Long] = TodoListRepository.allForProject(project_id).map(_.id)
    this.entries.filter(e => list_ids.contains(e.list_id)).sortWith(_.timestamp > _.timestamp)
  }

  def find(id: Long): TodoEntry =
    this.entries.find(_.id == id).orNull

  def update(id: Long, todo: TodoEntry): TodoEntry = {
    this.entries = this.entries.updated(this.entries.indexWhere(_.id == id), todo)
    this.state_changes += (nextState -> todo.id)
    todo
  }

  def getState: Long =
    this.state

  def allUpdatedSinceState(state: Long, project_id: Long): List[TodoEntry] = {
    val list_ids: Seq[Long] = TodoListRepository.allForProject(project_id).map(_.id)
    this.state_changes.filterKeys(key => key > state).values
      .toList.distinct.map(id => this.find(id))
      .filter(todo => list_ids.contains(todo.list_id))
      .sortWith((p1, p2) => p1.id > p2.id)
  }
}
