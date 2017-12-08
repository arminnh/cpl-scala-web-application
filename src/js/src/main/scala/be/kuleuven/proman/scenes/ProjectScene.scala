package be.kuleuven.proman.scenes

import be.kuleuven.proman.{printError, formatTimeStamp, hideError, showError, getFormFromEvent}
import be.kuleuven.proman.models._

import scala.util.{Failure, Success}
import scala.util.control.Breaks._
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.syntax._, io.circe.parser._, io.circe.Json
import cats.syntax.either._
import org.scalajs.dom.raw.{HTMLElement, KeyboardEvent, MouseEvent}
//import io.circe.generic.auto._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html._ // HTMLDivElement => Div
import org.scalajs.dom.raw.{Event, NodeListOf}
import scalatags.JsDom.all._ // Client side HTML Tags
import scala.scalajs.js.Any

//noinspection AccessorLikeMethodIsUnit
object ProjectScene {
  var state: Long = -999
  var project: TodoProject = _
  var synchronisation_interval: Int = -999
  lazy val todo_entry_ui = new TodoEntryTemplate(scalatags.JsDom)

  def setupHTML(): Unit = {
    hideError()

    dom.document.title = "Project: " + this.project.name
    dom.document.getElementById("top-title").innerHTML = div(
      h1("Project: " + this.project.name, fontSize := 36),
      div(cls := "clearfix")(
        div(cls := "pull-left")(
          p(id := "project-description", whiteSpace := "pre-wrap")(
            this.project.description
          )
        ),
        div(cls := "pull-right")(
          button(id := "btn-edit-description", cls := "btn btn-xs btn-default", title:= "edit")(
            span(cls := "glyphicon glyphicon-pencil"), " edit"
          )
        )
      ),
      form(id := "form-update-description", name := "form-update-description", action := s"/project/${this.project.id}", method := "put", display := "none")(
        br,
        div(cls := "form-group")(
          textarea(name := "description", placeholder := "The description of the project", cls := "form-control")
        ),
        button(tpe := "submit", cls := "btn btn-primary")("Submit")
      )
    ).render.innerHTML

    dom.document.getElementById("content").innerHTML = div(
      button(id := "back-to-start", cls := "btn btn-xs btn-primary", marginTop := 5)(
        span(cls := "glyphicon glyphicon-arrow-left"), " back"
      ),
      div(cls := "row")(
        div(cls := "col-sm-6")(
          h3("Create a new entry"),
          form(id := "form-create-todo", name := "form-create-todo", action := s"/todos/${this.project.id}/store", method := "post", cls := "form-inline")(
            div(cls := "form-group")(
              input(tpe := "text", name := "name", placeholder := "Message",
                    cls := "form-control", autocomplete := "off", marginRight := 15)
            ),
            button(tpe := "submit", cls := "btn btn-primary")("Create")
          )
        ),
        div(cls := "col-sm-6")(
          h3("Search"),
          div(cls := "form-group")(
            input(tpe := "text", name := "filter", placeholder := "Search by todo text", cls := "form-control", autocomplete := "off")
          )
        )
      ),
      h3("Pending todos"),
      div(id := "pending-todo-container")(
        div(cls := "table-responsive")(
          table(cls := "table table-condensed table-striped table-hover")(
            tbody
          )
        )
      ),
      h3("Finished todos ", button(id := "finished-toggle", cls := "btn btn-sm btn-default")(span(cls := "caret caret-up"))),
      div(id := "finished-todo-container")(
        div(cls := "table-responsive")(
          table(cls := "table table-condensed table-striped table-hover")(
            tbody
          )
        )
      )
    ).render.innerHTML

    dom.document.getElementById("btn-edit-description").asInstanceOf[Button].onclick = Any.fromFunction1(_ => {
      val form = dom.document.getElementById("form-update-description").asInstanceOf[Form]
      val p = dom.document.getElementById("project-description").asInstanceOf[Paragraph]

      if (form.style.display == "none") {
        form.elements.namedItem("description").asInstanceOf[TextArea].value = this.project.description
        form.style.display = "block"
        p.style.display = "none"
      } else {
        form.style.display = "none"
        p.style.display = "block"
      }
    })

    dom.document.getElementById("form-update-description").asInstanceOf[Form].onsubmit = Any.fromFunction1((e: Event) => {
      e.preventDefault()
      this.submitProjectUpdate(getFormFromEvent(e))
    })

    dom.document.getElementById("form-create-todo").asInstanceOf[Form].onsubmit = Any.fromFunction1((e: Event) => {
      e.preventDefault()
      this.submitNewTodo(getFormFromEvent(e))
    })

    dom.document.getElementById("back-to-start").asInstanceOf[Button].onclick = Any.fromFunction1(_ => {
      dom.window.clearInterval(this.synchronisation_interval)
      StartScene.setupScene()
    })

    val finished_toggle = dom.document.getElementById("finished-toggle").asInstanceOf[Button]
    finished_toggle.onclick = Any.fromFunction1(_ => {
      val container = dom.document.getElementById("finished-todo-container").asInstanceOf[Div]
      container.style.display = if (container.style.display == "none") "block" else "none"
      finished_toggle.firstChild.asInstanceOf[Span].classList.toggle("caret-up")
    })
  }

  def setupScene(project: TodoProject): Unit = {
    this.project = project
    this.state = -999
    this.setupHTML()
    this.synchronise()
    this.synchronisation_interval = dom.window.setInterval(Any.fromFunction0(() => synchronise()), 2500)
  }

  /**
    * Updates a todo's is_done value.
    * @param tr: The row that represents the todo entry.
    * @param is_done: The new is_done value.
    */
  def updateTODOIsDoneStatus(tr: TableRow, is_done: Boolean): Unit = {
    val todoM = decode[TodoEntry](tr.getAttribute("data-json"))

    todoM match {
      case Left(error) => printError(error)
      case Right(todo) =>
        todo.is_done = is_done

        Ajax.put(s"/todos/${todo.id}/update", todo.asJson.noSpaces).onComplete {
          case Failure(error) => printError(error)
          case Success(xhr) =>
            val updatedM = decode[TodoEntry](xhr.responseText)

            // move tr to other table
            tr.parentNode.removeChild(tr)
            createTodoInTable(todo)

            updatedM match {
              case Left(error) => printError(error)
              case Right(updated) => tr.setAttribute("data-json", updated.asJson.noSpaces)
            }
        }
    }
  }

  /**
    * Updates a todo's text value.
    * @param tr: The row that represents the todo entry.
    * @param text: The the new text value.
    */
  def updateTODOText(tr: TableRow, text: String): Unit = {
    val todoM = decode[TodoEntry](tr.getAttribute("data-json"))

    todoM match {
      case Left(error) => printError(error)
      case Right(todo) =>
        todo.text = text

        Ajax.put(s"/todos/${todo.id}/update", todo.asJson.noSpaces).onComplete {
          case Failure(error) => printError(error)
          case Success(xhr) =>
            val updatedM = decode[TodoEntry](xhr.responseText)

            updatedM match {
              case Left(error) => printError(error)
              case Right(updated) => tr.setAttribute("data-json", updated.asJson.noSpaces)
            }
        }
    }
  }

  /**
    * Creates a new TableRow in the correct table for a TodoEntry.
    * @param new_todo: The new TodoEntry.
    */
  def createTodoInTable(new_todo: TodoEntry): Unit = {
    val containerID = if (new_todo.is_done) "finished-todo-container" else "pending-todo-container"
    val tbody = dom.document.getElementById(containerID).getElementsByTagName("tbody").item(0).asInstanceOf[TableSection]

    // Find index where to insert the new row at. The entries are in descending timestamp order.
    var i = 0
    breakable {
      while (i < tbody.childElementCount) {
        val timestamp = tbody.childNodes.item(i).asInstanceOf[TableRow]
          .getElementsByClassName("todo-timestamp").item(0).asInstanceOf[TableCell]
          .getAttribute("data-timestamp").toLong

        if (new_todo.timestamp < timestamp) {
          i += 1
        } else {
          break
        }
      }
    }

    // Create a new row at the correct position.
    val tempRow = tbody.insertRow(i)
    val newRow = tbody.insertBefore(todo_entry_ui.singleTemplate(new_todo).render, tempRow)
    tbody.removeChild(tempRow)
    this.setupTodoTableRow(newRow.asInstanceOf[TableRow])
  }

  /**
    * Sets up event handlers for a TodoEntry's TableRow.
    * @param tr: The TableRow to set the handlers on.
    */
  def setupTodoTableRow(tr: TableRow): Unit = {
    val timestamp_tds = tr.getElementsByClassName("todo-timestamp").asInstanceOf[NodeListOf[TableDataCell]]
    for (i <- 0 until timestamp_tds.length) {
      val td = timestamp_tds.item(i)
      td.innerHTML = formatTimeStamp(td.getAttribute("data-timestamp").toLong)
    }

    val finished_btns = tr.getElementsByClassName("todo-finished").asInstanceOf[NodeListOf[Button]]
    for (i <- 0 until finished_btns.length) {
      val btn = finished_btns.item(i)
      btn.onclick = Any.fromFunction1(_ => { updateTODOIsDoneStatus(tr, is_done=false) })
    }

    val pending_btns = tr.getElementsByClassName("todo-pending").asInstanceOf[NodeListOf[Button]]
    for (i <- 0 until pending_btns.length) {
      val btn = pending_btns.item(i)
      btn.onclick = Any.fromFunction1(_ => { updateTODOIsDoneStatus(tr, is_done=true) })
    }

    val edit_btns = tr.getElementsByClassName("todo-edit").asInstanceOf[NodeListOf[Button]]
    for (i <- 0 until edit_btns.length) {
      val btn = edit_btns.item(i)
      btn.onclick = Any.fromFunction1(_ => {
        val td_text = tr.getElementsByClassName("todo-text").item(0).asInstanceOf[TableDataCell]

        if (td_text.hasChildNodes() && td_text.firstChild.nodeName != "#text") {
          // replace input box by it's value
          td_text.innerHTML = td_text.firstChild.asInstanceOf[Input].value
        } else {
          // replace text by a new input box
          val input_node = input(tpe := "text", name := "text", placeholder := "Message",  cls := "form-control",
                                 value := td_text.innerHTML, height := 24).render
          td_text.innerHTML = ""
          td_text.appendChild(input_node)

          // Update todo text. every keystroke is an update
          input_node.onkeyup = Any.fromFunction1((e: KeyboardEvent) => {
            if (e.keyCode == 13) {
              td_text.innerHTML = input_node.value
            }

            updateTODOText(tr, input_node.value)
          })

          input_node.focus()
          input_node.setSelectionRange(input_node.value.length, input_node.value.length)
        }
      })
    }
  }

  /**
    * Submits an update to the currently opened project's description.
    * @param form: The form to be submitted.
    */
  def submitProjectUpdate(form: Form): Unit = {
    val description = form.elements.namedItem("description").asInstanceOf[TextArea].value
    this.project.description = description

    Ajax.put(form.action, this.project.asJson.noSpaces).onComplete {
      case Failure(error) => printError(error)
      case Success(xhr) =>
        val updatedM = decode[TodoProject](xhr.responseText)

        updatedM match {
          case Left(error) => printError(error)
          case Right(updated) =>
            this.project = updated
            dom.document.getElementById("project-description").innerHTML = description
            dom.document.getElementById("btn-edit-description").asInstanceOf[Button].click()
        }
    }

  }

  /**
    * Submits a new todo entry and adds it to the correct table if it has been created successfully.
    * @param form: The form to be submitted.
    */
  def submitNewTodo(form: Form): Unit = {
    hideError()

    val name = form.elements.namedItem("name").asInstanceOf[Input].value

    if (name.length() == 0) {
      showError("Fill in a message first!")
    } else {
      //Ajax.post(form.action, name.asJson.noSpaces).onComplete {
      Ajax.post(form.action, new TodoEntry(name).asJson.noSpaces).onComplete {
        case Failure(error) => printError(error)

        case Success(xhr) =>
          form.reset()
          val new_todo = decode[TodoEntry](xhr.responseText)

          new_todo match {
            case Left(error) => printError(error)
            case Right(todo) => createTodoInTable(todo)
          }
      }
    }
  }

  /**
    * Synchronisation policy: Send the local state variable to the synchronisation route. The JSON response
    * of the server will contain the latest state variable and a list of todos that have been updated or
    * created since the last synchronisation.
    */
  def synchronise(): Unit = {
    Ajax.get("todos/sync/" + this.state + "/" + this.project.id + "/" + this.project.version).onComplete {
      case Failure(error) => printError(error)
      case Success(xhr) =>
        println("synchronising for state: " + this.state + ", response: " + xhr.responseText)
        parse(xhr.responseText) match {
          case Left(error) => printError(error)
          case Right(json) =>
            val todos: Seq[TodoEntry] = json.hcursor.downField("todos").as[Seq[TodoEntry]].getOrElse(List())
            this.updateTodos(todos)

            this.state = json.hcursor.downField("state").as[Long].getOrElse(this.state)

            val project: TodoProject = json.hcursor.downField("project").as[TodoProject].getOrElse(null)
            if (project != null) {
              this.project = project
              dom.document.getElementById("project-description").innerHTML = this.project.description
            }
        }
    }
  }

  /**
    * Updates the todos on the page. If an entry is new, a new row is created. If an entry has been updated,
    * then the row is replaced by its updated version, unless it is being edited. An entry is being edited
    * if an input element exists in the row of the entry.
    *
    * @param todos List of updated or new todos since last synchronisation.
    */
  def updateTodos(todos: Seq[TodoEntry]): Unit = {
    todos.foreach(todo => {
      val rows = dom.document.querySelectorAll(s"""[data-id="${todo.id}"]""").asInstanceOf[NodeListOf[TableRow]]
      if (rows.length > 0) {
        val row = rows.item(0)
        // Only replace the row if it is not being edited. No requirement is given for the case where multiple people
        // are editing an entry. In this case, the entry will get the text of the input box that was updated the latest.
        if (row.getElementsByTagName("input").length == 0) {
          row.parentElement.removeChild(row)
          this.createTodoInTable(todo)
        }
      } else {
        this.createTodoInTable(todo)
      }
    })
  }

}
