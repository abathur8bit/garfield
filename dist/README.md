# Install
[Download][glov-download] the distribution zip, then unzip and copy files to your ~/bin directory. See usage for details on running.

You may need to set the execute attribute on the script file **glov**. 

```
chmod 755 glov
``` 

Will do that nicely.

# Usage
To run, type **glov filename** where *filename* is the file you wish to view. 

```
$ glov filename
```

**Press h** while the program is running to view a keymap. 

| Key           | Action                                    |
|-------        |----                                       |
| h             | Help Keymap                               |
| q             | Quit                                      |
| k / Up        | Up                                        |
| j / Down      | Down                                      |
| K / PG Up     | Page Up                                   |
| J / PG Down   | Page Down                                 |
| \[ / Left     | Scroll Left                               |
| \] / Right    | Scroll Right  (right arrow)               |
| {             | Scroll to the first letter of line        |
| }             | Scroll to the last letter of line         |
| f             | Follow                                    |
| r             | Reload                                    |
| m             | Set bookmark                              |
| b             | Next bookmark                             |
| B             | Prev bookmark                             |
| o             | Toggle showing line numbers               |
| /             | Search                                    |
| ?             | Regular expression search                 |
| n             | Search next                               |
| N             | Search previous                           |
| c             | Toggle ignore case (default ignore)       |
| g             | Goto specified line                       |


[glov-download]: http://www.axorion.com/downloads/glov-0.5.zip