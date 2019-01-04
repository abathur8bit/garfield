# Performance
- Very slow redraw performance in Windows. 
	- Scroll via system call


# Functional
- Filter (keystroke '&') to display only matching lines, will need to show something on the status bar to indicate we are filtering
- Multiple file support
- Search shows the matching line *and* highlight the matching text
- Word wrap
- Remember bookmarks in files viewed
- Launch `glov` with `-l` param will start the last session


# Misc
- Build script to build and package jar.


## Log file blocks
This section is for notes on how to break up the blocks of a log file. Regex strings I can use to identify the start of a log entry.

Finds a date in the form of "2018/12/12" from the start of the line. The '/' can be any character. I have see / and - used already.

^\d\d\d\d.\d\d.\d\d

