package be.kuleuven.proman.controllers

import be.kuleuven.proman.models._
import be.kuleuven.proman.repositories._

import fs2.Task
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._

import scalatags.Text.all._
import scalatags.Text.tags2.title


object TodoProjectsController {

  // Returns the base html of the project filled in with the first page(the ProjectScene).
  def index: Task[Response] = {
    val todo_project_ui = new TodoProjectTemplate(scalatags.Text)
    Ok {
      html(
        head(
          meta(charset := "utf-8"),
          meta(httpEquiv := "X-UA-Compatible", content := "IE=Edge"),
          meta(httpEquiv := "Cache-Control", content := "no-cache, no-store, must-revalidate"),
          meta(httpEquiv := "Pragma", content := "no-cache"),
          meta(httpEquiv := "Expires", content := "0"),
          meta(name := "viewport", content := "width=device-width, initial-scale=1"),
          meta(name := "description", content := "Basic TODO web application"),
          meta(name := "author", content := "Armin Halilovic"),
          title("Todo Projects"),
          link(href := "/jvm/src/assets/img/favicon.ico", rel := "icon"),
          link(rel := "stylesheet", href := "/jvm/src/assets/css/bootstrap.css"),
          script(tpe := "text/javascript", src := "/js/target/scala-2.11/js-fastopt.js", attr("defer").empty)
        ),
        body(
          div(cls := "jumbotron")(div(id := "top-title", cls := "container")(h1(fontSize := 36)("Todo Projects"))),
          div(cls := "container", role := "main")(
            div(id := "error-container", cls := "alert alert-danger", style := "display: none;", marginTop := 20)(
              "error message"
            ),
            div(id := "content")(
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
                div(cls := "table-responsive")(table(cls := "table table-condensed table-striped table-hover")(tbody(
                  TodoProjectRepository.all().map(project => todo_project_ui.singleTemplate(project))
                )))
              )
            )
          )
        )
      ).render
    }.withType(MediaType.`text/html`)
  }

  def get(id: Long): Task[Response] = {
    val project = TodoProjectRepository.find(id)
    if (project != null) {
      Ok(project.asJson)
    } else {
      NotFound()
    }
  }

  def exists(name: String): Task[Response] =
    Ok(TodoProjectRepository.exists(name).asJson)

  def store(request: Request): Task[Response] =
    for {
      project <- request.as(jsonOf[TodoProject])
      response <- Ok(TodoProjectRepository.create(project.name).asJson)
    } yield {
      response
    }

  def update(request: Request, id: Long): Task[Response] =
    for {
      project <- request.as(jsonOf[TodoProject])
      response <- Ok(TodoProjectRepository.update(id, project).asJson)
    } yield {
      response
    }
}
