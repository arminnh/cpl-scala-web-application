package be.kuleuven.proman

import be.kuleuven.proman.models._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global // implicit ExecutionContext for Future tasks


object ProManApp {

  def errorAlert(error: Any): Unit = {
    println(error.toString)
  }

  def showError(error: String): Unit = {
    val container = dom.document.body.querySelector("#error-container").asInstanceOf[HTMLDivElement]
    container.innerHTML = error
    container.style.display = ""
  }

  def hideError(): Unit = {
    val container = dom.document.body.querySelector("#error-container").asInstanceOf[HTMLDivElement]
    container.style.display = "none"
  }

  def getProject(id: Int, callback: TODOProject => Unit): Unit = {
    println("get project " + id)

    Ajax.get("/project/" + id).onComplete {
      case Failure(error) => errorAlert(error)
      case Success(xhr) => {
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
  }

  def loadProject(project: TODOProject): Unit = {
    println("loading project: " + project)
    val newTitle = "Project: " + project.name
    dom.document.title = newTitle
    dom.document.body.querySelector("#title").innerHTML = "Project: " + project.name

    val content = dom.document.body.querySelector("#content")
    content.innerHTML = project.toString

    // get project's TODO entries and display them
    //val todoTarget = dom.document.body.querySelector("")
    //todoTarget.innerHTML = ""
    //val ui = new TODOEntryTemplate(scalatags.JsDom)
    //todoTarget.appendChild(ui.multipleTemplate(todos).render)
  }

  // Submit a new project if possible and then load it if successful.
  def submitNewProject(form: HTMLFormElement, e: Event): Unit = {
    hideError()
    println("formNewProject submit")

    val name = form.elements.namedItem("name").asInstanceOf[HTMLInputElement].value
    println("formNewProject input: " + name)

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
                Ajax.post(form.action, new TODOProject(name).asJson.noSpaces).onComplete {
                  case Failure(error) => {
                    println("formNewProject submit failure")
                    errorAlert(error)
                  }
                  case Success(xhr2) => {
                    println("formNewProject submit success, " + xhr2.responseText)
                    form.reset()
                    val newProject = decode[TODOProject](xhr2.responseText)
                    println("newProject " + newProject)

                    newProject match {
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

  def main(args: Array[String]): Unit = {

    val formNewProject = dom.document.body.querySelector("#form-create-project").asInstanceOf[HTMLFormElement]
    formNewProject.onsubmit = (e: Event) => {
      e.preventDefault()
      submitNewProject(formNewProject, e)
    }

//    val a = dom.document.querySelector(".project-link").asInstanceOf[HTMLAnchorElement]
//    a.onclick = (e: Event) => {
//      e.preventDefault()
//      loadProject(a.getAttribute("data-id").toInt)
//    }

    val projectAnchors = dom.document.querySelectorAll(".project-anchor").asInstanceOf[NodeListOf[HTMLAnchorElement]]
    for (i <- 0 until projectAnchors.length) {
      val a = projectAnchors.item(i)
      a.onclick = (e: Event) => {
        e.preventDefault()
        getProject(a.getAttribute("data-id").toInt, loadProject)
        dom.window.history.pushState("", dom.document.title, dom.window.location.pathname)
      }
    }

  }
}
