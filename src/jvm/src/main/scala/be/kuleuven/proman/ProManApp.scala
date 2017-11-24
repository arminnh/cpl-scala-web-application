package be.kuleuven.proman

import java.io.File

import be.kuleuven.proman.controllers._
import fs2.Task
import fs2.interop.cats._

import scala.io.StdIn
import org.http4s._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder


object ProManApp extends App {
  // Extensions for files that can be served through the browser.
  val fileExtensions: List[String] = List("js", "css", "png", "map", "ico")

  // Service that matches requests (at routes) to responses (by controller actions).
  val service: HttpService = HttpService {
    case GET            -> Root                    => TemporaryRedirect(uri("/projects"))

    case GET            -> Root/"projects"         => TODOProjectController.index()
    case request @ POST -> Root/"projects"/"store" => TODOProjectController.store(request)
    case GET            -> Root/"projects"/"json"  => TODOProjectController.getProjectsJSON

    // Serve some files with specific extensions
    case request @ GET  -> path ~ extension if fileExtensions.contains(extension) =>
        static(path.toList.mkString("/") + "." + extension, request)

    case _ -> path                                 => NotFound("Not found!")
  }

  // Serve a file
  def static(filename: String, request: Request): Task[Response] = {
    StaticFile.fromFile(new File("./" +  filename), Some(request)).getOrElseF(NotFound())
    //StaticFile.fromResource("/" + filename, Some(request)).getOrElseF(NotFound())
  }

  // Set up server that binds to localhost:8080/
  val server = BlazeBuilder.bindHttp(8080, "localhost").mountService(service, "/").run
  println("SERVER NOW RUNNING")

  // Block on reading a line, makes it easy to stop in your terminal (hit enter).
  StdIn.readLine()
  server.shutdownNow()
}
