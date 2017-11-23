package be.kuleuven.proman.models

class TODOProject(var name: String, var todos: List[TODOEntry]=List()) {

  override def toString: String = {
    "TODO Project with name: %s and todos: \n%s\n".format(name, todos.map(_.toString))
  }
}
