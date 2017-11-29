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

---
Total: **26h25**
