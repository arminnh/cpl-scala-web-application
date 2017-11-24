package be.kuleuven.proman

import be.kuleuven.proman.controllers._
import be.kuleuven.proman.models._

import scala.io.StdIn
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.server.blaze.BlazeBuilder



object ProManApp extends App {

  val test_todo = new TODOEntry("Finish project")
  var projects: Seq[TODOProject] = Seq(
      new TODOProject("SCALA PROJECT", List(test_todo))
  )

  // Service that matches requests (at routes) to responses (by controller actions).
  val service: HttpService = HttpService {
    case GET            -> Root                         => TemporaryRedirect(uri("/projects"))

    case GET            -> Root / "projects"            => TODOProjectController.index(projects)
    //case request @ POST -> Root / "projects" / "store"  => TODOProjectController.store(projects, request)
    case request @ POST -> Root / "projects" / "store"  =>
      try {
        for {
          project <- request.as(jsonOf[TODOProject])
          //aa <- request.as
          response <- Ok(project.name.asJson)
        } yield {
          projects = project +: projects
          println(projects)
          //        val projectss = project +: projects
          //        println(projectss)
          response
        }
      } catch {
        case e: MalformedMessageBodyFailure => Ok(println("malformed " + e.message + "\n" + e.details))
        case e: InvalidMessageBodyFailure => Ok(println("malformed " + e.message + "\n" + e.details))
        case e: Throwable => Ok(println("WTF: " + e))
      }

    case GET            -> Root / "js"                  => TODOProjectController.javascriptProgramResponse

    case _ => NotFound("Not found!")
  }

  // Set up server that binds to localhost:8080/
  val server = BlazeBuilder.bindHttp(8080, "localhost").mountService(service, "/").run

  projects.foreach(p => print(p.toString))
  println("SERVER NOW RUNNING")

  // Block on reading a line, makes it easy to stop in your terminal (hit enter).
  StdIn.readLine()
  server.shutdownNow()
}
