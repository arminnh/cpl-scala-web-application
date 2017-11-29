package be.kuleuven.proman.scenes

import be.kuleuven.proman.{errorAlert, formatTimeStamp, hideError, showError}
import be.kuleuven.proman.models._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.syntax._
import io.circe.parser.decode
import org.w3c.dom.html.HTMLInputElement

import scala.scalajs.js.Any
//import io.circe.generic.auto._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html._ // HTMLDivElement => Div
import scalatags.JsDom.all._ // Client side HTML Tags
import org.scalajs.dom.raw.{Event, NodeListOf}

//noinspection AccessorLikeMethodIsUnit
object ProjectScene {
  lazy val todo_entry_ui = new TODOEntryTemplate(scalatags.JsDom)

  def setupHTML(project: TODOProject): Unit = {
    hideError()

    dom.document.title = "Project: " + project.name
    dom.document.getElementById("top-title").innerHTML = div(
      h1("Project: " + project.name, fontSize := 36),
      button(id := "back-to-start", cls := "btn btn-xs btn-primary")(span(cls := "glyphicon glyphicon-arrow-left"), " back")
    ).render.innerHTML

    dom.document.getElementById("content").innerHTML = div(
      h2("Create a new entry"),
      form(id := "form-create-todo", action := s"/todos/${project.id}/store", method := "post", cls := "form-inline")(
        div(cls := "form-group")(input(tpe := "text", name := "name", placeholder := "Message", cls := "form-control")),
        button(tpe := "submit", cls := "btn btn-primary", marginLeft := 15)("Create")
      ),
      h2("Pending TODOs"),
      div(id := "pending-todo-container"),
      h2("Finished TODOs"),
      div(id := "finished-todo-container")
    ).render.innerHTML

    dom.document.getElementById("form-create-todo").asInstanceOf[Form].onsubmit = (e: Event) => {
      e.preventDefault()
      submitNewTodo(e.srcElement.asInstanceOf[Form])
    }

    dom.document.getElementById("back-to-start").asInstanceOf[Button].onclick = (e: Event) => {
      StartScene.setupScene()
    }
  }

  def setupScene(project: TODOProject): Unit = {
    println("loading project: " + project)

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

            val cells = dom.document.getElementsByClassName("todo-timestamp").asInstanceOf[NodeListOf[TableDataCell]]
            for (i <- 0 until cells.length) {
              val td = cells.item(i)
              td.innerHTML = formatTimeStamp(td.getAttribute("data-timestamp").toLong)
            }

            val edit_btns = dom.document.getElementsByClassName("todo-edit").asInstanceOf[NodeListOf[Button]]
            for (i <- 0 until edit_btns.length) {
              val btn = edit_btns.item(i)

              btn.onclick = { Any.fromFunction1((e: Event) => {
                val tr = btn.parentNode.parentNode.asInstanceOf[TableRow]
                val td_text = tr.getElementsByClassName("todo-text").item(0).asInstanceOf[TableDataCell]
                println("btn clicked" + btn)
                println("tr: " + tr)
                println("td_text in tr: " + td_text)

                println("td_text has child nodes: " + td_text.childNodes.length)
                for (j <- 0 until td_text.childNodes.length) {
                  println("node " + j + ": " + td_text.childNodes.item(j).toString + ", " + td_text.childNodes.item(j).textContent + ", " + td_text.childNodes.item(j).nodeName)
                }

                if (td_text.firstChild.nodeName == "#text") {
                  println("td_text no child nodes")
                  val input_node = input(tpe := "text", name := "text", placeholder := "Message",  cls := "form-control", value := td_text.innerHTML).render
                  td_text.innerHTML = ""
                  td_text.appendChild(input_node)

                  input_node.onkeydown = { Any.fromFunction1((e: Event) => {
                    Ajax.put("/todos/"+tr.getAttribute("data-id")+"/update", new TODOEntry(-999, -999, input_node.value).asJson.noSpaces).onComplete {
                      case Failure(error) => errorAlert(error)
                      case Success(todo) => println("Updated the todo: " + todo)
                    }
                  })}

                } else {
                  td_text.innerHTML = td_text.firstChild.asInstanceOf[Input].value
                }
              })}
            }

            val finished_btns = dom.document.getElementsByClassName("todo-finished").asInstanceOf[NodeListOf[Button]]
            for (i <- 0 until finished_btns.length) {
              val btn = finished_btns.item(i)
              btn.onclick = { Any.fromFunction1((e: Event) => {
                println("clicked on " + e.srcElement)
              })}
            }

            val pending_btns = dom.document.getElementsByClassName("todo-pending").asInstanceOf[NodeListOf[Button]]
            for (i <- 0 until pending_btns.length) {
              val btn = pending_btns.item(i)
              btn.onclick = { Any.fromFunction1((e: Event) => {
                println("clicked on " + e.srcElement)
              })}
            }
        }
    }
  }

  def submitNewTodo(form: Form): Unit = {
    hideError()
    println("submitNewTodo submit")

    val name = form.elements.namedItem("name").asInstanceOf[Input].value
    println("submitNewTodo input: " + name)

    if (name.length() == 0) {
      showError("Fill in a message first!")
    } else {
      Ajax.post(form.action, name.asJson.noSpaces).onComplete {
        case Failure(error) =>
          println("submitNewTodo submit failure")
          errorAlert(error)

        case Success(xhr) =>
          println("submitNewTodo submit success, " + xhr.responseText)
          form.reset()
          val new_todo = decode[TODOEntry](xhr.responseText)
          println("new_todo " + new_todo)

          new_todo match {
            case Left(error) => errorAlert(error)
            case Right(todo) =>
              val new_entry = this.todo_entry_ui.singleTemplate(todo).render

              // Format timestamp.
              val td_timestamp = new_entry.getElementsByClassName("todo-timestamp").asInstanceOf[NodeListOf[TableDataCell]].item(0)
              td_timestamp.innerHTML = formatTimeStamp(td_timestamp.getAttribute("data-timestamp").toLong)

              // Add new entry at top of table.
              val tbody = dom.document.getElementById("pending-todo-container").asInstanceOf[Div].firstChild.firstChild.firstChild
              tbody.insertBefore(new_entry, tbody.firstChild)
          }
      }
    }
  }

}
