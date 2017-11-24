package be.kuleuven.proman

import be.kuleuven.proman.models._

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.raw.{Event, HTMLButtonElement, HTMLInputElement}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global // implicit ExecutionContext for Future tasks


object ProManApp {

  def errorAlert(error: Any) = dom.window.alert(error.toString)

  def main(args: Array[String]): Unit = {

//    def update() =
//      Ajax.get("/").onComplete {
//        case Failure(error) => errorAlert(error)
//        case Success(xhr) => {
//            val todosM = decode[Seq[TODOEntry]](xhr.responseText)
//
//            todosM match {
//              case Left(error) => errorAlert(error)
//              case Right(todos) =>
//                val todoTarget = dom.document.body.querySelector("")
//                todoTarget.innerHTML = ""
//                val ui = new TODOEntryTemplate(scalatags.JsDom)
//                todoTarget.appendChild(ui.multipleTemplate(todos).render)
//            }
//          }
//      }
//
//    update()
//
//    val addProjectEl = dom.document.body.querySelector("").asInstanceOf[HTMLButtonElement]
//    addProjectEl.onclick = (ev: Event) => {
//      val projectNameEl = dom.document.body.querySelector("").asInstanceOf[HTMLInputElement]
//      val projectName = projectNameEl.value
//
//      Ajax.post("", new TODOEntry(projectName).asJson.noSpaces)
//        .onComplete {
//          case Failure(error) => errorAlert(error)
//          case Success(xhr) => {
//            projectNameEl.value = ""
//            update()
//          }
//        }
//    }

  }
}
