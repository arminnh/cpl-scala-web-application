package be.kuleuven.proman.controllers
import be.kuleuven.proman.models._

import java.io.File

import fs2.Task
import fs2.interop.cats._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import scalatags.Text.all._
import scalatags.Text.tags2.title
import io.circe.syntax._


object TODOProjectController {
  def helloString(str: String): Task[Response] = Ok(s"Hello, $str")

  def index(projects: Seq[TODOProject]): Task[Response] = Ok {
    html(
      head(
        title("TODO Projects"),
        link(href := "./jvm/src/assets/img/checked-box.png", rel := "icon")
      ),
      body(
        h1("TODO Projects"),
        ul( projects.map(p => li(p.name)) )
      ),
      script("console.log('hi')")
    ).render
  }.withType(MediaType.`text/html`)

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
