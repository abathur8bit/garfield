/* *****************************************************************************
 * Copyright 2018 Lee Patterson <https://github.com/abathur8bit>
 *
 * You may use and modify at will. Please credit me in the source.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ******************************************************************************/

import com.axorion.NConsole;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * File viewer command line application. Like less, but you can do a few other things to make viewing and searching
 * files easier.
 */
public class Garfield {
    private static final int TIMEOUT_DELAY = 100;
    private static final int TIMEOUT_BLOCK = -1;

    private static final int DIRECTION_FORWARD = 1;
    private static final int DIRECTION_REVERSE = -1;
    private static final int LINE_FOUND_FLAG=2;
    private static final int LINE_BOOKMARKED_FLAG=4;

    private static final int CURRENT_LINE_PAIR = 1;
    private static final int STATUS_BAR_PAIR = 2;
    private static final int BOOKMARK_PAIR = 3;
    private static final int MESSAGE_PAIR = 4;
    private static final int FOLLOW_PAIR = 5;

    private static final int KEY_LEFT = '[';
    private static final int KEY_RIGHT = ']';
    private static final int KEY_UP = 'k';
    private static final int KEY_DOWN = 'j';
    private static final int KEY_NPAGE = 'J';
    private static final int KEY_PPAGE = 'K';
    private static final int KEY_HOME = '<';
    private static final int KEY_END = '>';
    private static final int KEY_FOLLOW = 'f';
    private static final int KEY_SHOW_LINE_NUMBERS = 'o';
    private static final int KEY_BOOKMARK_SET = 'm';
    private static final int KEY_BOOKMARK_NEXT = 'b';
    private static final int KEY_BOOKMARK_PREV = 'M';
    private static final int KEY_QUIT = 'q';

    private final NConsole console;
    private final String filename;
    private final File currentFile;
    private boolean running = true;
    private final ArrayList<String> fileContents = new ArrayList<>();
    private final LineFlags lineFlags = new LineFlags();
    private int lineScreen;
    private int linesInFile;
    private long fileSizeBytes;
    private int lineOffset;
    private int screenHeight,screenWidth;
    private boolean showLineNumbers;
    private int lineNumDigitCount;
    private Date lastLoaded;
    private boolean following = false;


    private static void usage() {
        System.out.println("Garfield Log Viewer");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        if(args.length < 1) {
            usage();
        }

        Garfield app = new Garfield(args[0]);
        app.loadFile();
        app.view();
    }

    /**
     * Constructs the viewer, initializes the console, but doesn't load the file.
     * @param filename Filename that will e loaded.
     */

    public Garfield(String filename) {
        this.filename = filename;
        this.currentFile = new File(filename);
        console = new NConsole();
        console.initscr();
        console.initPair(CURRENT_LINE_PAIR,NConsole.COLOR_BLACK,NConsole.COLOR_WHITE);
        console.initPair(STATUS_BAR_PAIR,NConsole.COLOR_YELLOW,NConsole.COLOR_BLUE);
        console.initPair(BOOKMARK_PAIR,NConsole.COLOR_WHITE, NConsole.COLOR_CYAN);
        console.initPair(MESSAGE_PAIR,NConsole.COLOR_WHITE, NConsole.COLOR_RED);
        console.initPair(FOLLOW_PAIR,NConsole.COLOR_BLACK, NConsole.COLOR_CYAN);
//        showSplash();
    }

    public void showSplash() {
        console.clear();
        console.printCenterX(console.getHeight()/2,"Garfield the Log Viewer");
        console.getch();
    }

    /** Our main loop. Displays everything on the screen, including the file and status bar. */
    @SuppressWarnings("WeakerAccess")
    public void view() throws IOException {
        console.timeout(TIMEOUT_DELAY);
        console.updateSize();
        screenWidth = console.getWidth();
        screenHeight = console.getHeight();
        console.clear();
        while(running) {
            updateDisplay();

            int ch = console.getch();

            if(console.updateSize()) {
                screenWidth = console.getWidth();
                screenHeight = console.getHeight();
                console.clear();
                if(following) {
                    //make sure the last line is selected and in view
                    end();
                }
            }

            if(following && currentFile.length() != fileSizeBytes) {
                if(currentFile.length() < fileSizeBytes) {
                    //if the file has gotten smaller, then the file has been reset
                    loadFile();
                    lineFlags.clear();
                    lineScreen = 0;
                    lineOffset = 0;
                    console.clear();
                } else {
                    loadFile();
                    if(following) {
                        end();
                    }
                }
            }
            if(following) {
                //only keys that will not effect the position of the file are valid
                switch(ch) {
                    case KEY_QUIT: running = false; break;
                    case KEY_BOOKMARK_SET: bookmark(currentLineNum()); break;
                    case KEY_SHOW_LINE_NUMBERS: toggleShowLineNumbers(); break;
                    case KEY_FOLLOW: toggleFollowMode(); break;
                }
            } else {
                //all keys are valid
                switch(ch) {
                    case KEY_QUIT: running = false; break;

                    case KEY_UP:    cursorUp(); break;
                    case KEY_DOWN:  cursorDown(); break;
                    case KEY_HOME:  home(); break;
                    case KEY_END:   end(); break;
                    case KEY_NPAGE: pageDown(); break;
                    case KEY_PPAGE: pageUp(); break;

                    case KEY_BOOKMARK_PREV: nextBookmark(DIRECTION_REVERSE); break;
                    case KEY_BOOKMARK_NEXT: nextBookmark(DIRECTION_FORWARD); break;
                    case KEY_BOOKMARK_SET:  bookmark(currentLineNum()); break;

                    case KEY_SHOW_LINE_NUMBERS: toggleShowLineNumbers(); break;
                    case KEY_FOLLOW:            toggleFollowMode(); break;
                }
            }
        }
        console.endwin();
    }

    private void updateDisplay() {
        showFile();
        showStatusBar();
        console.move(screenWidth-1,lineScreen);
        console.refresh();
    }

    /** Load the file entirely into memory. Note that really large files might not be a good idea. */
    @SuppressWarnings("WeakerAccess")
    public void loadFile() throws IOException {
        fileContents.clear();

        lastLoaded = new Date();
        fileSizeBytes = currentFile.length();
        BufferedReader in = new BufferedReader(new FileReader(currentFile));
        String line;
        while((line = in.readLine()) != null) {
            fileContents.add(line);
        }
        in.close();
        linesInFile = fileContents.size();
        lineNumDigitCount = Integer.toString(linesInFile).length();
    }

    /** Displays the lines of the file. */
    private void showFile() {
        console.home();

        int maxx = screenWidth;
        int maxy = getMaxY();

        for(int i = lineOffset, y = 0; i< linesInFile && y<maxy; i++,y++) {
            if(i==currentLineNum())
                showLine(i,true,y,maxx);
            else
                showLine(i,false,y,maxx);
        }
    }

    /**
     * Show the specified line as either selected or not. If line numbers are enabled, the line is formatted as
     *
     *     ###: abc
     *
     * Where `###` is the line number, and `abc` is the string.
     *  @param lineNum The line of the file to show.
     * @param selected true to show as a selected line, false to show as normal.
     * @param y Y screen position to display the line.
     * @param width maximum width to of the line, won't print more then this number of characers.
     */
    private void showLine(int lineNum, boolean selected, int y, int width) {
        final int x=0;
        String row;
        if(showLineNumbers) {
            String format = "%"+lineNumDigitCount+"d: %-"+(width-lineNumDigitCount-2)+"s";
            row = String.format(format,lineNum+1, fileContents.get(lineNum));
        } else {
            row = fileContents.get(lineNum);
        }
        if(row.length() > width) {
            row = row.substring(0,width);
        }
        console.move(x,y);
        int pair;      //line color pair
        if(selected) {
            pair = CURRENT_LINE_PAIR;
            if(following) {
                pair= FOLLOW_PAIR;
            }
            console.attron(pair);
            console.printw(row);
            fillLine(width-row.length(),' ');
            console.attroff(pair);

            if(lineFlags.get(lineNum) != 0) {
                //show first char as flag color
                console.move(x,y);
                pair = setLineColor(lineNum);
                fillLine(1, ' ');
                console.attroff(pair);
            }
        } else {
            pair = setLineColor(lineNum);
            console.printw(row);
            fillLine(width-row.length(),' ');
            console.attroff(pair);
        }

    }

    /**
     * Changes the current color pair depending on the flags for the given line.
     *
     * @param lineNum Line number of the file.
     * @return the pair that was selected, or -1 if no pair.
     */
    private int setLineColor(int lineNum) {
        int pair = -1;
        if (lineFlags.isSet(lineNum,LINE_BOOKMARKED_FLAG)) {
            pair = LINE_BOOKMARKED_FLAG;
        }
        if(pair != -1) {
            console.attron(pair);
        }
        return pair;
    }


    /** Show the status bar at the bottom of the screen. Shows things like current line number and filename. */
    private void showStatusBar() {
        console.attron(STATUS_BAR_PAIR);
        final int currentLine = lineScreen + lineOffset +1;   //when showing the user, first line is 1, not 0.
        console.move(0,getMaxY());
        fillLine(screenWidth,' ');
        console.move(0,getMaxY());
//        console.printw("Type '?' for help | Line: "+currentLine+" of "+linesInFile+" | File: "+filename+" | Updated: "+lastLoaded+" | W:"+screenWidth+" H:"+screenHeight);
        console.printw("Line "+currentLine+" of "+ linesInFile +" | lineOffset "+ lineOffset +" | lineScreen "+ lineScreen +" | W:"+screenWidth+" H:"+screenHeight);
        if(following) {
            console.printw(" | Following");
        }
        console.attroff(STATUS_BAR_PAIR);
    }

    /** Output the specified char, at the specified location.
     *  @param width Number of characters to display.
     * @param ch The character to display.
     */
    @SuppressWarnings("SameParameterValue")
    private void fillLine(int width, char ch) {
        for(int i=0; i<width; i++) {
            console.printw(""+ch);
        }
    }

    /** The maximum displayable coordinate. */
    public int getMaxY() {
        return console.getHeight()-1;
    }

    /** Move the cursor up one line, scrolls if we get to the top of the display and there is more file to display. */
    private void cursorUp() {
        lineScreen--;
        if(lineScreen < 0) {
            scrollUp();
            lineScreen = 0;
        }
    }

    /** Move the cursor down one line, scrolls if we get to the bottom of the display and there is more file to display. */
    private void cursorDown() {
        if(lineScreen+lineOffset+1 < linesInFile) {
            lineScreen++;
            if(lineScreen >= getMaxY()) {
                lineScreen = getMaxY()-1;
                scrollDown();
            }
        }
    }

    /** Move to the first line of the file. */
    void home() {
        lineOffset = 0;
        lineScreen = 0;
    }

    /** Move to the last line of the file. */
    void end() {
        final int visibleLines = getMaxY()-1;
        if(linesInFile <= visibleLines) {
            //we don't need to scroll
            lineOffset = 0;
            lineScreen = linesInFile - 1;
        } else {
            lineOffset = linesInFile - visibleLines - 1;
            lineScreen = getMaxY()-1;
        }
    }

    /** Show the previous page of text. First line will be selected if needed. */
    void pageUp() {
        final int screenHeight = getMaxY();
        lineOffset -=screenHeight-1;
        if(lineOffset < 0) {
            lineOffset = 0;
            lineScreen = 0;
        }
    }

    /** Show the next page of text. Selected line will move to the end of the file if needed. */
    void pageDown() {
        final int screenHeight = getMaxY();
        lineOffset +=screenHeight-1;
        if(lineOffset > linesInFile -screenHeight) {
            lineOffset = linesInFile -screenHeight;
            lineScreen =screenHeight-1;
        }
    }

    /** Toggle the specified lines bookmark flag. */
    private void bookmark(int lineNum) {
        if(lineFlags.isSet(lineNum,LINE_BOOKMARKED_FLAG)) {
            lineFlags.reset(lineNum,LINE_BOOKMARKED_FLAG);
        } else {
            lineFlags.set(lineNum,LINE_BOOKMARKED_FLAG);
        }
    }

    /**
     * Show the next/previous bookmark, depending on the direction specified.
     * @param dir DIRECTION_FORWARD for next, DIRECTION_REVERSE for previous.
     */
    void nextBookmark(int dir) {
        int currentLine = currentLineNum();
        if(currentLine+dir < linesInFile && currentLine+dir >= 0)
            currentLine+=dir;

        if(DIRECTION_FORWARD==dir) {
            for(int i = currentLine; i< linesInFile; i++) {
                if(lineFlags.isSet(i,LINE_BOOKMARKED_FLAG)) {
                    scrollIntoView(i);
                    return;
                }
            }
        } else {
            for(int i=currentLine; i>=0; i--) {
                if(lineFlags.isSet(i,LINE_BOOKMARKED_FLAG)) {
                    scrollIntoView(i);
                    return;
                }
            }
        }
        showMsg("No more bookmarks found");
    }

    /**
     * Scrolls the give filel line number into view. Puts it at the top usually.
     * @param index file line number to display.
     */
    void scrollIntoView(int index)
    {
        int height = getMaxY();
        if(index >= lineOffset && index<= lineOffset +height)
        {
            //the found item is already visible
            lineScreen = index- lineOffset;
        }
        else
        {
            lineOffset = index;
            if(lineOffset >= linesInFile -height)
            {
                lineScreen = index-(linesInFile -height);
                lineOffset = linesInFile -height;
            }
            else
            {
                lineScreen =0;
            }
        }
    }

    /** Scroll one line up. */
    void scrollUp() {
        lineOffset--;
        if(lineOffset <0)
            lineOffset =0;
    }

    /** Scroll one line down. */
    void scrollDown() {
        if(linesInFile >= getMaxY()) {//make sure the file isn't less then a screen full
            lineOffset++;
            if(lineOffset >= linesInFile -getMaxY()) {
                lineOffset = linesInFile - getMaxY();
            }
        }
    }

    /** Returns the current file line number. */
    int currentLineNum() {
        return lineOffset + lineScreen;
    }

    /**
     * Display a message in the status bar area. Force user to press a key to dismiss the message.
     * @param msg The message to display.
     */
    void showMsg(String msg)
    {
        console.move(0,console.getHeight()-1);
        console.attron(MESSAGE_PAIR);
        fillLine(screenWidth,' ');
        console.printCenterX(getMaxY(),msg+" - PRESS ENTER");
        console.attroff(MESSAGE_PAIR);
        console.refresh();
        console.getch();
    }

    /** Toggle if we show line numbers. */
    void toggleShowLineNumbers() {
        showLineNumbers = !showLineNumbers;
    }

    void toggleFollowMode() {
        if(following) {
            following = false;
        } else {
            following = true;
            end();
        }
    }
}
