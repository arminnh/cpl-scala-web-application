package be.kuleuven.proman.repositories

import be.kuleuven.proman.models.TodoList
import scala.collection.mutable.{ HashMap, MultiMap, Set }


object TodoListRepository {
  private var id: Long = 0
  private var lists: Seq[TodoList] = Seq()

  // Map to keep track which project changed at which timestamp.
  private val changes = new HashMap[Long, Set[Long]] with MultiMap[Long, Long]

  private def nextID: Long = { id += 1; id }

  def create(project_id: Long, name: String): TodoList = {
    val list = new TodoList(this.nextID, project_id, name)
    this.lists +:= list
    this.changes.addBinding(System.currentTimeMillis(), list.id)
    list
  }

  def allForProject(project_id: Long): Seq[TodoList] =
    this.lists.filter(_.project_id == project_id).sortWith(_.id > _.id)

  def find(id: Long): TodoList =
    this.lists.find(_.id == id).orNull

  def update(id: Long, list: TodoList): TodoList = {
    this.lists = this.lists.updated(this.lists.indexWhere(_.id == id), list)
    this.changes.addBinding(System.currentTimeMillis(), list.id)
    list
  }

  def allUpdatedSince(timestamp: Long, project_id: Long): List[TodoList] =
    this.changes.filterKeys(_ >= timestamp).values.toList.flatten.distinct.map(id => this.find(id))
      .filter(list => list.project_id == project_id)
}
