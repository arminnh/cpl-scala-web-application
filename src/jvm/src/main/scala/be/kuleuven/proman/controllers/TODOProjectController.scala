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


object TODOProjectController {

  val ui = new TODOProjectTemplate(scalatags.Text)

  def index: Task[Response] = Ok {
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
        link(rel :="stylesheet", href := "/jvm/src/assets/css/bootstrap.css"),
        script(tpe := "text/javascript", src := "/js/target/scala-2.11/js-fastopt.js", attr("defer").empty)

      ),
      body(
        div(cls := "container", role := "main")(
          h1(id := "title", cls := "jumbotron")("Todo projects"),
          div(id := "info-container", cls := "alert alert-info", style := "display: none;")("info message"),
          div(id := "error-container", cls := "alert alert-danger", style := "display: none;")("error message"),
          div(id := "content")(
            h2("Create a new project"),
            form(id := "form-create-project", action := "/projects/store", method := "post", cls := "form-inline")(
              div(cls := "form-group")(input(tpe := "text", name := "name", placeholder := "Project title", cls := "form-control")),
              button(tpe := "submit", cls := "btn", marginLeft := 15)("Create")
            ),
            h2("Open  a project"),
            ui.multipleTemplate(TODOProjectRepository.all())
          )
        )
      )
    ).render
  }.withType(MediaType.`text/html`)


  def get(id: Int): Task[Response] = {
    val project = TODOProjectRepository.find(id)
    if (project != null) {
      Ok(project.asJson)
    } else {
      NotFound()
    }
  }

  def store(request: Request): Task[Response] =
    for {
      name <- request.as[String]
      response <- Ok(TODOProjectRepository.create(name).asJson)
    } yield {
      response
    }

  def exists(name: String): Task[Response] = {
    println("Project exists?: " + name)
    Ok(TODOProjectRepository.exists(name).asJson)
  }

  def getProjectsJSON: Task[Response] = Ok(TODOProjectRepository.all().asJson)
}
