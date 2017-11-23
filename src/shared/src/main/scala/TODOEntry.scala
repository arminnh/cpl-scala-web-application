import java.time.LocalDateTime

class TODOEntry(var text: String, var date: LocalDateTime, var is_done: Boolean=false) {
  val id: Int = TODOEntry.nextID

  override def toString: String = {
    "TODO on date: %s, is done: %b, with text: %s".format(this.date, this.is_done, this.text)
  }
}

object TODOEntry {
  private var id = 0

  private def nextID = {
    id += 1
    id
  }
}