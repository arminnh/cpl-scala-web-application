package be.kuleuven.proman

import be.kuleuven.proman.models._
import be.kuleuven.proman.repositories.TODOProjectRepository
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

  def loadProject(id: Int): Unit = {
    println("loading project " + id)

    Ajax.get("/projects/" + id).onComplete {
      case Failure(error) => errorAlert(error)
      case Success(xhr) => {
        println(xhr)
        println(xhr.responseText)
        val projectM = decode[TODOProject](xhr.responseText)

        projectM match {
          case Left(error) => errorAlert(error)
          case Right(project) => {
            println(project)
          }
        }
      }
    }
  }

  def main(args: Array[String]): Unit = {

    val formNewProject = dom.document.body.querySelector("#form-create-project").asInstanceOf[HTMLFormElement]
    formNewProject.onsubmit = (e: Event) => {
      e.preventDefault()
      println("formNewProject submit")

      val name = formNewProject.elements.namedItem("name").asInstanceOf[HTMLInputElement].value
      println("formNewProject input: " + name)

      Ajax.post(formNewProject.action, new TODOProject(name).asJson.noSpaces)
        .onComplete {
          case Failure(error) => {
            println("formNewProject submit failure")
            errorAlert(error)
          }
          case Success(xhr) => {
            println("formNewProject submit success")
            println(xhr.responseText)
            formNewProject.reset()
            loadProject(1)
            dom.window.location.reload(true)
          }
        }
    }

//    val a = dom.document.querySelector(".project-link").asInstanceOf[HTMLAnchorElement]
//    a.onclick = (e: Event) => {
//      e.preventDefault()
//      loadProject(a.getAttribute("data-id").toInt)
//    }

    val projectAnchors = dom.document.querySelectorAll(".project-link").asInstanceOf[NodeListOf[HTMLAnchorElement]]
    for (i <- 0 until projectAnchors.length) {
      val a = projectAnchors.item(i)
      a.onclick = (e: Event) => {
        e.preventDefault()
        loadProject(a.getAttribute("data-id").toInt)
      }
    }

//    def update() =
//      Ajax.get("/").onComplete {
//        case Failure(error) => errorAlert(error)
//        case Success(xhr) => {
//            val todosM = decode[Seq[TODOEntry]](xhr.responseText)
//
//            todosM match {
//              case Left(error) => errorAlert(error)
//              case Right(todos) =>
//                val todoTarget = dom.document.body.querySelector("")
//                todoTarget.innerHTML = ""
//                val ui = new TODOEntryTemplate(scalatags.JsDom)
//                todoTarget.appendChild(ui.multipleTemplate(todos).render)
//            }
//          }
//      }
//
//    update()
//
//    val addProjectEl = dom.document.body.querySelector("").asInstanceOf[HTMLButtonElement]
//    addProjectEl.onclick = (ev: Event) => {
//      val projectNameEl = dom.document.body.querySelector("").asInstanceOf[HTMLInputElement]
//      val projectName = projectNameEl.value
//
//      Ajax.post("", new TODOEntry(projectName).asJson.noSpaces)
//        .onComplete {
//          case Failure(error) => errorAlert(error)
//          case Success(xhr) => {
//            projectNameEl.value = ""
//            update()
//          }
//        }
//    }

  }
}
