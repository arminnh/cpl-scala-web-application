package be.kuleuven

import org.scalajs.dom
import org.scalajs.dom.html._ // HTMLDivElement => Div

import scala.scalajs.js

package object proman {

  // Error: Referring to non-existent class java.text.SimpleDateFormat
  def formatTimeStamp(ts: Long): String = {
    //val date = new js.Date()
    val date = new js.Date(ts)
    date.getUTCFullYear() + "-" + date.getUTCMonth() + "-" + date.getUTCDate() + " " +
      date.getUTCHours() + ":" + date.getUTCMinutes() + ":" + date.getUTCSeconds()
  }

  def printError(error: Throwable): Unit = {
    showError("Error: see console")
    println("Throwable error: " + error)
    println(error.printStackTrace())
  }

  def printError(error: Error): Unit = {
    showError("Error: see console")
    println("Error: " + error)
    println(error.printStackTrace())
  }

  def showError(error: String): Unit = {
    val container = dom.document.getElementById("error-container").asInstanceOf[Div]
    container.innerHTML = error
    container.style.display = ""
  }

  def hideError(): Unit = {
    val container = dom.document.getElementById("error-container").asInstanceOf[Div]
    container.style.display = "none"
  }
}

