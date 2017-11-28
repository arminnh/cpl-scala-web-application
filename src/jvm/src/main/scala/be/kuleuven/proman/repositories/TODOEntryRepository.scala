package be.kuleuven.proman.repositories

import be.kuleuven.proman.models.TODOEntry

object TODOEntryRepository {
    private var id = 0
    private var entries: Seq[TODOEntry] = Seq()

    private def nextID: Int = {
      id += 1
      id
    }

    def create(project_id: Int, name: String, is_done: Boolean=false): TODOEntry = {
      val entry = new TODOEntry(nextID, project_id, name)
      entry.is_done = is_done
      this.entries :+= entry
      entry
    }

    def all(): Seq[TODOEntry] = this.entries

    def allForProject(project_id: Int): Seq[TODOEntry] = this.entries.filter(_.project_id == project_id)

    def find(id: Int): TODOEntry = this.entries.find(_.id == id).orNull

    def update(id: Int, name: String): TODOEntry = {
      var entry = this.find(id)
      entry.text = name
      this.entries = this.entries.updated(this.entries.indexWhere(_.id == id), entry)
      entry
    }
}
