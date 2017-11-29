package be.kuleuven.proman

import java.io.File

import be.kuleuven.proman.controllers._
import be.kuleuven.proman.repositories._
import fs2.Task
import fs2.interop.cats._

import scala.io.StdIn
import org.http4s._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder

import scalatags.Text.all._
import scalatags.Text.tags2.title

object ProManApp extends App {
  // Extensions for files that can be served through the browser.
  val fileExtensions: List[String] = List("js", "css", "png", "map", "ico", "eot", "svg", "ttf", "woff", "woff2")

  // Base html of the project.
  val HTML = Ok {
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
        div(cls := "jumbotron")(div(id := "top-title", cls := "container")),
        div(cls := "container", role := "main")(
          div(id := "info-container", cls := "alert alert-info", style := "display: none;")("info message"),
          div(id := "error-container", cls := "alert alert-danger", style := "display: none;")("error message"),
          div(id := "content")
        )
      )
    ).render
  }.withType(MediaType.`text/html`)

  // Service that matches requests (at routes) to responses (by controller actions).
  val service: HttpService = HttpService {
    case GET      -> Root                                       => TemporaryRedirect(uri("/projects"))
    case GET      -> Root/"projects"                            => HTML

    case GET      -> Root/"projects"/"json"                     => TODOProjectController.index
    case r @ POST -> Root/"projects"/"store"                    => TODOProjectController.store(r)
    case GET      -> Root/"project"/IntVar(id)                  => TODOProjectController.get(id)
    case GET      -> Root/"project"/"exists"/name               => TODOProjectController.exists(name)

    case GET      -> Root/"project"/IntVar(p_id)/"todos"/"json" => TODOEntryController.index(p_id)
    case r @ POST -> Root/"project"/IntVar(p_id)/"store"        => TODOEntryController.store(r, p_id)
    case r @ POST -> Root/"todo"/IntVar(todo_id)/"update"       => TODOEntryController.update(r, todo_id)

    // Serve some files with specific extensions
    case r @ GET  -> path~ext if fileExtensions.contains(ext)   => static(path.toList.mkString("/") + "." + ext, r)
    // Catch all
    case _        -> path                                       => NotFound("Not found!")
  }

  // Serve a file
  def static(filename: String, request: Request): Task[Response] = {
    StaticFile.fromFile(new File("./" +  filename), Some(request)).getOrElseF(NotFound())
    //StaticFile.fromResource("/" + filename, Some(request)).getOrElseF(NotFound())
  }

  // Set up server that binds to localhost:8080/
  val server = BlazeBuilder.bindHttp(8080, "localhost").mountService(service, "/").run

  // Create some dummy projects and todos
  List("SCALA PROJECT", "GAE", "THESIS").map(TODOProjectRepository.create)
  List(
    (1, "Some stuff", true), (1, "Some more stuff"), (1, "Even more stuff"), (1, "wtf"),
    (2, "read assignment", true), (2, "start"), (2, "do stuff"), (2, "finish"),
    (3, "Isolation Forest", true), (3, "Temporal feature extraction"), (3, "Plot anomalies on PCA")
  ).map{
    case (p_id: Int, name: String) => TODOEntryRepository.create(p_id, name)
    case (p_id: Int, name: String, is_done: Boolean) => TODOEntryRepository.create(p_id, name, is_done)
  }

  println("SERVER NOW RUNNING")

  // Block on reading a line, makes it easy to stop in your terminal (hit enter).
  StdIn.readLine()
  server.shutdownNow()
}
