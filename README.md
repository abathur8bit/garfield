# GLOV the log viewer
**GLOV** (**G**UI'less **LO**g **V**iewer) is an [open source][glov-src] console based log and text file viewing utility. If you routinely find yourself using **less** and **tail** on log files while you are shh'ed to a remote machine, then glov is a utility that will interest you. Its purpose is to provide a rich set of viewing, searching, and filtering features that are missing in `tail` and `less`. Features like bookmarking, filtering, and intelligently highlighting log blocks make it easier to work with large log files with lots of exception traces. 

Designed out of the need to have a command line log file viewer that would give me ability to view a file, search it, show line numbers, set bookmarks, and monitor it in real time. Something like **less** is great, but if I want to monitor the file, I always found myself using line numbers when I needed to see if the file changed, which is problematic if I don't remember the line number. Using **tail -f** is great for monitoring a file, and if you hit enter a few times, it effectively *marks* the location so you can continue to monitor the file, and know where you marked it. But neither are ideal. Something that will let me bounce around the file, bookmark locations, and monitor is what is really needed.

Although the idea of using something like [splunk] is great in theory, in practice a lot of enterprises don't use it for all their apps, which leaves the developers needing to use some combination of **ssh+less+tail**. Hence the desire for something that combines the best of both tools, but add some additional features.

Garifled is written in Java. Since Java doesn't provide the ability to monitor the keyboard for individual keypresses, and can't control a terminal in *nix or Windows, Garfield utilizes [NConsole] for it's low level console i/o. Under Linix and macOS, this means it uses ncursors. Under windows it uses native windows console API.

**Note:** Windows support isn't in place yet. Plan is to use the same methods as on *nix systems. 

# Usage
$ glov filename

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
| /     | Search                                    |
| ?     | Regular expression search                 |
| n     | Search next                               |
| N     | Search previous                           |
| c     | Toggle ignore case (default ignore)       |
| g     | Goto specified line                       |

Not yet implemented:

| Key   | Action                                    |
|-------|---                                        |
| &     | Display only matching lines               |



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
[regex]: https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html
[glov]: http://axorion.com/glov
[glov-src]: https://github.com/abathur8bit/garfield
[splunk]: https://www.splunk.com
[gas]: http://axorion.com/gms
[8bitblog]: http://www.8bitcoder.com/category/blog/
[axorion]: http://axorion.com
