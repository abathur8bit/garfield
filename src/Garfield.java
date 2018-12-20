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
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * File viewer command line application. Like less, but you can do a few other things to make viewing and searching
 * files easier.
 */
public class Garfield {
    private static final int DIRECTION_FORWARD = 1;
    private static final int DIRECTION_REVERSE = -1;
    private static final int LINE_FOUND_FLAG=2;
    private static final int LINE_BOOKMARKED_FLAG=4;

    private static final int CURRENT_LINE_PAIR = 1;
    private static final int STATUS_BAR_PAIR = 2;
    private static final int BOOKMARK_PAIR = 3;
    private static final int MESSAGE_PAIR = 4;

    private static final int KEY_LEFT = '[';
    private static final int KEY_RIGHT = ']';
    private static final int KEY_UP = 'k';
    private static final int KEY_DOWN = 'j';
    private static final int KEY_NPAGE = 'J';
    private static final int KEY_PPAGE = 'K';
    private static final int KEY_HOME = '<';
    private static final int KEY_END = '>';

    private final NConsole console;
    private final String filename;
    private boolean running = true;
    private final ArrayList<String> fileContents = new ArrayList<>();
    private int[] lineFlags;
    private int selectedLine;
    private int maxLines;
    private int lineIndex;
    private int screenHeight,screenWidth;


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
        console = new NConsole();
        console.initscr();
        console.initPair(CURRENT_LINE_PAIR,NConsole.COLOR_BLACK,NConsole.COLOR_WHITE);
        console.initPair(STATUS_BAR_PAIR,NConsole.COLOR_YELLOW,NConsole.COLOR_BLUE);
        console.initPair(BOOKMARK_PAIR,NConsole.COLOR_WHITE, NConsole.COLOR_CYAN);
        console.initPair(MESSAGE_PAIR,NConsole.COLOR_WHITE, NConsole.COLOR_RED);
//        showSplash();
    }

    public void showSplash() {
        console.clear();
        console.printCenterX(console.getHeight()/2,"Garfield the Log Viewer");
        console.getch();
    }

    /** Our main loop. Displays everything on the screen, including the file and status bar. */
    @SuppressWarnings("WeakerAccess")
    public void view() {
        console.clear();
        screenWidth = console.getWidth();
        screenHeight = console.getHeight();
        while(running) {
            if(console.updateSize()) {
                screenWidth = console.getWidth();
                screenHeight = console.getHeight();
                console.clear();
            }

            showFile();
            showStatusBar();
            console.move(screenWidth-1,selectedLine);
            console.refresh();


            int ch = console.getch();
            switch(ch) {
                case 'q': running = false; break;
                case 'Q': running = false; break;

                case KEY_UP:   cursorUp(); break;
                case KEY_DOWN: cursorDown(); break;

                case KEY_HOME: home(); break;
                case KEY_END: end(); break;

                case KEY_NPAGE: pageDown(); break;
                case KEY_PPAGE: pageUp(); break;

                case 'B': nextBookmark(DIRECTION_REVERSE); break;
                case 'b': nextBookmark(DIRECTION_FORWARD); break;
                case 'm': bookmark(currentLineNum()); break;
            }
        }
        console.endwin();
    }

    /** Load the file entirely into memory. Note that really large files might not be a good idea. */
    @SuppressWarnings("WeakerAccess")
    public void loadFile() throws IOException {
        fileContents.clear();
        selectedLine = 0;  //top of the screen
        lineIndex = 0;    //first line of the file

        BufferedReader in = new BufferedReader(new FileReader(filename));
        String line;
        while((line = in.readLine()) != null) {
            fileContents.add(line);
        }
        maxLines = fileContents.size();
        lineFlags = new int[maxLines];
    }

    /** Displays the lines of the file. */
    private void showFile() {
        console.home();

        int maxx = screenWidth;
        int maxy = getMaxY();

        for(int i=lineIndex,y=0; i<maxLines && y<maxy; i++,y++) {
            if(i==currentLineNum())
                showLine(i,true,0,y,maxx);
            else
                showLine(i,false,0,y,maxx);
        }
    }

    /**
     * Show the specified line as either selected or not.
     *
     * @param lineNum The line of the file to show.
     * @param selected true to show as a selected line, false to show as normal.
     * @param x X screen position to display the line.
     * @param y Y screen position to display the line.
     * @param maxWidth maximum width to of the line, won't print more then this number of characers.
     */
    @SuppressWarnings("SameParameterValue")
    private void showLine(int lineNum, boolean selected, int x, int y, int maxWidth) {
        String row = fileContents.get(lineNum);
        if(row.length() > maxWidth) {
            row = row.substring(0,maxWidth);
        }

        console.move(x,y);
        int pair;      //line color pair
        if(selected) {
            console.attron(CURRENT_LINE_PAIR);
            console.printw(row);
            fillLine(maxWidth-row.length()-1,' ');
            console.attroff(CURRENT_LINE_PAIR);

            if(lineFlags[lineNum] != 0) {
                //show first char as flag color
                console.move(x,y);
                pair = setLineColor(lineNum);
                fillLine(1,' ');
                console.attroff(pair);
            }
        } else {
            pair = setLineColor(lineNum);
            console.printw(row);
            fillLine(maxWidth-row.length()-1,' ');
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
        if ((lineFlags[lineNum] & LINE_BOOKMARKED_FLAG) == LINE_BOOKMARKED_FLAG) {
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
        final int currentLine = selectedLine+lineIndex+1;   //when showing the user, first line is 1, not 0.
        console.move(0,getMaxY());
        fillLine(screenWidth,' ');
        console.move(0,getMaxY());
        console.printw("Type '?' for help | Line: "+currentLine+" of "+maxLines+" | File: "+filename+" | W:"+screenWidth+" H:"+screenHeight);
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
        selectedLine--;
        if(selectedLine<0) {
            scrollUp();
            selectedLine=0;
        }
    }

    /** Move the cursor down one line, scrolls if we get to the bottom of the display and there is more file to display. */
    private void cursorDown() {
        if(selectedLine+lineIndex+1 < maxLines) {
            selectedLine++;
            if(selectedLine>=getMaxY()) {
                selectedLine = getMaxY()-1;
                scrollDown();
            }
        }
    }

    /** Move to the first line of the file. */
    void home() {
        lineIndex=0;
        selectedLine=0;
    }

    /** Move to the last line of the file. */
    void end() {
        if(maxLines<getMaxY()) {
            //we don't need to scroll
            selectedLine = maxLines-1;
        } else {
            lineIndex=maxLines-getMaxY();
            selectedLine=getMaxY()-1;
        }
    }

    /** Show the previous page of text. First line will be selected if needed. */
    void pageUp() {
        final int screenHeight = getMaxY();
        lineIndex-=screenHeight-1;
        if(lineIndex<0) {
            lineIndex=0;
            selectedLine=0;
        }
    }

    /** Show the next page of text. Selected line will move to the end of the file if needed. */
    void pageDown() {
        final int screenHeight = getMaxY();
        lineIndex+=screenHeight-1;
        if(lineIndex>maxLines-screenHeight) {
            lineIndex=maxLines-screenHeight;
            selectedLine=screenHeight-1;
        }
    }

    /** Toggle the specified lines bookmark flag. */
    private void bookmark(int lineNum) {
        if((lineFlags[lineNum] & LINE_BOOKMARKED_FLAG) == LINE_BOOKMARKED_FLAG)
            lineFlags[lineNum] &= 0xFF-LINE_BOOKMARKED_FLAG;
        else
            lineFlags[lineNum] |= LINE_BOOKMARKED_FLAG;
    }

    /**
     * Show the next/previous bookmark, depending on the direction specified.
     * @param dir DIRECTION_FORWARD for next, DIRECTION_REVERSE for previous.
     */
    void nextBookmark(int dir) {
        int currentLine = currentLineNum();
        if(currentLine+dir < maxLines && currentLine+dir >= 0)
            currentLine+=dir;

        if(DIRECTION_FORWARD==dir) {
            for(int i=currentLine; i<maxLines; i++) {
                if((lineFlags[i]&LINE_BOOKMARKED_FLAG) == LINE_BOOKMARKED_FLAG) {
                    scrollIntoView(i);
                    return;
                }
            }
        } else {
            for(int i=currentLine; i>=0; i--) {
                if((lineFlags[i]&LINE_BOOKMARKED_FLAG) == LINE_BOOKMARKED_FLAG) {
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
        if(index >= lineIndex && index<=lineIndex+height)
        {
            //the found item is already visible
            selectedLine = index-lineIndex;
        }
        else
        {
            lineIndex = index;
            if(lineIndex>=maxLines-height)
            {
                selectedLine = index-(maxLines-height);
                lineIndex = maxLines-height;
            }
            else
            {
                selectedLine=0;
            }
        }
    }

    /** Scroll one line up. */
    void scrollUp() {
        lineIndex--;
        if(lineIndex<0)
            lineIndex=0;
    }

    /** Scroll one line down. */
    void scrollDown() {
        if(maxLines >= getMaxY()) {//make sure the file isn't less then a screen full
            lineIndex++;
            if(lineIndex>=maxLines-getMaxY()) {
                lineIndex = maxLines - getMaxY();
            }
        }
    }

    /** Returns the current file line number. */
    int currentLineNum() {
        return lineIndex+selectedLine;
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
}
