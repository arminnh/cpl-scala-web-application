package be.kuleuven.proman

import be.kuleuven.proman.controllers._
import be.kuleuven.proman.repositories._
import java.io.File
import scala.io.StdIn
import fs2.Task
import fs2.interop.cats._
import org.http4s._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder

object ProManApp extends App {

  // Service that matches requests (at routes) to responses (by controller actions).
  val service: HttpService = HttpService {
    case GET      -> Root                                     => TemporaryRedirect(uri("/projects"))

    case GET      -> Root/"projects"                          => TodoProjectsController.index
    case r @ POST -> Root/"projects"/"store"                  => TodoProjectsController.store(r)
    case GET      -> Root/"project"/id                        => TodoProjectsController.get(id.toLong)
    case r @ PUT  -> Root/"project"/id                        => TodoProjectsController.update(r, id.toLong)
    case GET      -> Root/"project"/"exists"/name             => TodoProjectsController.exists(name)

    case r @ POST -> Root/"lists"/"store"                     => TodoListsController.store(r)
    case r @ PUT  -> Root/"list"/id                           => TodoListsController.update(r, id.toLong)

    case r @ POST -> Root/"todos"/"store"                     => TodoEntriesController.store(r)
    case r @ PUT  -> Root/"todo"/id                           => TodoEntriesController.update(r, id.toLong)

    case GET      -> Root/"sync"/"projects"/timestamp         => SynchronisationController.projects(timestamp.toLong)
    case GET      -> Root/"sync"/"todos"/project_id/timestamp => SynchronisationController.projectWithListsAndTodos(project_id.toLong, timestamp.toLong)

    // Serve the Scala.js code
    case r @ GET  -> Root/"js"                                => staticFile("/js/target/scala-2.11/js-fastopt.js", r)
    // Serve static files in the public directory
    case r @ GET  -> path if path.startsWith(Path("public"))  => staticFile(path.toString, r)

    // Catch all
    case _        -> _                                        => NotFound("Not found!")
  }

  // Serve a file
  def staticFile(filename: String, request: Request): Task[Response] =
    StaticFile.fromFile(new File("./" + filename), Some(request)).getOrElseF(NotFound("File not found!"))

  // Set up server that binds to localhost:8080/
  val server = BlazeBuilder.bindHttp(8080, "localhost").mountService(service, "/").run

  // Create some dummy projects, lists, and todos.
  List("SCALA PROJECT", "GAE", "THESIS").foreach(TodoProjectRepository.create)

  List(
    (1L, "Base requirements"), (1L, "Extension 1"), (1L, "Extension 2"),
    (2L, "Exercise 1"), (2L, "Exercise 2"), (2L, "Exercise 3"),
    (3L, "Thesis text"), (3L, "Isolation forest experiments"), (3L, "Literature to read")
  ).foreach {
    case (p_id: Long, name: String) => TodoListRepository.create(p_id, name)
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
    case (l_id: Long, text: String) => TodoEntryRepository.create(l_id, text)
    case (l_id: Long, text: String, is_done: Boolean) => TodoEntryRepository.create(l_id, text, is_done)
  }

  println("SERVER NOW RUNNING")

  // Block on reading a line, makes it easy to stop in your terminal (hit enter).
  StdIn.readLine()
  server.shutdownNow()
}
