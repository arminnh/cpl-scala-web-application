TODO: check projectscene after changing description

All lists can be hidden.  
Lists in the ProjectScene are sorted by name.

**Synchronisation state improvement:**
* todo_changes(project_id, list_id, entry_id, timestamp)
* Views synchronize on timestamp
* Example query for ProjectScene: select * from todo_entries where id in (select entry_id from todo_changes where timestamp > ?) 

# TODO
* extension: Render as much as you can on the server (e.g. initial project list should be rendered on the server): Move some html bits to a new shared class. e.g. lazy val start_scene_title = div(h1(...)).render.innerHTML
add these things in the response in the controllers and replace the html bits in the scene js code.


# Implementation of multi-user functionality

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/js/src/main/scala/be/kuleuven/proman/scenes/ProjectScene.scala
* src/js/src/main/scala/be/kuleuven/proman/scenes/StartScene.scala
* src/jvm/src/main/scala/be/kuleuven/proman/controllers/TodoEntriesController.scala
* src/jvm/src/main/scala/be/kuleuven/proman/controllers/TodoListsController.scala
* src/jvm/src/main/scala/be/kuleuven/proman/controllers/TodoProjectsController.scala
* src/main/scala/be/kuleuven/proman/repositories/TodoEntryRepository.scala
* src/jvm/src/main/scala/be/kuleuven/proman/repositories/TodoListRepository.scala
* src/jvm/src/main/scala/be/kuleuven/proman/repositories/TodoProjectRepository.scala

### Textual Explanation of the Feature
This is not an extension, a explanation of how the multi-user functionality was achieved in this project.
### Textual Explanation of the Solution

• All functionality has to be used by multiple users! All projects are visible
to everyone who uses the application. This means that the state is likely
maintained on the server.  
• Users should see an updated version of the server state, the program should
update every 10 seconds (that is, contact your server every 10 seconds
to ask for the latest state). Updates that a user made should be visible
without waiting for 10 seconds (unless the delay to the server is > 10s).

Synchronisation

On ProjectScene, TodoProject synchronisation is piggybacked with TodoList.

This solution was chosen in order to diminish the amount of data being sent from and to the server. An improvement 
on this implementation would be to only send changes and how to apply them from the server to client, instead of 
sending the whole updated objects. 

# Extensions

Fill in all sections on each extension. I primarily want to know if you finished
the extension, which files have something to do with your implementation and a
description of how you implemented the feature.


## Red Alert and Description Box
  
### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/jvm/src/main/scala/be/kuleuven/proman/ProManApp.scala
* src/js/src/main/scala/be/kuleuven/proman/proman.scala
* src/shared/src/main/scala/be/kuleuven/proman/models/TodoProject.scala
* src/js/src/main/scala/be/kuleuven/proman/scenes/ProjectScene.scala
### Textual Explanation of the Solution
The base html of the application (see 'HTML' field in ProManApp) contains a div with id "error-container". 
This div is used to display all errors and uses Bootstrap's alert class for styling. The proman package object 
contains `showError(error: String)`, which makes the alert div visible and sets the error text, and `hideError()`, 
which hides the alert div.  

I interpreted the "description box below the project name" part of this extension as projects having descriptions.
In order to achieve this functionality, the TodoProject class contains a 'description' field. On the project scene,
the description of the project is displayed under the project title.  
A hidden form is added as well so that changes can be made to descriptions. Pressing the button that shows "description"
next to a pencil icon will hide the description and show the form so that users can enter their changes. When the form
is submitted, a request is sent to the server to update the description and the changes are shown to the user. 
The new description of the project will be shown to other clients that are viewing the same project (at a 
synchronisation point, see ProjectScene's `synchronise()`).

In the case that "description box below the project name" actually meant a description box for the current error,
then that functionality is already handled by the "error-container" div and the TodoProject description can be 
considered an extra extension. 

## Hide All 'done' Items

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/js/src/main/scala/be/kuleuven/proman/scenes/ProjectScene.scala

### Textual Explanation of the Solution
The html code for the "Finished todos" list contains a button with the 'todo-list-toggle' class. 
Clicking this button will result in the hiding or showing of finished todos. The done items are hidden by simply hiding 
the table that contains them. The onclick event handler for this button is set in the `setupTodoListTable(div: Div)` 
function. 

## Render on the Server
Render as much as you can on the server, e.g., your initial project list should be rendered on the server.

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
<...>
### Textual Explanation of the Solution
<...>

## Kanban Board 
Instead of having only one todo-list, allow multiple lists that each have a title. This mimics a ‘kanban board’. 
Items that are marked as ‘todo’ go to a common area below all lists.

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/shared/src/main/scala/be/kuleuven/proman/models/TodoEntry.scala
* src/shared/src/main/scala/be/kuleuven/proman/models/TodoList.scala
* src/jvm/src/main/scala/be/kuleuven/proman/repositories/TodoListRepository.scala
* src/jvm/src/main/scala/be/kuleuven/proman/controllers/TodoListsController.scala
* src/js/src/main/scala/be/kuleuven/proman/scenes/ProjectScene.scala

### Textual Explanation of the Solution
The TodoList class is the model that represents a list of todo entries. It has a name and is linked to a project.   
The TodoEntry class represents an actual todo entered by a user and is linked to a TodoList.  
The TodoListController is used to handle client requests that are related to TodoLists (e.g. update a TodoList).  
The TodoListRepository implements all functionality related to storing/updating data about TodoLists.  

The ProjectScene contains functions that display/create/update/synchronise TodoLists. TodoList names can be edited by 
users. Multi-user support is also included here: TodoLists get synchronised so that new lists are added and existing 
lists' names are updated for  

The lists 

When creating new todo entry (in the ProjectScene), a list must be selected for the entry to be placed in. If this is
not done, an error is shown in the alert div.


## Persistence
  
### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
<...>
### Textual Explanation of the Solution
<...>

## Search Bar 
  
### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/js/src/main/scala/be/kuleuven/proman/scenes/ProjectScene.scala
* src/js/src/main/scala/be/kuleuven/proman/scenes/StartScene.scala
### Textual Explanation of the Solution
Both the ProjectScene and the StartScene contain an input at the top of the page that serves as the search bar. An 
'onkeyup' event handler is set on these inputs to call the `filterTodoProjects(filter_text: String)` (StartScene) and 
`filterTodoEntries(filter_text: String)` (ProjectScene) functions with the current input. These functions search for elements in the document that represent 
projects/entries and hide them if their names/texts do not contain the current input string. 


## Hide lists

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/shared/src/main/scala/be/kuleuven/proman/models/TodoList.scala

### Textual Explanation of the Feature
This extension combines the "Hide All 'done' Items" and "Kanban Board" extensions. The goal is to be able to hide any 
of the lists on the 'kanban board'.
### Textual Explanation of the Solution
Every list on the 'kanban board' contains a button with the class 'todo-list-toggle' (see `singleTemplate` method in 
`TodoListTemplate`). Clicking this button will toggle the visibility of the table that contains the todos for
the list.



## Allow for files to be served through the browser

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/shared/src/main/scala/be/kuleuven/proman/models/TodoList.scala

### Textual Explanation of the Feature
This extension combines the "Hide All 'done' Items" and "Kanban Board" extensions. The goal is to be able to hide any 
of the lists on the 'kanban board'.
### Textual Explanation of the Solution
Every list on the 'kanban board' contains a button with the class 'todo-list-toggle' (see `singleTemplate` method in 
`TodoListTemplate`). Clicking this button will toggle the visibility of the table that contains the todos for
the list.