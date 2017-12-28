# Multi-user functionality

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/js/src/main/scala/be/kuleuven/proman/scenes/ProjectScene.scala
* src/js/src/main/scala/be/kuleuven/proman/scenes/StartScene.scala
* src/jvm/src/main/scala/be/kuleuven/proman/controllers/SynchronisationController.scala
* src/jvm/src/main/scala/be/kuleuven/proman/repositories/TodoEntryRepository.scala
* src/jvm/src/main/scala/be/kuleuven/proman/repositories/TodoListRepository.scala
* src/jvm/src/main/scala/be/kuleuven/proman/repositories/TodoProjectRepository.scala

### Textual Explanation of the Feature
This is not an extension, but an explanation of how the multi-user functionality was achieved in this project.
### Textual Explanation of the Solution
Both ProjectScene and StartScene contain a `synchronise()` function, which makes sure the client is up to date with the state on the server. The functionality is achieved as follows. Every Repository contains a `changes` HashMap. These maps store (timestamp -> id) pairs that represent which resource has been created/updated at which timestamp. ProjectScene and StartScene each contain a `synchronisation_timestamp` member which is initialised with 0. At each call of `synchronise()`, the `synchronisation_timestamp` is sent to a route which calls a function in the SynchronisationController. The SynchronisationController fetches the correct data from the repositories and returns this in a response, along with a new timestamp to be used in future `synchronise()` calls. 

This method was implemented in this way in order to minimize the amount data sent between clients and the server. An improvement on this implementation would be to make the server return objects that specify which changes need to be carried out client-side, instead of returning whole objects that have been updated since a timestamp.  


# Extensions

Fill in all sections on each extension. I primarily want to know if you finished
the extension, which files have something to do with your implementation and a
description of how you implemented the feature.

## Red Alert and Description Box
  
### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/js/src/main/scala/be/kuleuven/proman/proman.scala
* src/js/src/main/scala/be/kuleuven/proman/scenes/ProjectScene.scala
* src/js/src/main/scala/be/kuleuven/proman/scenes/StartScene.scala
* src/jvm/src/main/scala/be/kuleuven/proman/controllers/TodoProjectsController.scala
* src/shared/src/main/scala/be/kuleuven/proman/models/TodoProject.scala

### Textual Explanation of the Solution
The base html of the application, which can be found in TodoProjectsController's `index` function, contains a div with id "error-container". This div is used to display all errors and uses Bootstrap's alert class for styling. The proman package object contains `showError(error: String)`, which makes the alert div visible and sets the error text, and `hideError()`, which hides the alert div. These two functions are used in several places in ProjectScene and StartScene.

I interpreted the "description box below the project name" part of this extension as projects having descriptions. In order to achieve this functionality, the TodoProject class contains a 'description' field. On the project scene, the description of the project is displayed under the project title.  
A hidden form is added as well so that changes can be made to descriptions. Clicking the button that shows "description" next to a pencil icon will hide the description and show the form so that users can enter their changes. When the form is submitted, a request is sent to the server to update the description and the changes are shown to the user. This feature is implemented with multi-user functionality as well. 

In the case that "description box below the project name" actually meant a description box for the current error, then that functionality is handled by the "error-container" div and the TodoProject description can be considered to be an extra extension. 

## Hide All 'done' Items

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/js/src/main/scala/be/kuleuven/proman/scenes/ProjectScene.scala

### Textual Explanation of the Solution
The html code for the "Finished todos" list contains a button with the 'todo-list-toggle' class.  Clicking this button will result in the hiding or showing of finished todos. The done items are hidden by simply hiding the table that contains them. The onclick event handler for this button is set in the `setupTodoListTable(div: Div)` function. 

## Render on the Server

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/js/src/main/scala/be/kuleuven/proman/ProManApp.scala
* src/js/src/main/scala/be/kuleuven/proman/scenes/StartScene.scala
* src/jvm/src/main/scala/be/kuleuven/proman/controllers/TodoProjectsController.scala

### Textual Explanation of the Solution
TodoProjectsController's `index` method returns the base HTML of the project. For this extension, the base HTML has been extended with the HTML for the StartScene. The current list of TodoProjects has been added to the div with id project-container.  
The first time StartScene loads, its `setupHTML` will not edit any HTML, as everything has already been rendered on the server. Future setups of StartScene will cause `setupHTML` to set up all necessary HTML for the StartScene.  

## Kanban Board 

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/js/src/main/scala/be/kuleuven/proman/scenes/ProjectScene.scala
* src/jvm/src/main/scala/be/kuleuven/proman/repositories/TodoListRepository.scala
* src/jvm/src/main/scala/be/kuleuven/proman/controllers/TodoListsController.scala
* src/shared/src/main/scala/be/kuleuven/proman/models/TodoEntry.scala
* src/shared/src/main/scala/be/kuleuven/proman/models/TodoList.scala

### Textual Explanation of the Solution
The TodoList class is the model that represents a list of todo entries. It has a name and is linked to a TodoProject.   
The TodoEntry class represents a todo entered by a user and is linked to a TodoList.  
The TodoListController handles client requests that are related to TodoLists (store or update a TodoList).  
The TodoListRepository handles all functionality related to storing/updating data about TodoLists.  

The ProjectScene contains functions that display/create/update/synchronise TodoLists. TodoList names can be edited by  users. Multi-user functionality is also present here: TodoLists get synchronised so that new lists are added and existing lists' names are updated.    

When creating new todo entry (ProjectScene's `submitNewTodo(form: Form)`), a list must be selected for the entry to be placed in. If this is not done, an error is shown in the alert div.

Items that users mark as "finished" (by clicking on the green check mark) will move to the "Finished todos" list, which is a list in every TodoProject. The "Finished todos" list cannot be edited and will always be at the bottom of the page. All the other lists are sorted by name in the ProjectScene.

## Persistence
  
### Status
~~Complete~~ | ~~In progress~~ | **Did not start**
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
Both the ProjectScene and the StartScene contain an input at the top of the page that serves as the search bar. An 'onkeyup' event handler is set on these inputs to call the `filterTodoProjects(filter_text: String)` (StartScene) and `filterTodoEntries(filter_text: String)` (ProjectScene) functions with the current input. These functions search for elements in the document that represent projects/entries and hide them if their names/texts do not contain the current input string. 

## Hide lists

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/js/src/main/scala/be/kuleuven/proman/scenes/ProjectScene.scala
* src/shared/src/main/scala/be/kuleuven/proman/models/TodoList.scala

### Textual Explanation of the Feature
This extension combines the "Hide All 'done' Items" and "Kanban Board" extensions. The goal is to be able to hide any 
of the lists on the 'kanban board'.
### Textual Explanation of the Solution
Every list on the 'kanban board' contains a button with the class 'todo-list-toggle' (see `singleTemplate` method in `TodoListTemplate`). Clicking this button will toggle the visibility of the table that contains the todos for the list. The event handler for this is set in ProjectScene's `setupTodoListTable(div: Div)`.

## Serving static files

### Status
**Complete** | ~~In progress~~ | ~~Did not start~~
### Source Files
* src/jvm/src/main/scala/be/kuleuven/proman/ProManApp.scala

### Textual Explanation of the Feature
Allow for static files such as images, CSS files, and JavaScript files to be served through the browser.
### Textual Explanation of the Solution
Files in /src/public/ can be served through the browser. In ProManApp, routes that start with "/public/" are handled by the `staticFile(filename: Strong, request: Request)` function. This function tries to return the file with the given relative filename starting from src/. If the file does not exist, a NotFound response is returned.