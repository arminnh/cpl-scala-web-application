// package be.kuleuven.proman

// import collection.mutable.Seq
import scala.io.StdIn

import java.io.File
import java.time.LocalDateTime

import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder
import fs2.Task
import fs2.interop.cats._
import scalatags.Text._
import scalatags.Text.all._
import scalatags.Text.tags2.title
import io.circe.generic.auto._
import io.circe.syntax._


object ProManApp extends App {
  val test_todo = new TODOEntry("Finish project", LocalDateTime.now())
  val projects: Seq[TODOProject] = List(
      new TODOProject("SCALA PROJECT", List(test_todo))
  )

  print(test_todo)
  projects.foreach(p => print(p.toString))

  val service: HttpService = HttpService {
    case GET -> Root / "hello" => homepage

    case GET -> Root / "js" => javascriptProgramResponse

    case GET -> Root / name => Ok(s"Hello, $name")
  }

  lazy val homepage: Task[Response] = Ok {
    html(
      head(
        title("ANIMALS!!!"),
        link(href := "https://image.flaticon.com/teams/slug/freepik.jpg", rel := "icon")
      ),
      body(
        h1("ANIMALS!!!!"),
        ul(
            li("aaaaa"),
            li("aaaaa")
        )
        //div(
        //  h1("Breed, Name")
        //)
      ),
      script("console.log('hi')")
    ).render
  }.withType(MediaType.`text/html`)

  lazy val projectsJSON: Task[Response] = Ok {
    // projects.asJson
    List(1, 2, 3).asJson
  }

  // compilation result of the Scala.js code.
  lazy val javascriptProgramResponse: Task[Response] =
    StaticFile
      .fromFile(new File("./js/target/scala-2.11/js-fastopt.js"))
      .getOrElseF(NotFound())

  // Standard server that binds to localhost:8080/
  val server = BlazeBuilder.bindHttp(8080, "localhost").mountService(service, "/").run

  // Block on reading a line, makes it easy to stop in your terminal (hit enter).
  StdIn.readLine()
  server.shutdownNow()
}
