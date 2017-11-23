// package be.kuleuven.proman

import scala.scalajs.js
import org.scalajs.dom
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

import dom.ext.Ajax

object ProManApp {

  print("hello wtf")

  def main(args: Array[String]): Unit = {
    print("wtffff")

    var xhr = new dom.XMLHttpRequest()
    xhr.open("GET", "/service/animals")
    xhr.onload = { (e: dom.Event) =>
        if (xhr.status == 200) {
            val container = dom.document.querySelector("#animals-container")

            // val animals: Either[io.circe.Error, List[Animal]] = decode[List[Animal]](xhr.responseText)
            //
            // animals match {
            //     case Left(error) => dom.window.alert(error.getMessage)
            //     case Right(list) => list.foreach( animal => {
            //         val li = dom.document.createElement("li")
            //         li.textContent = animal.breed + ", " + animal.name
            //         container.appendChild(li)
            //     })
            // }
            //
            // // animals.right = List[List[Animal]] ???
            // animals.right.foreach(
            //     wtf => wtf.foreach(
            //         animal => {
            //             val li = dom.document.createElement("li")
            //             li.textContent = animal.breed + ", " + animal.name
            //             container.appendChild(li)
            //         }
            //     )
            // )
        }

    }
    xhr.send()
  }
}
