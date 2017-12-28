package be.kuleuven.proman.scenes

import be.kuleuven.proman._
import be.kuleuven.proman.models._

import scala.util.control.Breaks._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.syntax._, io.circe.parser._
import cats.syntax.either._
import scala.scalajs.js.Any
import scala.scalajs.js.timers.SetIntervalHandle
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import org.scalajs.dom.html._ // HTMLDivElement => Div
import org.scalajs.dom.raw.{Event, NodeListOf}
import org.scalajs.dom.raw.KeyboardEvent
import scalatags.JsDom.all._ // Client side HTML Tags


object ProjectScene {
  var project: TodoProject = _
  var synchronisation_timestamp: Long = _
  var synchronisation_interval: SetIntervalHandle = _
  val todo_entry_ui = new TodoEntryTemplate(scalatags.JsDom)
  val todo_list_ui = new TodoListTemplate(scalatags.JsDom)

  /**
    * Sets up everything necessary for the current scene.
    * @param project: The project that this scene will display.
    */
  def setupScene(project: TodoProject): Unit = {
    this.project = project
    this.synchronisation_timestamp = 0
    this.setupHTML()
    this.synchronise()
    this.synchronisation_interval = scala.scalajs.js.timers.setInterval(10000) { synchronise() }
  }

  /**
    * Sets up the HTML for the scene along with event handlers.
    */
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
      div(cls := "row", marginTop := 5)(
        div(cls := "col-sm-6", margin := "6px 0")(
          div(
            button(id := "back-to-start", cls := "btn btn-xs btn-primary", title := "Back to list of projects", marginRight := 5)(
              span(cls := "glyphicon glyphicon-arrow-left"), " back"
            ),
            button(id := "btn-edit-description", cls := "btn btn-xs btn-default", title:= "Edit the project's description", marginRight := 5)(
              span(cls := "glyphicon glyphicon-pencil"), " description"
            ),
            button(id := "btn-create-list", cls := "btn btn-xs btn-default", title:= "Create a new list for this project")(
              span(cls := "glyphicon glyphicon-plus"), " list"
            )
          )
        ),
        div(cls := "col-sm-6")(
          input(tpe := "text", id := "todo-filter", placeholder := "Search by todo text", cls := "form-control")
        )
      ),
      div(id := "div-create-list", display := "none")(
        h3("Create a new list"),
        form(id := "form-create-list", name := "form-create-list", action := s"/lists/store", method := "post", cls := "form-inline")(
          div(cls := "form-group")(
            input(tpe := "text", name := "name", placeholder := "Name new list", cls := "form-control", autocomplete := "off", marginRight := 15)
          ),
          button(tpe := "submit", cls := "btn btn-primary")("Create")
        )
      ),
      div(id := "div-create-entry")(
        h3("Create a new entry"),
        form(id := "form-create-todo", name := "form-create-todo", action := s"/todos/store", method := "post", cls := "form-inline")(
          div(cls := "form-group")(
            input(tpe := "text", name := "text", placeholder := "Todo text", cls := "form-control", autocomplete := "off", marginRight := 15)
          ),
          div(cls := "form-group")(
            select(name := "list_id", cls := "form-control", marginRight := 15, width := 225)(
              option(value := "0", disabled, selected)("Select a list for the todo")
            )
          ),
          button(tpe := "submit", cls := "btn btn-primary")("Create")
        )
      ),
      div(id := "todo-lists")(
        div(
          h3(
            h3(cls := "clearfix")(
              div(cls := "pull-left")("Finished todos"),
              div(cls := "pull-right list-button-container")(
                button(cls := "btn btn-sm btn-default todo-list-toggle")(span(cls := "caret caret-up"))
              )
            )
          ),
          div(cls := "table-responsive")(
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

    dom.document.getElementById("btn-create-list").asInstanceOf[Button].onclick = Any.fromFunction1(_ => {
      val form_container = dom.document.getElementById("div-create-list").asInstanceOf[Div]
      if (form_container.style.display == "none")  {
        form_container.style.display = "block"
        form_container.querySelector("input").asInstanceOf[Input].focus()
      } else {
        form_container.style.display = "none"
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

    val input_filter = dom.document.getElementById("todo-filter").asInstanceOf[Input]
    input_filter.onkeyup = Any.fromFunction1((e: Event) => {
      this.filterTodoEntries(input_filter.value)
    })

    dom.document.getElementById("back-to-start").asInstanceOf[Button].onclick = Any.fromFunction1(_ => {
      scala.scalajs.js.timers.clearInterval(this.synchronisation_interval)
      StartScene.setupScene()
    })

    this.setupTodoListTable(dom.document.getElementById("todo-lists").firstChild.asInstanceOf[Div])
  }

  /**
    * Creates a new table that represents a TodoList.
    * @param list: The new TodoList.
    */
  def createTodoListTable(list: TodoList): Unit = {
    val list_container = dom.document.getElementById("todo-lists").asInstanceOf[Div]
    val new_list = list_container.insertBefore(
      this.todo_list_ui.singleTemplate(list).render,
      list_container.childNodes.item(this.getNewTodoListIndex(list.name))
    ).asInstanceOf[Div]

    this.setupTodoListTable(new_list)
  }

  /**
    * Returns the index in the view for a new TodoList with a given name. Lists in the view are sorted alphabetically
    * by name.
    * @param name: The name of the new TodoList
    */
  def getNewTodoListIndex(name: String): Int = {
    var names: Seq[String] = Seq(name)
    val spans = dom.document.querySelectorAll("span.todo-list-name").asInstanceOf[NodeListOf[Span]]
    for (i <- 0 until spans.length) {
      names :+= spans.item(i).innerHTML
    }
    names.sortWith((a, b) => a.toLowerCase() < b.toLowerCase()).indexOf(name)
  }

  /**
    * Creates a new TableRow in the correct table for a TodoEntry.
    * @param new_todo: The new TodoEntry.
    */
  def createTodoInTable(new_todo: TodoEntry): Unit = {
    val querySelector = if (new_todo.is_done) "#finished-todo-tbody" else s"tbody.todo-list-tbody[data-id='${new_todo.list_id}']"
    val tbody = dom.document.querySelector(querySelector).asInstanceOf[TableSection]

    // Find index where to insert the new row at. The entries are in descending timestamp order.
    var i = 0
    breakable {
      while (i < tbody.childElementCount) {
        val timestamp = tbody.childNodes.item(i).asInstanceOf[TableRow]
          .querySelector(".todo-timestamp").getAttribute("data-timestamp").toLong

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
    * Updates a todo's is_done value and moves it to the list that it belongs after the update.
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
            // move tr to other table
            tr.parentNode.removeChild(tr)
            this.createTodoInTable(todo)
        }
    }
  }

  /**
    * Sets up event handlers for a TodoList's table.
    * @param div: The container for the TodoList
    */
  def setupTodoListTable(div: Div): Unit = {
    val btn_toggle_list = div.querySelector("button.todo-list-toggle").asInstanceOf[Button]
    val btn_toggle_input = div.querySelector("button.todo-list-edit").asInstanceOf[Button]
    val span_name = div.querySelector("span.todo-list-name").asInstanceOf[Span]
    val input_name = div.querySelector("input[name='name']").asInstanceOf[Input]

    btn_toggle_list.onclick = Any.fromFunction1(_ => {
      val table = div.querySelector("table").asInstanceOf[Table]
      table.style.display = if (table.style.display == "none") "table" else "none"
      btn_toggle_list.firstChild.asInstanceOf[Span].classList.toggle("caret-up")
    })

    if (btn_toggle_input != null) {
      btn_toggle_input.onclick = Any.fromFunction1(_ => {
        if (span_name.style.display != "none") {
          span_name.style.display = "none"
          input_name.value = span_name.innerHTML
          input_name.style.display = "block"
          input_name.focus()
          input_name.setSelectionRange(input_name.value.length, input_name.value.length)
        } else {
          input_name.style.display = "none"
          span_name.innerHTML = input_name.value
          span_name.style.display = "block"
        }
      })
    }

    if (input_name != null) {
      input_name.onkeyup = Any.fromFunction1((e: KeyboardEvent) => {
        if (e.keyCode == 13) {
          btn_toggle_input.click()
        }

        decode[TodoList](div.getAttribute("data-json")) match {
          case Left(error) => printError(error)
          case Right(todo_list) =>
            todo_list.name = input_name.value

            Ajax.put("/list/" + todo_list.id, todo_list.asJson.noSpaces).onComplete {
              case Failure(error) => printError(error)
              case Success(xhr) =>
                decode[TodoList](xhr.responseText) match {
                  case Left(error) => printError(error)
                  case Right(updated) => div.setAttribute("data-json", updated.asJson.noSpaces)
                }
            }
        }
      })
    }
  }

  /**
    * Sets up event handlers for a TodoEntry's TableRow.
    * @param tr: The TableRow to set the handlers on.
    */
  def setupTodoTableRow(tr: TableRow): Unit = {
    val td_timestamp = tr.querySelector(".todo-timestamp").asInstanceOf[TableDataCell]
    val btn_finished = tr.querySelector(".todo-finished").asInstanceOf[Button]
    val btn_pending = tr.querySelector(".todo-pending").asInstanceOf[Button]
    val btn_toggle_input = tr.querySelector(".todo-edit").asInstanceOf[Button]
    val input_text = tr.querySelector(".todo-text input").asInstanceOf[Input]
    val span_text = tr.querySelector(".todo-text span").asInstanceOf[Span]

    td_timestamp.innerHTML = formatTimeStamp(td_timestamp.getAttribute("data-timestamp").toLong)

    if (btn_finished != null) {
      btn_finished.onclick = Any.fromFunction1(_ => { updateTODOIsDoneStatus(tr, is_done=false) })
    }

    if (btn_pending != null) {
      btn_pending.onclick = Any.fromFunction1(_ => { updateTODOIsDoneStatus(tr, is_done=true) })
    }

    btn_toggle_input.onclick = Any.fromFunction1(_ => {
      if (span_text.style.display != "none") {
        span_text.style.display = "none"
        input_text.value = span_text.innerHTML
        input_text.style.display = "block"
        input_text.focus()
        input_text.setSelectionRange(input_text.value.length, input_text.value.length)
      } else {
        span_text.style.display = "block"
        span_text.innerHTML = input_text.value
        input_text.style.display = "none"
      }
    })

    // Update the entry's text. every keystroke is an update
    input_text.onkeyup = Any.fromFunction1((e: KeyboardEvent) => {
      if (e.keyCode == 13) {
        btn_toggle_input.click()
      }

      decode[TodoEntry](tr.getAttribute("data-json")) match {
        case Left(error) => printError(error)
        case Right(todo) =>
          todo.text = input_text.value

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
  }

  /**
    * Submits an update to the currently opened project. Currently, only the description can be updated.
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
    * Submits a new TodoList for the currently opened project and inserts it into the view if it has been created
    * successfully.
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
          form.parentElement.style.display = "none"
          decode[TodoList](xhr.responseText) match {
            case Left(error) => printError(error)
            case Right(new_list) => this.createTodoListTable(new_list)
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

    val input_text = form.elements.namedItem("text").asInstanceOf[Input]
    if (input_text.value.length() == 0) {
      showError("Fill in a message first!")
    } else {
      val list_select: Select = form.elements.namedItem("list_id").asInstanceOf[Select]
      val list_id: Long = list_select.options(list_select.selectedIndex).value.toLong

      if (list_id == 0) {
        showError("Choose a list for the todo first!")
      } else {
        Ajax.post(form.action, new TodoEntry(list_id, input_text.value).asJson.noSpaces).onComplete {
          case Failure(error) => printError(error)
          case Success(xhr) =>
            input_text.value = ""
            decode[TodoEntry](xhr.responseText) match {
              case Left(error) => printError(error)
              case Right(new_todo) => this.createTodoInTable(new_todo)
            }
        }
      }
    }
  }

  /**
    * Synchronisation policy: Send project id and the local sync timestamp to the synchronisation route. The JSON
    * response of the server will contain a new timestamp for future sync requests along with objects that have been
    * created or updated since the last synchronisation.
    */
  def synchronise(): Unit = {
    Ajax.get(s"sync/todos/${this.project.id}/${this.synchronisation_timestamp}").onComplete {
      case Failure(error) => printError(error)
      case Success(xhr) =>
        println(s"synchronising for timestamp: ${formatTimeStamp(this.synchronisation_timestamp)}, response: " + xhr.responseText)
        parse(xhr.responseText) match {
          case Left(error) => printError(error)
          case Right(json) =>
            val project: TodoProject = json.hcursor.downField("project").as[TodoProject].getOrElse(null)
            if (project != null) {
              this.project = project
              dom.document.getElementById("project-description").innerHTML = this.project.description
            }

            this.updateLists(json.hcursor.downField("lists").as[Seq[TodoList]].getOrElse(List()))
            this.updateTodos(json.hcursor.downField("todos").as[Seq[TodoEntry]].getOrElse(List()))

            this.synchronisation_timestamp = json.hcursor.downField("timestamp").as[Long].getOrElse(this.synchronisation_timestamp)
        }
    }
  }

  /**
    * Updates the lists on the page. If a list is new, a new table is created. If a list has been updated,
    * then the name of the list on the page is updated, unless it is being edited. A list is being edited
    * if an input element exists in the row of the entry. The select element for new TodoEntries is updated
    * as well.
    *
    * @param lists: List of updated or new lists since last synchronisation.
    */
  def updateLists(lists: Seq[TodoList]): Unit = {
    lists.foreach(list => {
      val span = dom.document.querySelector(s"span.todo-list-name[data-id='${list.id}']").asInstanceOf[Span]
      if (span != null) {
        // Only rename the list if it is not being edited. No requirement is given for the case where multiple people
        // are editing something. In this case, the entry will get the text of the input box that was updated the latest.
        if (span.parentElement.parentElement.querySelector("input") != null) {
          span.innerHTML = list.name
        }
      } else {
        this.createTodoListTable(list)
      }

      val option_list = dom.document.querySelector(s"option[value='${list.id}']").asInstanceOf[Option]
      if (option_list != null) {
        option_list.innerHTML = list.name
      } else {
        val select_list = dom.document.querySelector("select[name='list_id']").asInstanceOf[Select]

        select_list.insertBefore(
          option(value := list.id)(list.name).render,
          select_list.childNodes.item(this.getNewTodoListIndex(list.name) + 1)
        ).asInstanceOf[Div]
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
      val row = dom.document.querySelector(s"tr[data-id='${todo.id}']").asInstanceOf[TableRow]
      if (row != null) {
        // Only replace the row if it is not being edited. No requirement is given for the case where multiple people
        // are editing an entry. In this case, the entry will get the text of the input box that was updated the latest.
        val input_text = row.querySelector("input[name='text']").asInstanceOf[Input]
        if (input_text.style.display == "none") {
          row.parentElement.removeChild(row)
          this.createTodoInTable(todo)
        }
      } else {
        this.createTodoInTable(todo)
      }
    })
  }

  /**
    * Filters out the TodoEntries in the view based on a given string. All TodoEntries that do not contain the given
    * string in their text are hidden in the view.
    * @param filter_text: The given text to filter the entries on.
    */
  def filterTodoEntries(filter_text: String): Unit = {
    val todo_rows = dom.document.querySelectorAll("tr[data-id]").asInstanceOf[NodeListOf[TableRow]]
    for (i <- 0 until todo_rows.length) {
      val tr = todo_rows.item(i)
      val todo_text: String = tr.querySelector(".todo-text span").asInstanceOf[Span].innerHTML
      tr.style.display = if (todo_text.toLowerCase().contains(filter_text.toLowerCase())) "table-row" else "none"
    }
  }
}
