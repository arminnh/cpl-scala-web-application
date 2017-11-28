package be.kuleuven.proman

import be.kuleuven.proman.models._

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw._
import dom.html._ // HTMLDivElement => Div
import scalatags.JsDom.all._ // Client side HTML Tags
import scala.scalajs.js.Date
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global // implicit ExecutionContext for Future tasks


//noinspection AccessorLikeMethodIsUnit
object ProManApp {

  // Error: Referring to non-existent class java.text.SimpleDateFormat
  def formatTimeStamp(ts: Long): String = {
    val date = new Date(ts)
    date.getFullYear() + "-" + date.getMonth() + "-" + date.getDay() + " " +
      date.getHours() + ":" + date.getMinutes() + ":" + date.getSeconds()
  }

  def errorAlert(error: Throwable): Unit = {
    println("Throwable error: " + error)
    println(error.getLocalizedMessage)
    println(error.printStackTrace())
  }

  def errorAlert(error: Error): Unit = {
    println("Error: " + error)
    println(error.getLocalizedMessage)
    println(error.printStackTrace())
  }

  def showError(error: String): Unit = {
    val container = dom.document.getElementById("error-container").asInstanceOf[Div]
    container.innerHTML = error
    container.style.display = ""
  }

  def hideError(): Unit = {
    val container = dom.document.getElementById("error-container").asInstanceOf[Div]
    container.style.display = "none"
  }

  def getProject(id: Int, callback: TODOProject => Unit): Unit = {
    println("get project " + id)

    Ajax.get("/project/" + id).onComplete {
      case Failure(error) => errorAlert(error)
      case Success(xhr) =>
        println("success, " + xhr.responseText)
        val projectM = decode[TODOProject](xhr.responseText)

        projectM match {
          case Left(error) => errorAlert(error)
          case Right(project) => {
            println("loading project " + project)
            callback(project)
          }
        }
    }
  }

  def loadProject(project: TODOProject): Unit = {
    println("loading project: " + project)
    val new_title = "Project: " + project.name
//    dom.document.title = new_title
    dom.document.body.querySelector("#title").innerHTML = "Project: " + project.name

    val content = dom.document.getElementById("content")
    content.innerHTML = ""

    // add html for showing a project with its todos
    content.appendChild(h2("Create a new entry").render)
    content.appendChild(
      form(id := "form-create-todo", action := s"/project/${project.id}/store", method := "post", cls := "form-inline")(
        div(cls := "form-group")(input(tpe := "text", name := "name", placeholder := "Message", cls := "form-control")),
        button(tpe := "submit", cls := "btn", marginLeft := 15)("Create")
      ).render)
    content.appendChild(h2("Pending TODOs").render)
    content.appendChild(div(id := "pending-todo-container").render)
    content.appendChild(h2("Finished TODOs").render)
    content.appendChild(div(id := "finished-todo-container").render)

    // get project's todos and display them
    Ajax.get(s"/project/${project.id}/todos").onComplete {
      case Failure(error) => errorAlert(error)
      case Success(xhr) =>
        val todosM = decode[Seq[TODOEntry]](xhr.responseText)

        todosM match {
          case Left(error) => errorAlert(error)
          case Right(todos) =>
            val ui = new TODOEntryTemplate(scalatags.JsDom)

            val pending_todo_target = dom.document.getElementById("pending-todo-container")
            pending_todo_target.appendChild(ui.multipleTemplate(todos.filter(_.is_done == false)).render)

            val finished_todo_target = dom.document.getElementById("finished-todo-container")
            finished_todo_target.appendChild(ui.multipleTemplate(todos.filter(_.is_done == true)).render)

            val cells = dom.document.getElementsByClassName("todo-timestamp").asInstanceOf[NodeListOf[TableDataCell]]
            for (i <- 0 until cells.length) {
              val td = cells.item(i)
              td.innerHTML = formatTimeStamp(td.getAttribute("data-timestamp").toLong)
            }

            val edit_btns = dom.document.getElementsByClassName("todo-edit").asInstanceOf[NodeListOf[Button]]
            for (i <- 0 until edit_btns.length) {
              val btn = edit_btns.item(i)
              btn.onclick = (e: Event) => {
                println("clicked on " + e.srcElement)
              }
            }

            val finished_btns = dom.document.getElementsByClassName("todo-finished").asInstanceOf[NodeListOf[Button]]
            for (i <- 0 until finished_btns.length) {
              val btn = finished_btns.item(i)
              btn.onclick = (e: Event) => {
                println("clicked on " + e.srcElement)
              }
            }

            val pending_btns = dom.document.getElementsByClassName("todo-pending").asInstanceOf[NodeListOf[Button]]
            for (i <- 0 until pending_btns.length) {
              val btn = pending_btns.item(i)
              btn.onclick = (e: Event) => {
                println("clicked on " + e.srcElement)
              }
            }
        }
    }

    // set event handler on new todo form
    val form_new_todo = dom.document.getElementById("form-create-todo").asInstanceOf[Form]
    form_new_todo.onsubmit = (e: Event) => {
      submitNewTodo(form_new_todo, e)
    }
  }

  // Submit a new project if possible and then load it if successful.
  def submitNewProject(form: Form, e: Event): Unit = {
    e.preventDefault()
    hideError()
    println("form_new_project submit")

    val name = form.elements.namedItem("name").asInstanceOf[Input].value
    println("form_new_project input: " + name)

    if (name.length() == 0) {
      showError("Fill in a name first!")
    } else {
      Ajax.get("project/exists/" + name).onComplete {
        case Failure(error) => errorAlert(error)
        case Success(xhr) => {
          val existsM = decode[Boolean](xhr.responseText)

          existsM match {
            case Left(error) => errorAlert(error)
            case Right(exists) => {
              println("project exists?: " + exists)

              if (exists) {
                showError("A project with that name already exists! Try again with a different name.")
              } else {
                Ajax.post(form.action, name.asJson.noSpaces).onComplete {
                  case Failure(error) => {
                    println("form_new_project submit failure")
                    errorAlert(error)
                  }
                  case Success(xhr2) => {
                    println("form_new_project submit success, " + xhr2.responseText)
                    form.reset()
                    val new_project = decode[TODOProject](xhr2.responseText)
                    println("new_project " + new_project)

                    new_project match {
                      case Left(error) => errorAlert(error)
                      case Right(project) => loadProject(project)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  def submitNewTodo(form: Form, e: Event): Unit = {
    e.preventDefault()
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
              val ui = new TODOEntryTemplate(scalatags.JsDom)

              val tbody = dom.document.getElementById("pending-todo-container").asInstanceOf[Div].firstChild.firstChild
              tbody.insertBefore(ui.singleTemplate(todo).render, tbody.firstChild)
          }
      }
    }
  }

  def main(args: Array[String]): Unit = {

    val form_new_project = dom.document.getElementById("form-create-project").asInstanceOf[Form]
    form_new_project.onsubmit = (e: Event) => {
      submitNewProject(form_new_project, e)
    }

    val project_anchors = dom.document.querySelectorAll(".project-anchor").asInstanceOf[NodeListOf[Anchor]]
    for (i <- 0 until project_anchors.length) {
      val anchor = project_anchors.item(i)
      anchor.onclick = (e: Event) => {
        e.preventDefault()
        hideError()
        getProject(anchor.getAttribute("data-id").toInt, loadProject)
        // dom.window.history.pushState("", dom.document.title, dom.window.location.pathname)
      }
    }

  }
}
