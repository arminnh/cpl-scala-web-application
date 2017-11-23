class TODOProject(var name: String, var todos: List[TODOEntry]=List()) {

  override def toString: String = {
    "TODO Project with name: %s"
  }
}
