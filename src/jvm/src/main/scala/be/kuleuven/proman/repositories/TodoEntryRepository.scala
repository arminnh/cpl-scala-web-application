package be.kuleuven.proman.repositories

import be.kuleuven.proman.models.TodoEntry
import scala.collection.mutable.{ HashMap, MultiMap, Set }


object TodoEntryRepository {
  private var id: Long = 0
  private var entries: Seq[TodoEntry] = Seq()

  // Map to keep track which entry changed at which timestamp.
  private val changes = new HashMap[Long, Set[Long]] with MultiMap[Long, Long]

  private def nextID: Long = { id += 1; id }

  def create(list_id: Long, text: String, is_done: Boolean=false): TodoEntry = {
    val todo = new TodoEntry(nextID, list_id, text, is_done)
    this.entries +:= todo
    this.changes.addBinding(System.currentTimeMillis(), todo.id)
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
    this.changes.addBinding(System.currentTimeMillis(), todo.id)
    todo
  }

  def allUpdatedSince(timestamp: Long, project_id: Long): List[TodoEntry] = {
    val list_ids: Seq[Long] = TodoListRepository.allForProject(project_id).map(_.id)

    this.changes.filterKeys(_ >= timestamp).values.toList.flatten.distinct.map(id => this.find(id))
      .filter(todo => list_ids.contains(todo.list_id))
  }
}
