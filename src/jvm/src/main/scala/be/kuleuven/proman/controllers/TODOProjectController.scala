package be.kuleuven.proman.controllers

import be.kuleuven.proman.models._

import java.io.File

import fs2.Task
import fs2.interop.cats._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import scalatags.Text.all._
import scalatags.Text.tags2.title


object TODOProjectController {

  val ui = new TODOProjectTemplate(scalatags.Text)

  def index(projects: Seq[TODOProject]): Task[Response] = Ok {
    html(
      head(
        meta(charset := "utf-8"),
        meta(httpEquiv := "X-UA-Compatible", content := "IE=Edge"),
        meta(name := "viewport", content := "width=device-width, initial-scale=1"),
        meta(name := "description", content := "Basic TODO web application"),
        meta(name := "author", content := "Armin Halilovic"),
        title("TODO Projects"),
        link(href := "./jvm/src/assets/img/checked-box.png", rel := "icon"),
        link(rel :="stylesheet", href := "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"),
        script(tpe := "text/javascript", src := "./js/target/scala-2.11/js-fastopt.js", attr("defer").empty)
      ),
      body(
        div(cls := "container", role := "main")(
          h1(cls := "jumbotron")("TODO Projects"),
          form(action := "/projects/store", method := "post")(
            input(tpe := "text", name := "name", placeholder := "Create a new project"),
            button(tpe := "submit")("Create")
          ),
          ui.multipleTemplate(projects)
        )
      )
    ).render
  }.withType(MediaType.`text/html`)

  def store(projects: Seq[TODOProject], request: Request): Task[Response] =
    for {
      project <- request.as(jsonOf[TODOProject])
      response <- Ok(project.name.asJson)
    } yield {
//      projects = project +: projects
      val projectss = project +: projects
      println(projectss)
      response
    }

  def getProjectsJSON(projects: Seq[TODOProject]): Task[Response] = Ok {
    // projects.asJson
    List(1, 2, 3).asJson
  }

  // compilation result of the Scala.js code.
  lazy val javascriptProgramResponse: Task[Response] =
    StaticFile
      .fromFile(new File("./js/target/scala-2.11/js-fastopt.js"))
      .getOrElseF(NotFound())

}
