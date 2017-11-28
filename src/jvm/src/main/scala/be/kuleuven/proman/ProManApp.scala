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


object ProManApp extends App {
  // Extensions for files that can be served through the browser.
  val fileExtensions: List[String] = List("js", "css", "png", "map", "ico", "eot", "svg", "ttf", "woff", "woff2")

  // Service that matches requests (at routes) to responses (by controller actions).
  val service: HttpService = HttpService {
    case GET      -> Root                                      => TemporaryRedirect(uri("/projects"))

    case GET      -> Root/"projects"                           => TODOProjectController.index
    case r @ POST -> Root/"projects"/"store"                   => TODOProjectController.store(r)
    case GET      -> Root/"projects"/"json"                    => TODOProjectController.getProjectsJSON
    case GET      -> Root/"project"/IntVar(id)                 => TODOProjectController.get(id)
    case GET      -> Root/"project"/"exists"/name              => TODOProjectController.exists(name)

    case GET      -> Root/"project"/IntVar(project_id)/"todos" => TODOEntryController.index(project_id)
    case r @ POST -> Root/"project"/IntVar(project_id)/"store" => TODOEntryController.store(r, project_id)
    case r @ POST -> Root/"todo"/IntVar(todo_id)/"update"      => TODOEntryController.update(r, todo_id)

    // Serve some files with specific extensions
    case r @ GET  -> path~ext if fileExtensions.contains(ext)  => static(path.toList.mkString("/") + "." + ext, r)
    // Catch all
    case _        -> path                                      => NotFound("Not found!")
  }

  // Serve a file
  def static(filename: String, request: Request): Task[Response] = {
    StaticFile.fromFile(new File("./" +  filename), Some(request)).getOrElseF(NotFound())
    //StaticFile.fromResource("/" + filename, Some(request)).getOrElseF(NotFound())
  }

  // Set up server that binds to localhost:8080/
  val server = BlazeBuilder.bindHttp(8080, "localhost").mountService(service, "/").run
  TODOProjectRepository.create("SCALA PROJECT")
  TODOProjectRepository.create("GAE")
  TODOProjectRepository.create("THESIS")
  TODOEntryRepository.create(1, "Some stuff", is_done = true)
  TODOEntryRepository.create(1, "Some more stuff")
  TODOEntryRepository.create(1, "Even more stuff")
  TODOEntryRepository.create(1, "wtf")
  TODOEntryRepository.create(2, "read assignment", is_done = true)
  TODOEntryRepository.create(2, "start")
  TODOEntryRepository.create(2, "do stuff")
  TODOEntryRepository.create(2, "finish")
  TODOEntryRepository.create(3, "Isolation Forest", is_done = true)
  TODOEntryRepository.create(3, "Temporal feature extraction")
  TODOEntryRepository.create(3, "Plot anomalies on PCA")
  println("SERVER NOW RUNNING")

  // Block on reading a line, makes it easy to stop in your terminal (hit enter).
  StdIn.readLine()
  server.shutdownNow()
}
