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
* 04h00: WIP: Multiple lists per project extension 
* 02h30: TodoList functionality updates:
    * New lists can be created.
    * Lists can be updated (+ multi-user).
    * Lists can be hidden.
    * Lists are sorted alphabetically.
    * Added list_id select in create_entry form.
    * Updated ProjectScene input forms in view. 
* 01h50: 
    * Multiple lists extension + search extension finished
    * New ProjectLists now get inserted in the correct alphabetical order in the "list_id" select.
* 01h00: Filled in extensions file + minor updates to code
* 02h00: Wasted another ton of time to try to get the most basic functionality of sending a custom json object and 
         parsing it on the server to work. Gave up and just left the data in the URL. God damn I fucking hate http4s. 
         Is it that hard to just provide request.get("parameter") method?
* 02h00: Updates to synchronisation
    * Moved synchronisation functions to SynchronisationController and replaced multiple requests by one request on ProjectScene.
    * Updated synchronisation to work with timestamps instead of incrementing state variables.
    * Fixed mistake in synchronisation of projects when descriptions are updated. 
* 02h15: 
    * Server side rendering of initial list 
    * Clean up project code / remove unnecessary code / update extensions file text
* 00h30: Finished extensions.md file. 
---
Total: **54h50**