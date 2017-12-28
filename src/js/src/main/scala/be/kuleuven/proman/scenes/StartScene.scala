package be.kuleuven.proman.scenes

import be.kuleuven.proman._
import be.kuleuven.proman.models._

import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.syntax._, io.circe.parser._
import cats.syntax.either._
import scala.scalajs.js.Any
import scala.scalajs.js.timers.SetIntervalHandle
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html._ // HTMLDivElement => Div
import org.scalajs.dom.raw.{Event, NodeListOf}
import scalatags.JsDom.all._ // Client side HTML Tags


object StartScene {
  var synchronisation_timestamp: Long = _
  var synchronisation_interval: SetIntervalHandle = _
  val todo_project_ui = new TodoProjectTemplate(scalatags.JsDom)

  /**
    * Sets up everything necessary for the current scene.
    * @param firstSetup: Denotes whether or not this is the first time this scene is loaded for the current client.
    */
  def setupScene(firstSetup: Boolean = false): Unit = {
    this.synchronisation_timestamp = 0
    this.setupHTML(firstSetup)
    this.synchronise()
    this.synchronisation_interval = scala.scalajs.js.timers.setInterval(500) { synchronise() }
  }

  /**
    * Sets up the HTML for the scene along with event handlers.
    * @param firstSetup: Denotes whether or not this is the first time this scene is loaded for the current client.
    */
  def setupHTML(firstSetup: Boolean = false): Unit = {
    hideError()

    // The first time the page is loaded, do not need to set HTML, it will already be rendered on the server.
    if (!firstSetup) {
      dom.document.title = "Todo Projects"
      dom.document.getElementById("top-title").innerHTML = div(
        h1(fontSize := 36)("Todo Projects")
      ).render.innerHTML

      dom.document.getElementById("content").innerHTML = div(
        div(cls := "row")(
          div(cls := "col-sm-6")(
            h3("Create a new project"),
            form(id := "form-create-project", name := "form-create-project", action := "/projects/store", method := "post", cls := "form-inline")(
              div(cls := "form-group")(
                input(tpe := "text", name := "name", placeholder := "Project title",
                  cls := "form-control", autocomplete := "off", marginRight := 15)
              ),
              button(tpe := "submit", cls := "btn btn-primary")("Create")
            )
          ),
          div(cls := "col-sm-6")(
            h3("Search"),
            div(cls := "form-group")(
              input(tpe := "text", id := "project-filter", placeholder := "Search by name", cls := "form-control")
            )
          )
        ),
        h3("Open a project"),
        div(id := "project-container")(
          div(cls := "table-responsive")(table(cls := "table table-condensed table-striped table-hover")(tbody))
        )
      ).render.innerHTML
    } else {
      // If the page already contains a table of projects, we still need to set the event handlers.
      // Normally, this is done in 'updateProjects' when new projects are added.

      val btns = dom.document.querySelectorAll(".project-anchor").asInstanceOf[NodeListOf[Button]]
      for (i <- 0 until btns.length) {
        val btn = btns.item(i)
        btn.onclick = Any.fromFunction1(_ => getProjectAndShow(btn.getAttribute("data-id").toInt))
      }
    }

    // Set event handlers
    dom.document.getElementById("form-create-project").asInstanceOf[Form].onsubmit = Any.fromFunction1((e: Event) => {
      e.preventDefault()
      this.submitNewProject(getFormFromEvent(e))
    })

    val project_filter = dom.document.getElementById("project-filter").asInstanceOf[Input]
    project_filter.onkeyup = Any.fromFunction1((e: Event) => {
      this.filterTodoProjects(project_filter.value)
    })
  }

  /**
    * Submits a new project and the loads the ProjectScene for that project if is has been created successfully.
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
        case Failure(error) => printError(error)
        case Success(xhr) => {
          decode[Boolean](xhr.responseText) match {
            case Left(error) => printError(error)
            case Right(exists) => {
              if (exists) {
                showError("A project with that name already exists! Try again with a different name.")
              } else {
                Ajax.post(form.action, new TodoProject(name).asJson.noSpaces).onComplete {
                  case Failure(error) => printError(error)
                  case Success(xhr2) =>
                    form.reset()
                    decode[TodoProject](xhr2.responseText) match {
                      case Left(error) => printError(error)
                      case Right(new_project) =>
                        scala.scalajs.js.timers.clearInterval(this.synchronisation_interval)
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
      case Failure(error) => printError(error)
      case Success(xhr) =>
        decode[TodoProject](xhr.responseText) match {
          case Left(error) => printError(error)
          case Right(project) => {
            scala.scalajs.js.timers.clearInterval(this.synchronisation_interval)
            ProjectScene.setupScene(project)
          }
        }
    }
  }

  /**
    * Synchronisation policy: Send the local sync timestamp to the synchronisation route. The JSON response
    * of the server will contain a new timestamp for future sync requests along with a list of projects that have been
    * created/updated since the last synchronisation.
    */
  def synchronise(): Unit = {
    Ajax.get("sync/projects/" + this.synchronisation_timestamp).onComplete {
      case Failure(error) => printError(error)
      case Success(xhr) =>
        println("synchronising for timestamp: " + formatTimeStamp(this.synchronisation_timestamp) + ", response: " + xhr.responseText)
        parse(xhr.responseText) match {
          case Left(error) => printError(error)
          case Right(json) =>
            val projects: Seq[TodoProject] = json.hcursor.downField("projects").as[Seq[TodoProject]].getOrElse(List())
            this.updateProjects(projects)

            this.synchronisation_timestamp = json.hcursor.downField("timestamp").as[Long].getOrElse(this.synchronisation_timestamp)
        }
    }
  }

  /**
    * Processes a list of new or updated TodoProjects. New projects are simply added at the top of the projects table.
    * Updated projects are ignored for now, but could be used later on if e.g. project names can be updated, project
    * descriptions are displayed on hover on this page, etc.
    * @param projects: A list of projects to insert in the table.
    */
  def updateProjects(projects: Seq[TodoProject]): Unit = {
    val tbody = dom.document.getElementById("project-container").querySelector("tbody").asInstanceOf[TableSection]
    val tempRow = tbody.insertRow(0)

    projects.foreach(project => {
      // If project not on page yet, insert in table and set onclick event handler.
      if (dom.document.querySelector(s"button[data-id='${project.id}']") == null) {
        val row = tbody.insertBefore(todo_project_ui.singleTemplate(project).render, tempRow).asInstanceOf[TableRow]

        val btn = row.querySelector(".project-anchor").asInstanceOf[Button]
        btn.onclick = Any.fromFunction1(_ => getProjectAndShow(btn.getAttribute("data-id").toInt))
      }
    })

    tbody.removeChild(tempRow)
  }

  /**
    * Filters out the TodoProjects in the view based on a given string. All TodoProjects that do not contain the given
    * string in their title are hidden in the view.
    * @param filter_text: The given text to filter the projects on.
    */
  def filterTodoProjects(filter_text: String): Unit = {
    val btns_project = dom.document.querySelectorAll("button[data-id]").asInstanceOf[NodeListOf[TableRow]]
    for (i <- 0 until btns_project.length) {
      val btn = btns_project.item(i)
      val project_name: String = btn.innerHTML
      if (project_name.toLowerCase().contains(filter_text.toLowerCase())) {
        btn.parentElement.parentElement.style.display = "table-row"
      } else {
        btn.parentElement.parentElement.style.display = "none"
      }
    }
  }
}
