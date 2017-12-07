package be.kuleuven.proman.scenes

import be.kuleuven.proman.{errorAlert, formatTimeStamp, hideError, showError}
import be.kuleuven.proman.models._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global // implicit ExecutionContext for Future tasks
import io.circe.syntax._, io.circe.parser._, io.circe.Json
import cats.syntax.either._
//import io.circe.generic.auto._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html._ // HTMLDivElement => Div
import org.scalajs.dom.raw.{Event, NodeListOf}
import scalatags.JsDom.all._ // Client side HTML Tags
import scala.scalajs.js.Any

//noinspection AccessorLikeMethodIsUnit
object StartScene {
  var state: Long = -999
  var synchronisation_interval: Int = -999
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
        div(cls := "form-group")(
          input(tpe := "text", name := "name", placeholder := "Project title", cls := "form-control", autocomplete := "off")
        ),
        button(tpe := "submit", cls := "btn btn-primary", marginLeft := 15)("Create")
      ),
      h2("Open  a project"),
      div(id := "project-container")(
        div(cls := "table-responsive")(table(cls := "table table-condensed table-striped table-hover")(tbody))
      )
    ).render.innerHTML

    dom.document.getElementById("form-create-project").asInstanceOf[Form].onsubmit = Any.fromFunction1((e: Event) => {
      e.preventDefault()
      submitNewProject(e.srcElement.asInstanceOf[Form])
    })
  }

  def setupScene(): Unit = {
    setupHTML()
    this.state = -999
    synchronise()
    this.synchronisation_interval = dom.window.setInterval(Any.fromFunction0(() => synchronise()), 2500)
  }

  /**
    * Submits a new project and the loads the ProjectScene for that project if is has been successfully created.
    * @param form: The form to be submitted.
    */
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

                //Ajax.post(form.action, name.asJson.toString()).onComplete {
                Ajax.post(form.action, new TODOProject(name).asJson.noSpaces).onComplete {
                  case Failure(error) => errorAlert(error)
                  case Success(xhr2) =>
                    form.reset()
                    val new_projectM = decode[TODOProject](xhr2.responseText)

                    new_projectM match {
                      case Left(error) => errorAlert(error)
                      case Right(new_project) =>
                        dom.window.clearInterval(this.synchronisation_interval)
                        ProjectScene.setupScene(new_project)
                    }
                }
              }
            }
          }
        }
      }
    }
  }

  /**
    * Ajax gets a project and then loads its ProjectScene.
    * @param id: The id of the project to get.
    */
  def getProjectAndShow(id: Int): Unit = {
    Ajax.get("/project/" + id).onComplete {
      case Failure(error) => errorAlert(error)
      case Success(xhr) =>
        val projectM = decode[TODOProject](xhr.responseText)

        projectM match {
          case Left(error) => errorAlert(error)
          case Right(project) => {
            // TODO" check what this does again
            dom.window.history.pushState("", dom.document.title, dom.window.location.pathname)
            dom.window.clearInterval(this.synchronisation_interval)
            ProjectScene.setupScene(project)
          }
        }
    }
  }

  /**
    * Inserts a list of projects into the project table.
    * @param projects: A list of projects to insert in the table.
    */
  def createProjectsInTable(projects: Seq[TODOProject]): Unit = {
    val tbody = dom.document.getElementById("project-container").getElementsByTagName("tbody").item(0).asInstanceOf[TableSection]
    val tempRow = tbody.insertRow(0)

    // Insert projects in table and set onclick event handler.
    projects.foreach(project => {
      val row = tbody.insertBefore(todo_project_ui.singleTemplate(project).render, tempRow).asInstanceOf[TableRow]

      val anchor = row.getElementsByClassName("project-anchor").item(0).asInstanceOf[Anchor]
      anchor.onclick = Any.fromFunction1(_ => getProjectAndShow(anchor.getAttribute("data-id").toInt))
    })

    tbody.removeChild(tempRow)
  }

  /**
    * Synchronisation policy: Send the local state variable to the synchronisation route. The JSON response
    * of the server will contain the latest state variable and a list of projects that have been created
    * since the last synchronisation. The projects are then simply added at the top of the table. The returned
    * list of projects can only be new projects since that's all that can be done with the current requirements.
    */
  def synchronise(): Unit = {
    println("synchronising for state: " + this.state)

    Ajax.get("projects/sync/" + this.state).onComplete {
      case Failure(error) => errorAlert(error)
      case Success(xhr) =>
        parse(xhr.responseText) match {
          case Left(error) => errorAlert(error)
          case Right(json) =>
            val projects: Seq[TODOProject] = json.hcursor.downField("projects").as[Seq[TODOProject]].getOrElse(List())
            createProjectsInTable(projects)

            this.state = json.hcursor.downField("state").as[Long].getOrElse(this.state)
        }
    }
  }

}
