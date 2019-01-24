README

![](https://raw.githubusercontent.com/tston529/CSC552/master/Project%203/unixproject3.png?token=AfKaB6FSRhy7VqS5F_GONMquyQR5vJ9mks5cU1U5wA%3D%3D )

(The following is the readme submitted with the project.)

Preface: An Apology
-------------------
This code is garbage.  Its structure and implementations
are a metaphor for its creator and his slow descent into 
madness over the course of 4+ weeks.  Like a work by the
renowned M.C. Escher, the code almost makes sense at first
glance, however, upon closer inspection, its issues and
manic form begin to rear their ugly heads. 

"What does this line do?" "Where does this lead?"

Truthfully, at this point, I don't know. Tread with caution.

SEMAPHORES
----------
Reader's/Writers has finally been implemented.  It's weak
reader's preference, since this was a truly last-minute
implementation.  One more semaphore is used client-side
to simulate a blocking queue, which will either make it 
safer or just give more of a chance to block infinitely. 
Won't know till we test in class.

ISSUES
------
Since I made the mistake of designing an interface before
everything else was done, the stdin buffer appears to be
empty when a message is received, since I didn't debug
that thouroughly.  The command line input buffer is NOT
empty, it just looks that way...

I had attempted an function to close out all clients
if the server closes via CTRL+C, and this works
sporadically.  I can't predict when or how it will fail
but it does most of the time.

The final client on a machine does not detach/delete
shared memory on the machine.

DOXYGEN
-------
http://acad.kutztown.edu/~tston529/CSC552_Docs/P3/html/index.html
