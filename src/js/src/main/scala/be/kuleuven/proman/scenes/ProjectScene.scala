package be.kuleuven.proman.scenes

import be.kuleuven.proman.{errorAlert, formatTimeStamp, hideError, showError}
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
  var project_id: Long = -999
  var synchronisation_interval: Int = -999
  lazy val todo_entry_ui = new TODOEntryTemplate(scalatags.JsDom)

  def setupHTML(project: TODOProject): Unit = {
    hideError()

    dom.document.title = "Project: " + project.name
    dom.document.getElementById("top-title").innerHTML = div(
      h1("Project: " + project.name, fontSize := 36),
      button(id := "back-to-start", cls := "btn btn-xs btn-primary")(
        span(cls := "glyphicon glyphicon-arrow-left"), " back"
      )
    ).render.innerHTML

    dom.document.getElementById("content").innerHTML = div(
      h2("Create a new entry"),
      form(id := "form-create-todo", action := s"/todos/${project.id}/store", method := "post", cls := "form-inline")(
        div(cls := "form-group")(
          input(tpe := "text", name := "name", placeholder := "Message", cls := "form-control", autocomplete := "off")
        ),
        button(tpe := "submit", cls := "btn btn-primary", marginLeft := 15)("Create")
      ),
      h2("Pending TODOs"),
      div(id := "pending-todo-container")(
        div(cls := "table-responsive")(table(cls := "table table-condensed table-striped table-hover")(tbody))
      ),
      h2("Finished TODOs"),
      div(id := "finished-todo-container")(
        div(cls := "table-responsive")(table(cls := "table table-condensed table-striped table-hover")(tbody))
      )
    ).render.innerHTML

    dom.document.getElementById("form-create-todo").asInstanceOf[Form].onsubmit = Any.fromFunction1((e: Event) => {
      e.preventDefault()
      submitNewTodo(e.srcElement.asInstanceOf[Form])
    })

    dom.document.getElementById("back-to-start").asInstanceOf[Button].onclick = Any.fromFunction1(_ => {
      dom.window.clearInterval(this.synchronisation_interval)
      StartScene.setupScene()
    })
  }

  def setupScene(project: TODOProject): Unit = {
    setupHTML(project)
    this.project_id = project.id
    this.state = -999
    synchronise()
    this.synchronisation_interval = dom.window.setInterval(Any.fromFunction0(() => synchronise()), 2500)
  }

  /**
    * Updates a todo's is_done value.
    * @param tr: The row that represents the todo entry.
    * @param is_done: The new is_done value.
    */
  def updateTODOIsDoneStatus(tr: TableRow, is_done: Boolean): Unit = {
    val todoM = decode[TODOEntry](tr.getAttribute("data-json"))

    todoM match {
      case Left(error) => errorAlert(error)
      case Right(todo) =>
        todo.is_done = is_done

        Ajax.put(s"/todos/${todo.id}/update", todo.asJson.noSpaces).onComplete {
          case Failure(error) => errorAlert(error)
          case Success(xhr) =>
            val updatedM = decode[TODOEntry](xhr.responseText)

            // move tr to other table
            tr.parentNode.removeChild(tr)
            createTodoInTable(todo)

            updatedM match {
              case Left(error) => errorAlert(error)
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
    val todoM = decode[TODOEntry](tr.getAttribute("data-json"))

    todoM match {
      case Left(error) => errorAlert(error)
      case Right(todo) =>
        todo.text = text

        Ajax.put(s"/todos/${todo.id}/update", todo.asJson.noSpaces).onComplete {
          case Failure(error) => errorAlert(error)
          case Success(xhr) =>
            val updatedM = decode[TODOEntry](xhr.responseText)

            updatedM match {
              case Left(error) => errorAlert(error)
              case Right(updated) => tr.setAttribute("data-json", updated.asJson.noSpaces)
            }
        }
    }
  }

  /**
    * Creates a new TableRow in the correct table for a TODOEntry.
    * @param new_todo: The new TODOEntry.
    */
  def createTodoInTable(new_todo: TODOEntry): Unit = {
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
    setupTodoTableRow(newRow.asInstanceOf[TableRow])
  }

  /**
    * Sets up event handlers for a TODOEntry's TableRow.
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
      Ajax.post(form.action, new TODOEntry(name).asJson.noSpaces).onComplete {
        case Failure(error) => errorAlert(error)

        case Success(xhr) =>
          form.reset()
          val new_todo = decode[TODOEntry](xhr.responseText)

          new_todo match {
            case Left(error) => errorAlert(error)
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
    println("synchronising for state: " + this.state)

    Ajax.get("todos/sync/" + this.project_id + "/" + this.state).onComplete {
      case Failure(error) => errorAlert(error)
      case Success(xhr) =>
        parse(xhr.responseText) match {
          case Left(error) => errorAlert(error)
          case Right(json) =>
            val todos: Seq[TODOEntry] = json.hcursor.downField("todos").as[Seq[TODOEntry]].getOrElse(List())
            this.updateTodos(todos)

            this.state = json.hcursor.downField("state").as[Long].getOrElse(this.state)
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
  def updateTodos(todos: Seq[TODOEntry]): Unit = {
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
