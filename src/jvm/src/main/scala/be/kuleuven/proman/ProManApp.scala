import java.time.LocalDateTime

import collection.mutable.Seq

object ProManApp extends App {
  val test_todo = new TODOEntry("Finish project", LocalDateTime.now())
  val projects: Seq[TODOProject] = List(new TODOProject("SCALA PROJECT", List(test_todo)))

  print(test_todo)
  projects.foreach(p => print(p.toString))

  val service: HttpService = HttpService {
    case GET -> Root / "hello" =>
      homepage

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
          animals.map( a => li(a.breed + ", " + a.name) )
        )
        //div(
        //  h1("Breed, Name")
        //)
      ),
      script("console.log('hi')")
    ).render
  }.withType(MediaType.`text/html`)

  lazy val projectsJSON: Task[Response] = Ok {
    projects.asJson
  }
}
