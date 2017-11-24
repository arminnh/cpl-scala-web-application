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
        title("TODO Projects"),
        link(href := "/jvm/src/assets/img/favicon.ico", rel := "icon"),
        link(rel :="stylesheet", href := "/jvm/src/assets/css/bootstrap.css"),
        script(tpe := "text/javascript", src := "/js/target/scala-2.11/js-fastopt.js", attr("defer").empty)

      ),
      body(
        div(cls := "container", role := "main")(
          h1(cls := "jumbotron")("TODO Projects"),
          div(cls := "alert alert-default"),
          div(cls := "alert alert-warning"),
          div(cls := "alert alert-warning", hidden),
          h2("Create a new project"),
          form(action := "/projects/store", method := "post", id := "form-create-project")(
            input(tpe := "text", name := "name", placeholder := "Create a new project"),
            button(tpe := "submit", cls := "btn")("Create")
          ),
          h2("Open a project"),
          ui.multipleTemplate(TODOProjectRepository.all())
        )
      )
    ).render
  }.withType(MediaType.`text/html`)


  def get(id: Int): Task[Response] = {
    Ok(TODOProjectRepository.find(id).orNull.asJson)
//    val projectM = TODOProjectRepository.find(id)
//    projectM match {
//      case Some(project) => Ok(project.asJson)
//      case None => NotFound("Project not found!")
//    }
  }

  def store(request: Request): Task[Response] = {
    println("Project store")
    request.params.mapValues(println)
    println(request.body.toString())
    println(request.attributes.entries)

//    val name = request.asJson.hcursor.downField("name").as[String]
//    println("creating new project with name " + name)
//    TODOProjectRepository.create(name.right.get)
    Ok()
  }
//    for {
//      project <- request.as(jsonOf[TODOProject])
//      json <- request.asJson.hcursor.downField("name").as[String]
//      response <- Ok(project.name)
//    } yield {
//      TODOProjectRepository.create(project.name)
//      response
//    }


  def getProjectsJSON: Task[Response] = Ok {
    TODOProjectRepository.all().asJson
  }
}
