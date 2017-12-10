package be.kuleuven.proman.scenes

import be.kuleuven.proman._
import be.kuleuven.proman.models._

import scala.util.{Failure, Success}
import scala.util.control.Breaks._
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.syntax._, io.circe.parser._, io.circe.Json
import cats.syntax.either._
import org.scalajs.dom.raw.{HTMLElement, KeyboardEvent, MouseEvent}
//import io.circe.generic.auto._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html._ // HTMLDivElement => Div
import org.scalajs.dom.raw.{Event, NodeListOf}
import scalatags.JsDom.all._ // Client side HTML Tags
import scala.scalajs.js.Any

//noinspection AccessorLikeMethodIsUnit
object ProjectScene {
  var state_entries: Long = -999
  var state_lists: Long = -999
  var project: TodoProject = _
  var synchronisation_interval: Int = -999
  lazy val todo_entry_ui = new TodoEntryTemplate(scalatags.JsDom)
  lazy val todo_list_ui = new TodoListTemplate(scalatags.JsDom)

  def setupHTML(): Unit = {
    hideError()

    dom.document.title = s"Project: ${this.project.name}"
    dom.document.getElementById("top-title").innerHTML = div(
      h1(s"Project: ${this.project.name}", fontSize := 36),
      p(id := "project-description", whiteSpace := "pre-wrap")(
        this.project.description
      ),
      form(id := "form-update-description", name := "form-update-description", action := s"/project/${this.project.id}", method := "put", display := "none")(
        br,
        div(cls := "form-group")(
          textarea(name := "description", placeholder := "The description of the project", cls := "form-control")
        ),
        button(tpe := "submit", cls := "btn btn-primary")("Submit")
      )
    ).render.innerHTML

    dom.document.getElementById("content").innerHTML = div(
      div(marginTop := 5)(
        button(id := "back-to-start", cls := "btn btn-xs btn-primary", title := "Back to list of projects", marginRight := 5)(
          span(cls := "glyphicon glyphicon-arrow-left"), " back"
        ),
        button(id := "btn-edit-description", cls := "btn btn-xs btn-default", title:= "Edit the project's description", marginRight := 5)(
          span(cls := "glyphicon glyphicon-pencil"), " description"
        ),
        button(id := "btn-create-list", cls := "btn btn-xs btn-default", title:= "Create a new list for this project")(
          span(cls := "glyphicon glyphicon-plus"), " list"
        )
      ),
      div(cls := "row")(
        div(cls := "col-sm-6")(
          h3("Create a new list"),
          form(id := "form-create-list", name := "form-create-list", action := s"/lists/store", method := "post", cls := "form-inline")(
            div(cls := "form-group")(
              input(tpe := "text", name := "name", placeholder := "Name", cls := "form-control", autocomplete := "off", marginRight := 15)
            ),
            button(tpe := "submit", cls := "btn btn-primary")("Create")
          )
        )
      ),
      div(cls := "row")(
        div(cls := "col-sm-6")(
          h3("Create a new entry"),
          form(id := "form-create-todo", name := "form-create-todo", action := s"/todos/store", method := "post", cls := "form-inline")(
            div(cls := "form-group")(
              input(tpe := "text", name := "name", placeholder := "Message", cls := "form-control", autocomplete := "off", marginRight := 15)
            ),
            button(tpe := "submit", cls := "btn btn-primary")("Create")
          )
        ),
        div(cls := "col-sm-6")(
          h3("Search"),
          div(cls := "form-group")(
            input(tpe := "text", name := "filter", placeholder := "Search by todo text", cls := "form-control", autocomplete := "off")
          )
        )
      ),
      div(id := "todo-lists")(
        div(
          h3(
            div(
              "Finished todos ",
              button(cls := "btn btn-sm btn-default todo-list-toggle")(span(cls := "caret caret-up"))
            )
          ),
          div(id := "finished-todo-list-container", cls := "table-responsive")(
            table(cls := "table table-condensed table-striped table-hover")(
              tbody(id := "finished-todo-tbody")
            )
          )
        )
      )
    ).render.innerHTML

    dom.document.getElementById("btn-edit-description").asInstanceOf[Button].onclick = Any.fromFunction1(_ => {
      val form = dom.document.getElementById("form-update-description").asInstanceOf[Form]
      val p = dom.document.getElementById("project-description").asInstanceOf[Paragraph]

      if (form.style.display == "none") {
        form.elements.namedItem("description").asInstanceOf[TextArea].value = this.project.description
        form.style.display = "block"
        p.style.display = "none"
      } else {
        form.style.display = "none"
        p.style.display = "block"
      }
    })

    dom.document.getElementById("form-update-description").asInstanceOf[Form].onsubmit = Any.fromFunction1((e: Event) => {
      e.preventDefault()
      this.submitProjectUpdate(getFormFromEvent(e))
    })

    dom.document.getElementById("form-create-list").asInstanceOf[Form].onsubmit = Any.fromFunction1((e: Event) => {
      e.preventDefault()
      this.submitNewList(getFormFromEvent(e))
    })

    dom.document.getElementById("form-create-todo").asInstanceOf[Form].onsubmit = Any.fromFunction1((e: Event) => {
      e.preventDefault()
      this.submitNewTodo(getFormFromEvent(e))
    })

    dom.document.getElementById("back-to-start").asInstanceOf[Button].onclick = Any.fromFunction1(_ => {
      dom.window.clearInterval(this.synchronisation_interval)
      StartScene.setupScene()
    })

    this.setupTodoListTable(dom.document.getElementById("todo-lists").firstChild.asInstanceOf[Div])
  }

  def setupScene(project: TodoProject): Unit = {
    this.project = project
    this.state_entries = -999
    this.state_lists = -999
    this.setupHTML()
    this.synchronise()
    this.synchronisation_interval = dom.window.setInterval(Any.fromFunction0(() => synchronise()), 2500)
  }

  /**
    * Updates a todo's is_done value.
    * @param tr: The row that represents the todo entry.
    * @param is_done: The new is_done value.
    */
  def updateTODOIsDoneStatus(tr: TableRow, is_done: Boolean): Unit = {
    decode[TodoEntry](tr.getAttribute("data-json")) match {
      case Left(error) => printError(error)
      case Right(todo) =>
        todo.is_done = is_done

        Ajax.put("/todo/"+todo.id, todo.asJson.noSpaces).onComplete {
          case Failure(error) => printError(error)
          case Success(xhr) =>
            val updatedM = decode[TodoEntry](xhr.responseText)

            // move tr to other table
            tr.parentNode.removeChild(tr)
            this.createTodoInTable(todo)

            updatedM match {
              case Left(error) => printError(error)
              case Right(updated) => tr.setAttribute("data-json", updated.asJson.noSpaces)
            }
        }
    }
  }

  /**
    * Creates a new list/table for TodoEntries.
    * @param list: The new TodoList.
    */
  def createListTable(list: TodoList): Unit = {
    // Find where to place the list.
    var names: Seq[String] = Seq(list.name)
    val spans = dom.document.querySelectorAll("span.todo-list-name").asInstanceOf[NodeListOf[Span]]
    for (i <- 0 until spans.length) {
      names :+= spans.item(i).innerHTML
    }
    val i = names.sorted.indexOf(list.name) // TODO: sorted by ascii values?

    val list_container = dom.document.getElementById("todo-lists").asInstanceOf[Div]
    val l = list_container.insertBefore(todo_list_ui.singleTemplate(list).render, list_container.childNodes.item(i))
    this.setupTodoListTable(l.asInstanceOf[Div])
  }

  /**
    * Sets up event handlers for a TodoList's table.
    * @param div: The container for the TodoList
    */
  def setupTodoListTable(div: Div): Unit = {
    val toggle_btn = div.querySelector(".todo-list-toggle").asInstanceOf[Button]
    val edit_btn = div.querySelector(".todo-list-edit").asInstanceOf[Button]
    val list_input = div.querySelector("input").asInstanceOf[Input]

    toggle_btn.onclick = Any.fromFunction1(_ => {
      val container = toggle_btn.parentElement.nextElementSibling.asInstanceOf[Div]
      container.style.display = if (container.style.display == "none") "block" else "none"
      toggle_btn.firstChild.asInstanceOf[Span].classList.toggle("caret-up")
    })

    list_input.onkeyup = Any.fromFunction1((e: KeyboardEvent) => {
      println("editing list name")
      if (e.keyCode == 13) {
        list_input.parentElement.style.display = "none"
        edit_btn.previousElementSibling.innerHTML = list_input.value
        edit_btn.parentElement.style.display = "block"
      }

      decode[TodoList](div.getAttribute("data-json")) match {
        case Left(error) => printError(error)
        case Right(list) =>
          list.name = list_input.value

          Ajax.put("/list/"+list.id, list.asJson.noSpaces).onComplete {
            case Failure(error) => printError(error)
            case Success(xhr) =>
              decode[TodoList](xhr.responseText) match {
                case Left(error) => printError(error)
                case Right(updated) => div.setAttribute("data-json", updated.asJson.noSpaces)
              }
          }
      }
    })

    edit_btn.onclick = Any.fromFunction1(_ => {
      println(edit_btn)
      val div_name = edit_btn.parentElement.asInstanceOf[Div]

      if (div_name.style.display != "none") {
        div_name.style.display = "none"
        list_input.parentElement.style.display = "block"
        list_input.focus()
      } else {
        div_name.firstElementChild.innerHTML = list_input.value
        div_name.style.display = "block"
        list_input.parentElement.style.display = "none"
      }
    })
    
  }

  /**
    * Creates a new TableRow in the correct table for a TodoEntry.
    * @param new_todo: The new TodoEntry.
    */
  def createTodoInTable(new_todo: TodoEntry): Unit = {
    val querySelector = if (new_todo.is_done) "finished-todo-tbody" else s".todo-list-tbody[data-id=${new_todo.list_id}]"
    val tbody = dom.document.querySelector(querySelector).asInstanceOf[TableSection]

    // Find index where to insert the new row at. The entries are in descending timestamp order.
    var i = 0
    breakable {
      while (i < tbody.childElementCount) {
        val timestamp = tbody.childNodes.item(i).asInstanceOf[TableRow]
          .getElementsByClassName("todo-timestamp").item(0).asInstanceOf[TableCell]
          .getAttribute("data-timestamp").toLong

        if (new_todo.timestamp < timestamp) {
          i += 1
        } else {
          break
        }
      }
    }

    // Create a new row at the correct position.
    val tempRow = tbody.insertRow(i)
    val newRow = tbody.insertBefore(todo_entry_ui.singleTemplate(new_todo).render, tempRow)
    tbody.removeChild(tempRow)
    this.setupTodoTableRow(newRow.asInstanceOf[TableRow])
  }

  /**
    * Sets up event handlers for a TodoEntry's TableRow.
    * @param tr: The TableRow to set the handlers on.
    *          TODO: refactor with QuerySelector
    */
  def setupTodoTableRow(tr: TableRow): Unit = {
    val timestamp_tds = tr.getElementsByClassName("todo-timestamp").asInstanceOf[NodeListOf[TableDataCell]]
    for (i <- 0 until timestamp_tds.length) {
      val td = timestamp_tds.item(i)
      td.innerHTML = formatTimeStamp(td.getAttribute("data-timestamp").toLong)
    }

    val finished_btns = tr.getElementsByClassName("todo-finished").asInstanceOf[NodeListOf[Button]]
    for (i <- 0 until finished_btns.length) {
      val btn = finished_btns.item(i)
      btn.onclick = Any.fromFunction1(_ => { updateTODOIsDoneStatus(tr, is_done=false) })
    }

    val pending_btns = tr.getElementsByClassName("todo-pending").asInstanceOf[NodeListOf[Button]]
    for (i <- 0 until pending_btns.length) {
      val btn = pending_btns.item(i)
      btn.onclick = Any.fromFunction1(_ => { updateTODOIsDoneStatus(tr, is_done=true) })
    }

    val edit_btns = tr.getElementsByClassName("todo-edit").asInstanceOf[NodeListOf[Button]]
    for (i <- 0 until edit_btns.length) {
      val btn = edit_btns.item(i)
      btn.onclick = Any.fromFunction1(_ => {
        val td_text = tr.getElementsByClassName("todo-text").item(0).asInstanceOf[TableDataCell]

        if (td_text.hasChildNodes() && td_text.firstChild.nodeName != "#text") {
          // replace input box by it's value
          td_text.innerHTML = td_text.firstChild.asInstanceOf[Input].value
        } else {
          // replace text by a new input box
          val input_node = input(tpe := "text", name := "text", placeholder := "Message",  cls := "form-control",
                                 value := td_text.innerHTML, height := 24).render
          td_text.innerHTML = ""
          td_text.appendChild(input_node)

          // Update the entry's text. every keystroke is an update
          input_node.onkeyup = Any.fromFunction1((e: KeyboardEvent) => {
            if (e.keyCode == 13) {
              td_text.innerHTML = input_node.value
            }

            decode[TodoEntry](tr.getAttribute("data-json")) match {
              case Left(error) => printError(error)
              case Right(todo) =>
                todo.text = input_node.value

                Ajax.put("/todo/"+todo.id, todo.asJson.noSpaces).onComplete {
                  case Failure(error) => printError(error)
                  case Success(xhr) =>
                    decode[TodoEntry](xhr.responseText) match {
                      case Left(error) => printError(error)
                      case Right(updated) => tr.setAttribute("data-json", updated.asJson.noSpaces)
                    }
                }
            }
          })

          input_node.focus()
          input_node.setSelectionRange(input_node.value.length, input_node.value.length)
        }
      })
    }
  }

  /**
    * Submits an update to the currently opened project's description.
    * @param form: The form to be submitted.
    */
  def submitProjectUpdate(form: Form): Unit = {
    val description = form.elements.namedItem("description").asInstanceOf[TextArea].value
    this.project.description = description

    Ajax.put(form.action, this.project.asJson.noSpaces).onComplete {
      case Failure(error) => printError(error)
      case Success(xhr) =>
        decode[TodoProject](xhr.responseText) match {
          case Left(error) => printError(error)
          case Right(updated) =>
            this.project = updated
            dom.document.getElementById("project-description").innerHTML = description
            dom.document.getElementById("btn-edit-description").asInstanceOf[Button].click()
        }
    }
  }

  /**
    * Submits a new TodoList for the currently opened project and
    * inserts it into the view if it has been created successfully.
    * @param form: The form to be submitted
    */
  def submitNewList(form: Form): Unit = {
    hideError()

    val name = form.elements.namedItem("name").asInstanceOf[Input].value
    if (name.length() == 0) {
      showError("Fill in a name first!")
    } else {
      Ajax.post(form.action, new TodoList(this.project.id, name).asJson.noSpaces).onComplete {
        case Failure(error) => printError(error)
        case Success(xhr) =>
          form.reset()
          form.parentElement.parentElement.style.display = "none"
          decode[TodoList](xhr.responseText) match {
            case Left(error) => printError(error)
            case Right(new_list) => this.createListTable(new_list)
          }
      }
    }
  }

  /**
    * Submits a new TodoEntry and adds it to the correct table if it has been created successfully.
    * @param form: The form to be submitted.
    */
  def submitNewTodo(form: Form): Unit = {
    hideError()

    val name = form.elements.namedItem("name").asInstanceOf[Input].value
    val list_select = form.elements.namedItem("list_id").asInstanceOf[Select]
    val list_id = list_select.options(list_select.selectedIndex).asInstanceOf[Long]

    if (name.length() == 0) {
      showError("Fill in a message first!")
    } else {
      //Ajax.post(form.action, name.asJson.noSpaces).onComplete {
      Ajax.post(form.action, new TodoEntry(list_id, name).asJson.noSpaces).onComplete {
        case Failure(error) => printError(error)
        case Success(xhr) =>
          form.reset()
          decode[TodoEntry](xhr.responseText) match {
            case Left(error) => printError(error)
            case Right(new_todo) => this.createTodoInTable(new_todo)
          }
      }
    }
  }

  /**
    * Synchronisation policy: Send the local state variable to the synchronisation route. The JSON response
    * of the server will contain the latest state variable and a list of todos that have been updated or
    * created since the last synchronisation.
    */
  def synchronise(): Unit = {
    Ajax.get(s"lists/sync/${this.state_lists}/${this.project.id}").onComplete {
      case Failure(error) => printError(error)
      case Success(xhr) =>
        println(s"synchronising for list state: ${this.state_lists}, response: ${xhr.responseText}")
        parse(xhr.responseText) match {
          case Left(error) => printError(error)
          case Right(json) =>
            this.updateLists(json.hcursor.downField("lists").as[Seq[TodoList]].getOrElse(List()))
            this.state_lists = json.hcursor.downField("state").as[Long].getOrElse(this.state_lists)

            Ajax.get(s"todos/sync/${this.state_entries}/${this.project.id}/${this.project.version}").onComplete {
              case Failure(error) => printError(error)
              case Success(xhr) =>
                println(s"synchronising for state: ${this.state_entries}, response: ${xhr.responseText}")
                parse(xhr.responseText) match {
                  case Left(error) => printError(error)
                  case Right(json) =>
                    this.updateTodos(json.hcursor.downField("todos").as[Seq[TodoEntry]].getOrElse(List()))
                    this.state_entries = json.hcursor.downField("state").as[Long].getOrElse(this.state_entries)

                    val project: TodoProject = json.hcursor.downField("project").as[TodoProject].getOrElse(null)
                    if (project != null) {
                      this.project = project
                      dom.document.getElementById("project-description").innerHTML = this.project.description
                    }
                }
            }
        }
    }
  }

  /**
    * Updates the lists on the page. If a list is new, a new table is created. If a list has been updated,
    * then the name of the list on the page is updated, unless it is being edited. A list is being edited
    * if an input element exists in the row of the entry.
    *
    * @param lists: List of updated or new lists since last synchronisation.
    */
  def updateLists(lists: Seq[TodoList]): Unit = {
    lists.foreach(list => {
      println(list)
      val span = dom.document.querySelector(s"span.todo-list-name[data-id=${list.id}]").asInstanceOf[Span]
      println(span)

      if (span != null) {
        // Only rename the list if it is not being edited. No requirement is given for the case where multiple people
        // are editing something. In this case, the entry will get the text of the input box that was updated the latest.
        if (span.parentElement.parentElement.querySelector("input") != null) {
          span.innerHTML = list.name
        }

      } else {
        this.createListTable(list)
      }
    })
  }

  /**
    * Updates the todos on the page. If an entry is new, a new row is created. If an entry has been updated,
    * then the row is replaced by its updated version, unless it is being edited. An entry is being edited
    * if an input element exists in the row of the entry.
    *
    * @param todos: List of updated or new todos since last synchronisation.
    */
  def updateTodos(todos: Seq[TodoEntry]): Unit = {
    todos.foreach(todo => {
      val row = dom.document.querySelector(s"tr[data-id=${todo.id}]").asInstanceOf[TableRow]
      if (row != null) {
        // Only replace the row if it is not being edited. No requirement is given for the case where multiple people
        // are editing an entry. In this case, the entry will get the text of the input box that was updated the latest.
        if (row.getElementsByTagName("input").length == 0) {
          row.parentElement.removeChild(row)
          this.createTodoInTable(todo)
        }
      } else {
        this.createTodoInTable(todo)
      }
    })
  }

}
