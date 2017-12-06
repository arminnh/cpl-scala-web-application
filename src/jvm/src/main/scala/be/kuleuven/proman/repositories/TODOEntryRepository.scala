package be.kuleuven.proman.repositories

import be.kuleuven.proman.models.TODOEntry

object TODOEntryRepository {
    private var id: Long = 0
    private var entries: Seq[TODOEntry] = Seq()

    private def nextID: Long = { id += 1; id }

    def create(project_id: Long, name: String, is_done: Boolean=false): TODOEntry = {
      val entry = new TODOEntry(nextID, project_id, name, is_done)
      this.entries +:= entry
      entry
    }

    def all(): Seq[TODOEntry] = this.entries

    def allForProject(project_id: Long): Seq[TODOEntry] = this.entries.filter(_.project_id == project_id).sortWith(_.timestamp > _.timestamp)

    def find(id: Long): TODOEntry = this.entries.find(_.id == id).orNull

    def update(id: Long, todo: TODOEntry): TODOEntry = {
      this.entries = this.entries.updated(this.entries.indexWhere(_.id == id), todo)
      todo
    }

    def update(id: Long, name: String, is_done: Boolean): TODOEntry = {
      var entry = this.find(id)
      entry.text = name
      entry.is_done = is_done
      this.entries = this.entries.updated(this.entries.indexWhere(_.id == id), entry)
      entry
    }
}
