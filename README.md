# Garfield the log viewer
Garfield, or garf for short, is a console based log and text file viewer. Its purpose is to provide a rich set of viewing, searching, and filtering features that are missing in `tail` and `less`. Features like bookmarking, filtering, and intelligently highlighting log blocks make it easier to work with large log files with lots of exception traces. 

Why not just use something like splunk? Most of the log files I look at are on a *nix box, and the only access is through an ssh session. Using less and tail is frustrating, as it's hard to see where a log block starts and ends. It's also hard to look at multiple places in the file, or keep track of something you were previously looking at.

So bookmarking lets you jump around the file, filtering lets you toss out crap you are not interested in, all while watching the file in realtime. 

Garifled is written in Java. Since Java doesn't provide the ability to monitor the keyboard for individual keypresses, and can't control a terminal in *nix or Windows, Garfield utilizes [NConsole] for it's low level console i/o. Under Linix and macOS, this means it uses ncursors. Under windows it uses native windows console API.

**Note:** Windows support isn't in place yet. Plan is to use the same methods as on *nix systems. 

# Usage
$ garf filename

Filename is the text file you want to view.


# Keymap
Keys are meant to be a little like `less`. 

| Key   | Action                                    |
|-------|----                                       |
| q     | Quit                                      |
| k     | Up                                        |
| j     | Down                                      |
| K     | Page Up                                   |
| J     | Page Down                                 |
| [     | Left                                      |
| ]     | Right                                     |
| {     | Scroll to the first letter of line        |
| }     | Scroll to the last letter of line         |
| f     | Follow                                    |
| r     | Reload                                    |
| m     | Set bookmark                              |
| b     | Next bookmark                             |
| B     | Prev bookmark                             |
| o     | Toggle showing line numbers               |

Not yet implemented:

| Key   | Action                                    |
|-------|---                                        |
| /     | Search                                    |
| ?     | Regular expression search                 |
| n     | Search next                               |
| N     | Search previous                           |
| &     | Display only matching lines               |
| g     | Goto specified line                       |



# Features explained
Explanation of features.

## Search
Regular search, regex search. After doing a search, you can turn filter mode on to show only matching lines. 


## Follow
Follow the file. Best for monitoring files in real time. Any changes to the file will be shown, and the bottom of the file is always in view. 

You can set **bookmarks** while in follow mode.

Like `tail -f` or `less +F`.


## Refresh
Reloads the file from disk. Position in file, and any bookmarks are preserved.


# TODO
- Filter (&) to display only matching lines, will need to show something on the status bar to indicate we are filtering
- Multiple file support
- Search shows the matching line *and* highlight the matching text
- Word wrap
- Remember bookmarks in files viewed
- Case insensitive searches
- Ability to choose between case and no case in searches
- Launch Garfield with -l param will start the last session
 

## Search
Search history. When you hit the up arrow, it will show previous queries. Down will show the next query. When it goes past the last one, a blank line is shows. 

Line editor. Allows you to edit the query you are working on. You can use the left/right arrows to move around in the query. Backspace and delete buttons will delete charaters.


## Multiple file support
Ability to show and monitor multiple files. Multiple files will be shown as a split screen. Initial versions will handle window resizing automatically, but later revs will allow the user to control the window size.

When **follow** mode is active, all windows are set to follow mode. Later revs will allow the user to choose to activate only active file, or optionally all files.

Could use the number keys to select what file to activate. Then you can have support for 9 files, if you use 0 to close a file.


#JNI
See [nconsole] for Java Native Interface (JNI) library.

# Building/running
Currently tied to [nconsole] project, and I need to fix up the project to make it so someone else can run it.

```
mkdir -p classes
cp ../nconsole/nconsole.jar lib
cp ../nconsole/*.so lib
javac -d classes -cp lib/nconsole.jar src/*
java -Djava.library.path=../nconsole -cp lib/nconsole.jar:classes Garfield README.md 
```


# Log file blocks
This section is for notes on how to break up the blocks of a log file. Regex strings I can use to identify the start of a log entry.

Finds a date in the form of "2018/12/12" from the start of the line. The '/' can be any character. I have see / and - used already.

^\d\d\d\d.\d\d.\d\d


[garfield]: https://github.com/abathur8bit/garfield
[nconsole]: https://github.com/abathur8bit/nconsole