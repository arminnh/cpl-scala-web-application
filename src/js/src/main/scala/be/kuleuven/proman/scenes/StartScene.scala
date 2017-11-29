package be.kuleuven.proman.scenes

import be.kuleuven.proman._
import be.kuleuven.proman.models._

import scala.util.{Failure, Success}
import io.circe.syntax._
import io.circe.parser.decode
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.ext.Ajax.InputData
import org.scalajs.dom.html._

import scalatags.JsDom.all._
import org.scalajs.dom.raw.{Event, NodeListOf}

import scala.concurrent.ExecutionContext.Implicits.global // implicit ExecutionContext for Future tasks

//import io.circe.generic.auto._
//import io.circe.parser._

//noinspection AccessorLikeMethodIsUnit
object StartScene {
  lazy val todo_project_ui = new TODOProjectTemplate(scalatags.JsDom)

  def setupHTML(): Unit = {
    hideError()

    dom.document.title = "Todo Projects"
    dom.document.getElementById("top-title").innerHTML = div(
      h1("Todo Projects", fontSize := 36)
    ).render.innerHTML

    dom.document.getElementById("content").innerHTML = div(
      h2("Create a new project"),
      form(id := "form-create-project", action := "/projects/store", method := "post", cls := "form-inline")(
        div(cls := "form-group")(input(tpe := "text", name := "name", placeholder := "Project title", cls := "form-control")),
        button(tpe := "submit", cls := "btn", marginLeft := 15)("Create")
      ),
      h2("Open  a project"),
      div(id := "project-container")
    ).render.innerHTML

    dom.document.getElementById("form-create-project").asInstanceOf[Form].onsubmit = (e: Event) => {
      e.preventDefault()
      submitNewProject(e.srcElement.asInstanceOf[Form])
    }
  }

  def setupScene(): Unit = {
    setupHTML()

    // Fetch projects and display them in a table.
    Ajax.get("projects/json").onComplete {
      case Failure(error) => errorAlert(error)
      case Success(xhr) =>
        val projectsM = decode[Seq[TODOProject]](xhr.responseText)

        projectsM match {
          case Left(error) => errorAlert(error)
          case Right(projects) =>
            dom.document.getElementById("project-container").appendChild(todo_project_ui.multipleTemplate(projects).render)

            // Set event handlers on project anchors.
            val project_anchors = dom.document.getElementsByClassName("project-anchor").asInstanceOf[NodeListOf[Anchor]]
            for (i <- 0 until project_anchors.length) {
              val anchor = project_anchors.item(i)
              anchor.onclick = (e: Event) => {
                getProjectAndShow(anchor.getAttribute("data-id").toInt)
                // dom.window.history.pushState("", dom.document.title, dom.window.location.pathname)
              }
            }
        }
    }
  }

  def getProjectAndShow(id: Int): Unit = {
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
            ProjectScene.setupScene(project)
          }
        }
    }
  }

  // Submit a new project if possible and then load it if successful.
  def submitNewProject(form: Form): Unit = {
    hideError()
    println("form_new_project submit")

    val name = form.elements.namedItem("name").asInstanceOf[Input].value
    println("form_new_project input: " + name)

    if (name.length() == 0) {
      showError("Fill in a name first!")
    } else {
      val encodedName = scalajs.js.URIUtils.encodeURIComponent(name)
      println("encoded name: " + encodedName)
      Ajax.get("project/exists/" + encodedName).onComplete {
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

                Ajax.post(form.action, name.asJson.toString()).onComplete {
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
                      case Right(project) => ProjectScene.setupScene(project)
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

}
