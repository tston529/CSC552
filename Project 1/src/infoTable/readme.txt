README.TXT
Tyler Stoney
Sept. 16. 2018

Contents
=========
The program is in a package labeled 'infoTable'
containing the following files:
- Controller.java
- HealthData.java
- Info.java
- healthTable.fxml

A separate package is submitted for compilation purposes,
this is the com.google.gson package; Licensed under the
Apache 2.0 License, which is available at
http://www.apache.org/licenses/LICENSE-2.0

Compiling/Running
=================
From my package's parent directory, run:
javac infoTable/*.java && java infoTable.Info

to both compile and run the program. The ampersands
should be replaced with a semicolon if on PC, while 
using Powershell, if memory serves.

To just run the program post-compilation, the main 
function resides in the Info class, so run that however
you choose (java infoTable.java)

Design Decisions
================
Layout:
------
First and foremost, the elephant in the room: the layout
was designed in a designer application, which generates an
xml file with the layout in it, but the file and all
components are native to Java so they can be compiled
without any external dependencies. To test this, I compiled
on ACAD, which worked without any hitches, as I had assumed.

JavaFX is the package used for the interface; and I had used
it extensively in OOP, so I gravitated towards it rather than
Swing or something else; plus its table abilities are quite 
handy for a project like this (see 'FAQ').
As for the layout, you had wanted us NOT to use FlowLayout, 
and I am pleased to say that this absolutely does not use
FlowLayout! It uses a "Split Pane Layout," native to JavaFX,
and is implicitely created upon the program's launch, since
all things window/gui are read in through the xml file. It was
the first thing I thought to do, given the two-pronged nature
of this project - the upper pane has the table of results,
while the bottom has the options for the user to alter the
query.

Interactables:
-------------
My dataset has few column headers, and even fewer which change.
The data was (from what I had seen personally) gathered in 2015
so the year rarely, if at all, changes, rendering that column 
useless for queries.  The ID was never given explanation
for its patterning, and is a unique identifier, so it too
is mostly useless for queries. 

The real meat and potatoes comes from the column labeled
"variable," and they couldn't have named it better if they
tried (sarcasm does not translate well to paper, unfortunately).
This column contains a  description of the numerical data's 
value, acting as a - topic - identifier for the row,
e.g. 'air_pollution_particulate_matter_value.' Since the more
specific topics are wide and varied, I opted for the user
(at the moment, at least) to choose a broad topic to bring
down all rows pertaining to that topic.

In future iterations of this, I hope to broaden my selectable
topics to narrow down data returned in a query, but as this
seems to be a proof-of-concept program, I let that slide.

FAQ:
---
Q) Some of the functions seem redundant!
A) Not a question, but yes, some functions are redundant for the
	sake of the rubric.  My data source provides an API endpoint
	to grab data through a query, and returns the result in 
	JSON format.  Using gson, I can serialize the data and slap 
	them into lists to populate the table in one fell swoop, 
	effectively saving the CSV read/write steps. I'm just following
	orders, here.
Q) Why make a separate class for HealthData?
A) JavaFX has a neat automagic way to populate its tables; by 
	assigning each column a unique header, and by creating
	Classes which have a public accessor method containing the
	name of that column header, that table can be assigned
	a list of this custom object and it will fill the table 
	columns automagically (e.g. a column headed "name" will be
	populated from a list of class containing a public method
	"getName()" ).
