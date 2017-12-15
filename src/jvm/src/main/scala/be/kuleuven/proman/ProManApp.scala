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
          div(id := "info-container", cls := "alert alert-info", style := "display: none;", marginTop := 20)(
            "info message"
          ),
          div(id := "error-container", cls := "alert alert-danger", style := "display: none;", marginTop := 20)(
            "error message"
          ),
          div(id := "content")
        )
      )
    ).render
  }.withType(MediaType.`text/html`)

  // Service that matches requests (at routes) to responses (by controller actions).
  val service: HttpService = HttpService {
    case GET      -> Root                                     => TemporaryRedirect(uri("/projects"))
    case GET      -> Root/"projects"                          => HTML

    case GET      -> Root/"projects"/"json"                   => TodoProjectsController.index
    case r @ POST -> Root/"projects"/"store"                  => TodoProjectsController.store(r)
    case GET      -> Root/"project"/id                        => TodoProjectsController.get(id.toLong)
    case r @ PUT  -> Root/"project"/id                        => TodoProjectsController.update(r, id.toLong)
    case GET      -> Root/"project"/"exists"/name             => TodoProjectsController.exists(name)
    case GET      -> Root/"projects"/"sync"/state             => TodoProjectsController.synchronise(state.toLong)

    case r @ POST -> Root/"lists"/"store"                     => TodoListsController.store(r)
    case r @ PUT  -> Root/"list"/id                           => TodoListsController.update(r, id.toLong)
    case GET      -> Root/"lists"/"sync"/state/project_id     => TodoListsController.synchronise(state.toLong, project_id.toLong)

    case GET      -> Root/"todos"/project_id/"json"           => TodoEntriesController.index(project_id.toLong)
    case r @ POST -> Root/"todos"/"store"                     => TodoEntriesController.store(r)
    case r @ PUT  -> Root/"todo"/id                           => TodoEntriesController.update(r, id.toLong)
    case GET      -> Root/"todos"/"sync"/state/project_id/v   => TodoEntriesController.synchronise(state.toLong, project_id.toLong, v.toInt)

    // Serve some files with specific extensions
    case r @ GET  -> path~ext if fileExtensions.contains(ext) => static(path.toList.mkString("/") + "." + ext, r)
    // Catch all
    case _        -> path                                     => NotFound("Not found!")
  }

  // Serve a file
  def static(filename: String, request: Request): Task[Response] = {
    StaticFile.fromFile(new File("./" +  filename), Some(request)).getOrElseF(NotFound())
    //StaticFile.fromResource("/" + filename, Some(request)).getOrElseF(NotFound())
  }

  // Set up server that binds to localhost:8080/
  val server = BlazeBuilder.bindHttp(8080, "localhost").mountService(service, "/").run

  // Create some dummy projects, lists, and todos.
  List("SCALA PROJECT", "GAE", "THESIS").foreach(TodoProjectRepository.create)

  List(
    (1L, "Base requirements"), (1L, "Extension 1"), (1L, "Extension 2"),
    (2L, "Exercise 1"), (2L, "Exercise 2"), (2L, "Exercise 3"),
    (3L, "Thesis text"), (3L, "Isolation forest experiments"), (3L, "Literature to read")
  ).foreach {
    case (p_id: Long, name: String) => Thread.sleep(15); TodoListRepository.create(p_id, name)
  }

  List(
    (1L, "startscene", true), (1L, "projectscene", true), (1L, "multi-user1", true), (1L, "everything", true),
    (2L, "a", true), (2L, "b", true), (2L, "c", true), (3L, "d", true), (3L, "e", true), (3L, "f", true), (3L, "g", true),
    (4L, "read assignment", true), (4L, "do experiments"), (5L, "start"), (5L, "work"), (5L, "finish"),
    (6L, "i"), (6L, "have"), (6L, "no"), (6L, "idea"),
    (7L, "intro"), (7L, "background"), (7L, "methods"), (7L, "results"),
    (8L, "More trees", true), (8L, "Higher sampling"), (8L, "better features"), (8L, "Plot anomalies on PCA"),
    (9L, "improved dtw"), (9L, "tensor decomposition"), (9L, "time series discord"), (9L, "series feature extraction")
  ).foreach {
    case (l_id: Long, text: String) => Thread.sleep(15); TodoEntryRepository.create(l_id, text)
    case (l_id: Long, text: String, is_done: Boolean) => Thread.sleep(15); TodoEntryRepository.create(l_id, text, is_done)
  }

  println("SERVER NOW RUNNING")

  // Block on reading a line, makes it easy to stop in your terminal (hit enter).
  StdIn.readLine()
  server.shutdownNow()
}
