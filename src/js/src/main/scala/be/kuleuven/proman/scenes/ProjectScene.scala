package be.kuleuven.proman.scenes

import be.kuleuven.proman.{errorAlert, formatTimeStamp, hideError, showError}
import be.kuleuven.proman.models._

import scala.util.{Failure, Success}
import scala.util.control.Breaks._
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.syntax._
import io.circe.parser.decode
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
  var project_id: Int = -999
  lazy val todo_entry_ui = new TODOEntryTemplate(scalatags.JsDom)

  def setupHTML(project: TODOProject): Unit = {
    this.project_id = project.id
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
      div(id := "pending-todo-container"),
      h2("Finished TODOs"),
      div(id := "finished-todo-container")
    ).render.innerHTML

    dom.document.getElementById("form-create-todo").asInstanceOf[Form].onsubmit = Any.fromFunction1((e: Event) => {
      e.preventDefault()
      submitNewTodo(e.srcElement.asInstanceOf[Form])
    })

    dom.document.getElementById("back-to-start").asInstanceOf[Button].onclick = Any.fromFunction1(_ => {
      StartScene.setupScene()
    })
  }

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

  def updateTODOText(tr: TableRow, input_node: Input): Unit = {
    val todoM = decode[TODOEntry](tr.getAttribute("data-json"))

    todoM match {
      case Left(error) => errorAlert(error)
      case Right(todo) =>
        todo.text = input_node.value

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

  def createTodoInTable(new_todo: TODOEntry): Unit = {
    val containerID = if (new_todo.is_done) "finished-todo-container" else "pending-todo-container"
    val tbody = dom.document.getElementById(containerID).getElementsByTagName("tbody").item(0).asInstanceOf[TableSection]
    println("found tbody, children: " + tbody.childElementCount)

    // Find index where to insert the new row at.
    var i = 0
    breakable {
      while (i < tbody.childElementCount) {
        val timestamp = tbody.childNodes.item(i).asInstanceOf[TableRow]
          .getElementsByClassName("todo-timestamp").item(0).asInstanceOf[TableCell]
          .getAttribute("data-timestamp").toLong

        if (new_todo.timestamp < timestamp) {
          i += 1
        } else {
          println("break")
          break
        }
      }
    }

    println("Position: " + i)
    // Insert the new todo row.
    val tempRow = tbody.insertRow(i)
    val newRow = tbody.insertBefore(todo_entry_ui.singleTemplate(new_todo).render, tempRow)
    tbody.removeChild(tempRow)
    setupTodoTableRow(newRow.asInstanceOf[TableRow])
  }

  def setupScene(project: TODOProject): Unit = {
    setupHTML(project)

    // Fetch project's todos and display them in tables.
    Ajax.get(s"/todos/${project.id}/json").onComplete {
      case Failure(error) => errorAlert(error)
      case Success(xhr) =>
        val todosM = decode[Seq[TODOEntry]](xhr.responseText)

        todosM match {
          case Left(error) => errorAlert(error)
          case Right(todos) =>

            val pending_todo_target = dom.document.getElementById("pending-todo-container")
            pending_todo_target.appendChild(this.todo_entry_ui.multipleTemplate(todos.filter(_.is_done == false)).render)

            val finished_todo_target = dom.document.getElementById("finished-todo-container")
            finished_todo_target.appendChild(this.todo_entry_ui.multipleTemplate(todos.filter(_.is_done == true)).render)

            val trs = dom.document.getElementsByTagName("tr").asInstanceOf[NodeListOf[TableRow]]
            for (i <- 0 until trs.length) {
              val tr = trs.item(i)
              setupTodoTableRow(tr)
            }
        }
    }
  }

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

        if (td_text.firstChild.nodeName != "#text") {
          // replace input box by it's value
          td_text.innerHTML = td_text.firstChild.asInstanceOf[Input].value
        } else {
          // replace text by a new input box
          val input_node = input(tpe := "text", name := "text", placeholder := "Message",  cls := "form-control", value := td_text.innerHTML, height := 24).render
          td_text.innerHTML = ""
          td_text.appendChild(input_node)

          // Update todo text. every keystroke is an update
          input_node.onkeydown = Any.fromFunction1((e: KeyboardEvent) => {
            if (e.keyCode == 13) {
              td_text.innerHTML = input_node.value
            } else {
              updateTODOText(tr, input_node)
            }
          })

          input_node.focus()
          input_node.setSelectionRange(input_node.value.length, input_node.value.length)
        }
      })
    }
  }

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

}
