package be.kuleuven.proman
import be.kuleuven.proman.controllers._
import be.kuleuven.proman.models._

// import collection.mutable.Seq
import scala.io.StdIn
import java.time.LocalDateTime

import org.http4s._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder


object ProManApp extends App {

  val test_todo = new TODOEntry("Finish project", LocalDateTime.now())
  val projects: Seq[TODOProject] = List(
      new TODOProject("SCALA PROJECT", List(test_todo))
  )

  // Define routes.
  val service: HttpService = HttpService {
    case GET -> Root => TODOProjectController.index(projects)

    case GET -> Root / "js" => TODOProjectController.javascriptProgramResponse
  }

  // Set up server that binds to localhost:8080/
  val server = BlazeBuilder.bindHttp(8080, "localhost").mountService(service, "/").run

  projects.foreach(p => print(p.toString))
  print("SERVER NOW RUNNING")

  // Block on reading a line, makes it easy to stop in your terminal (hit enter).
  StdIn.readLine()
  server.shutdownNow()
}
