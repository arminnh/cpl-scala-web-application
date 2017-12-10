Lists in the ProjectScene are sorted by name

**Synchronisation state improvement:**
* todo_changes(project_id, list_id, entry_id, timestamp)
* Views synchronize on timestamp
* Example query for ProjectScene: select * from todo_entries where id in (select entry_id from todo_changes where timestamp > ?) 

## TODO
* extension: Render as much as you can on the server (e.g. initial project list should be rendered on the server): Move some html bits to a new shared class. e.g. lazy val start_scene_title = div(h1(...)).render.innerHTML
add these things in the response in the controllers and replace the html bits in the scene js code.

## Timesheet
* 03h20: First setup, learn about scala project structure. Created first 2 models and 2 controllers. Created root page that shows the projects in a list.
* 06h00: TODO encoders/decoders. First attempt at storing new projects. Updated homepage. Updated routes. Suffered through worthless documentation and non existing forum posts. Trying to figure out the worthless "The request body was malformed." error message.
* 03h30: New projects can now be created and assets can be served via http
* 01h10: Improved ScalaJS code and Repository code
* 02h30: New project form input validation + error showing/hiding. Projects successfully get loaded and written to page when clicked.
* 06h15: Updates to TODOEntry creating/loading/showing:
    * First version of project todos loading and showing in tables.
    * New todos can be created (instant adding in table needs to be improved).
    * Placed edit/finished/not finished buttons for todo entries.
    * Fixed TODOEntry date formatting (manual solution as java.time and java.util.time wouldn't work).
* 03h40: Created scenes classes and updated scenes:
    * Split JS/ProManApp class into proman (util functions) and scenes (view functions) packages.
    * Extracted base HTML to JVM/ProManApp.
    * Projects index page is now loaded with JS (StartScene).
    * ProjectScene now contains a button to go back to the StartScene (project index).
* 01h45: Todo entries can now be updated. Removed Date class from TODOEntry entirely and used System.currentTimeMillis().
* 03h00: Futher improved ProjectScene functionality. Only need to fix 2 small things. Updated code to be more prepared for multi-user functionality.
* 00h50: Finished ProjectScene functionality & feel. 
* 03h00: First multi-user functionality for StartScene
* 01h20: Finished and tweaker multi-user functionality for both scenes. Added documentation in scenes.
* 02h10: Extensions: Project description (with multi-user) & hide finished todos 
* 00h15: Fixed event.scrElement incompatability with firefox
* 00h30: TodoList functionality 
---
Total: **37h55**

1510->1710

