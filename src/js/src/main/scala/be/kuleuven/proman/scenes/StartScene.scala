package be.kuleuven.proman.scenes

import be.kuleuven.proman.{errorAlert, formatTimeStamp, hideError, showError}
import be.kuleuven.proman.models._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global // implicit ExecutionContext for Future tasks
import io.circe.syntax._
import io.circe.parser.decode
//import io.circe.generic.auto._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html._ // HTMLDivElement => Div
import org.scalajs.dom.raw.{Event, NodeListOf}
import scalatags.JsDom.all._ // Client side HTML Tags
import scala.scalajs.js.Any

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

    dom.document.getElementById("form-create-project").asInstanceOf[Form].onsubmit = Any.fromFunction1((e: Event) => {
      e.preventDefault()
      submitNewProject(e.srcElement.asInstanceOf[Form])
    })
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
              anchor.onclick = Any.fromFunction1(_ => {
                getProjectAndShow(anchor.getAttribute("data-id").toInt)
                // dom.window.history.pushState("", dom.document.title, dom.window.location.pathname)
              })
            }
        }
    }
  }

  def getProjectAndShow(id: Int): Unit = {
    Ajax.get("/project/" + id).onComplete {
      case Failure(error) => errorAlert(error)
      case Success(xhr) =>
        val projectM = decode[TODOProject](xhr.responseText)

        projectM match {
          case Left(error) => errorAlert(error)
          case Right(project) => {
            ProjectScene.setupScene(project)
          }
        }
    }
  }

  // Submit a new project if possible and then load it if successful.
  def submitNewProject(form: Form): Unit = {
    hideError()

    val name = form.elements.namedItem("name").asInstanceOf[Input].value
    if (name.length() == 0) {
      showError("Fill in a name first!")
    } else {
      val encodedName = scalajs.js.URIUtils.encodeURIComponent(name)
      Ajax.get("project/exists/" + encodedName).onComplete {
        case Failure(error) => errorAlert(error)
        case Success(xhr) => {
          val existsM = decode[Boolean](xhr.responseText)

          existsM match {
            case Left(error) => errorAlert(error)
            case Right(exists) => {
              if (exists) {
                showError("A project with that name already exists! Try again with a different name.")
              } else {

                Ajax.post(form.action, name.asJson.toString()).onComplete {
                  case Failure(error) => errorAlert(error)
                  case Success(xhr2) =>
                    form.reset()
                    val new_project = decode[TODOProject](xhr2.responseText)

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
