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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final int LOG_FLAG=8;

    private static final int CURRENT_LINE_PAIR = 1;
    private static final int STATUS_BAR_PAIR = 2;
    private static final int BOOKMARK_PAIR = 3;
    private static final int MESSAGE_PAIR = 4;
    private static final int FOLLOW_PAIR = 5;
    private static final int SEARCH_PAIR = 6;
    private static final int LOG_PAIR = 7;

    private static final int KEY_LEFT = '[';
    private static final int KEY_RIGHT = ']';
    private static final int KEY_LEFT_EDGE = '{';
    private static final int KEY_RIGHT_EDGE = '}';
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
    private static final int KEY_BOOKMARK_PREV = 'B';
    private static final int KEY_QUIT = 'q';
    private static final int KEY_SEARCH = '/';
    private static final int KEY_SEARCH_REGEX = '?';
    private static final int KEY_SEARCH_NEXT = 'n';
    private static final int KEY_SEARCH_PREV = 'N';
    private static final int KEY_ENTER = 10;
    private static final int KEY_BACKSPACE = 127;
    private static final int KEY_RELOAD = 'r';
    private static final int KEY_GOTO = 'g';
    private static final int KEY_IGNORE_CASE = 'i';

    private final NConsole console;
    private final String filename;
    private final File currentFile;
    private boolean running = true;
    private final ArrayList<String> fileContents = new ArrayList<>();
    private final LineFlags lineFlags = new LineFlags();
    private int lineScreen;
    private int linesInFile;
    private long fileSizeBytes;
    private int fileMaxLineLength;
    private int lineOffset;
    private int horzOffset;
    private int screenHeight,screenWidth;
    private boolean showLineNumbers;
    private int lineNumDigitCount;
    private Date lastLoaded;
    private boolean following = false;
    private String query;
    private boolean queryWasRegex = false;
    private boolean ignoreCase = true;
    private boolean logBlocks = false;      //show log blocks based on having a date

    private static void usage() {
        System.out.println("Garfield Log Viewer");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        if(args.length < 1) {
            usage();
        }

        if(args[0].equals("makefile")) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
            int num = Integer.parseInt(args[1]);
            for(int i=0; i<num; i++) {
                System.out.println(format.format(new Date()));
            }
            System.exit(0);
        }
        File f = new File(args[0]);
        if(f.exists()) {
            Garfield app = new Garfield(args[0]);
            app.loadFile();
            app.view();
        } else {
            System.out.println("File not found");
        }
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
        console.initPair(BOOKMARK_PAIR,NConsole.COLOR_WHITE, NConsole.COLOR_RED);
        console.initPair(MESSAGE_PAIR,NConsole.COLOR_WHITE, NConsole.COLOR_RED);
        console.initPair(FOLLOW_PAIR,NConsole.COLOR_BLACK, NConsole.COLOR_CYAN);
        console.initPair(SEARCH_PAIR,NConsole.COLOR_BLACK,NConsole.COLOR_YELLOW);
        console.initPair(LOG_PAIR,NConsole.COLOR_BLACK,NConsole.COLOR_BLUE);
//        showSplash();
    }

    public void showSplash() {
        console.clear();
        console.printCenterX(console.getHeight()/2,"Garfield the Log Viewer");
        console.getch();
    }

    /**
     * Our main loop. Displays everything on the screen, including the file and status bar.
     *
     * Key sequence read when you press the following buttons:
     *   SEQUENCE       KEY
     *   27,91,65       up
     *   27,91,66       down
     *   27,91,68       left
     *   27,91,67       right
     *   27,91,53,126   pg up
     *   27,91,54,126   pg down
     */
    @SuppressWarnings("WeakerAccess")
    public void view() throws IOException {
        console.timeout(TIMEOUT_DELAY);
        console.updateSize();
        screenWidth = console.getWidth();
        screenHeight = console.getHeight();
        console.clear();
        boolean escaped = false;    //ch was escaped
        boolean canProcessKey = true;
        while(running) {
            updateDisplay();
            int ch = console.getch();
            if(ch == 27) {
                escaped = true;
                canProcessKey = false;
            } else if(ch == 91) {
                escaped = true;
                canProcessKey = false;
            } else if(ch == 126) {
                escaped = false;
                canProcessKey = true;
            } else if(escaped) {
                if(ch == 65) {
                    ch = KEY_UP;
                    canProcessKey = true;
                    escaped = false;
                } else if(ch == 66) {
                    ch = KEY_DOWN;
                    canProcessKey = true;
                    escaped = false;
                } else if(ch == 68) {
                    ch = KEY_LEFT;
                    canProcessKey = true;
                    escaped = false;
                } else if(ch == 67) {
                    ch = KEY_RIGHT;
                    canProcessKey = true;
                    escaped = false;
                } else if(ch == 53) {
                    ch = KEY_PPAGE;
                    canProcessKey = true;
                    escaped = false;
                } else if(ch == 54) {
                    ch = KEY_NPAGE;
                    canProcessKey = true;
                    escaped = false;
                }
            }
            updateWindowSize();
            checkFileChanged();
            if(canProcessKey) {
                processKey(ch);
            }
        }
        console.endwin();
    }

    /**
     * Checks if the window size has changed. If so, clear the screen so we don't have artifacts. If we ar following
     * then we need to position the cursor at the end of the file.
     */
    private void updateWindowSize() {
        if(console.updateSize()) {
            screenWidth = console.getWidth();
            screenHeight = console.getHeight();
            console.clear();
            if(following) {
                //make sure the last line is selected and in view
                end();
            }
        }
    }

    private void checkFileChanged() throws IOException {
        if(following && currentFile.length() != fileSizeBytes) {
            reloadFile();
        }
    }

    private void processKey(int ch) throws IOException {
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
                case KEY_LEFT:  cursorLeft(false); break;
                case KEY_RIGHT: cursorRight(false); break;
                case KEY_LEFT_EDGE:  cursorLeft(true); break;
                case KEY_RIGHT_EDGE: cursorRight(true); break;
                case KEY_HOME:  home(); break;
                case KEY_END:   end(); break;
                case KEY_NPAGE: pageDown(); break;
                case KEY_PPAGE: pageUp(); break;

                case KEY_BOOKMARK_PREV: nextBookmark(DIRECTION_REVERSE); break;
                case KEY_BOOKMARK_NEXT: nextBookmark(DIRECTION_FORWARD); break;
                case KEY_BOOKMARK_SET:  bookmark(currentLineNum()); break;

                case KEY_SHOW_LINE_NUMBERS: toggleShowLineNumbers(); break;
                case KEY_FOLLOW:            toggleFollowMode(); break;

                case KEY_SEARCH:            search(false); break;
                case KEY_SEARCH_REGEX:      search(true); break;
                case KEY_SEARCH_NEXT:       searchAgain(DIRECTION_FORWARD); break;
                case KEY_SEARCH_PREV:       searchAgain(DIRECTION_REVERSE); break;

                case KEY_RELOAD:            refresh(true); break;
                case KEY_GOTO:              gotoLine(); break;
                case KEY_IGNORE_CASE:       toggleIgnoreCase(); break;
            }
        }
    }

    private void refresh(boolean reloadFile) throws IOException {
        console.clear();
        if(reloadFile) {
            reloadFile();
        }
    }

    private void reloadFile() throws IOException {
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
                searchSetFlags(query,queryWasRegex);
                end();
            }
        }
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
            final int lineLength = line.length();
            if(lineLength > fileMaxLineLength) {
                fileMaxLineLength = lineLength;     //keep track of the longest line
            }
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
     *
     * @param lineNum The line of the file to show.
     * @param selected true to show as a selected line, false to show as normal.
     * @param y Y screen position to display the line.
     * @param width maximum width to of the line, won't print more then this number of characers.
     */
    private void showLine(int lineNum, boolean selected, int y, int width) {
        final int x=0;
        String lineFormat = "%-"+(width-lineNumDigitCount-2)+"s";
        StringBuilder row = new StringBuilder();
        String selectedLine = fileContents.get(lineNum);

        if(showLineNumbers) {
            String numberFormat = "%"+lineNumDigitCount+"d: ";
            row.append(String.format(numberFormat,lineNum+1));
            if(horzOffset < selectedLine.length()) {
                row.append(String.format(lineFormat,selectedLine.substring(horzOffset)));
            }
        } else {
            if(horzOffset < selectedLine.length()) {
                row.append(String.format(lineFormat,selectedLine.substring(horzOffset)));
            }
        }

        console.move(x,y);
        int pair;      //line color pair
        if(selected) {
            pair = CURRENT_LINE_PAIR;
            if(following) {
                pair= FOLLOW_PAIR;
            }
            console.attron(pair);
            console.printw(row.toString());
            fillLine(width-row.length(),' ');
            console.attroff(pair);

            if(lineFlags.get(lineNum) != 0) {
                //show first char as flag color
                console.move(x,y);
                pair = setLineColor(lineNum);
                if(row.length()>0) {
                    fillLine(1,row.charAt(0));
                } else {
                    fillLine(1,' ');
                }
                console.attroff(pair);
            }
        } else {
            pair = setLineColor(lineNum);
            console.printw(row.toString());
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
            pair = BOOKMARK_PAIR;
        } else if(lineFlags.isSet(lineNum,LINE_FOUND_FLAG)) {
            pair = SEARCH_PAIR;
        }

        if(pair != -1) {
            console.attron(pair);
        }
        return pair;
    }


    /** Show the status bar at the bottom of the screen. Shows things like current line number and filename.
     * Help 'h' | 1:1/32 | a.txt | C R F | Updated: Mon Dec 24 16:36:04 EST 2018 | W:95 H:27
     */
    private void showStatusBar() {
        final int activePair = MESSAGE_PAIR;
        console.attron(STATUS_BAR_PAIR);
        final int currentLine = lineScreen + lineOffset + 1;   //when showing the user, first line is 1, not 0.
        console.move(0,getMaxY());
        fillLine(screenWidth,' ');
        console.move(0,getMaxY());

        console.printw("Help 'h' | "+(horzOffset+1)+":"+currentLine+"/"+linesInFile+" | ");
        if(ignoreCase) {
            console.attron(activePair);
        }
        console.printw("C");
        if(ignoreCase) {
            console.attron(STATUS_BAR_PAIR);
        }
        if(following) {
            console.attron(activePair);
        }
        console.printw("F");
        if(following) {
            console.attron(STATUS_BAR_PAIR);
        }

        console.printw(" | "+filename);

//        console.printw("Line "+currentLine+" of "+ linesInFile +" | lineOffset "+ lineOffset +" | lineScreen "+ lineScreen +" | W:"+screenWidth+" H:"+screenHeight);
        console.attroff(STATUS_BAR_PAIR);
    }

    /**
     * Output the specified char, at the specified location.
     *
     * @param width Number of characters to display.
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

    /** Scroll left. Text moves to the right. */
    private void cursorLeft(boolean edge) {
        if(edge) {
            horzOffset = 0;
        } else {
            horzOffset--;
            if(horzOffset < 0) {
                horzOffset = 0;
            }
        }
    }

    /** Scroll right. Text moves to the left. */
    private void cursorRight(boolean edge) {
        if(fileMaxLineLength > screenWidth) {
            int digitCount = 0;
            if(showLineNumbers) {
                //take into consideration the line number characters and ": " at the end
                digitCount = lineNumDigitCount+2;
            }
            if(edge) {
                horzOffset = fileMaxLineLength-screenWidth+digitCount;
            } else {
                horzOffset++;
                if(horzOffset > fileMaxLineLength-screenWidth+digitCount) {
                    horzOffset = fileMaxLineLength-screenWidth+digitCount;
                }
            }
        }
    }

    /** Move to the first line of the file. */
    void home() {
        horzOffset = 0;
        lineOffset = 0;
        lineScreen = 0;
    }

    /** Move to the last line of the file. */
    void end() {
        horzOffset = 0;
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
        lineOffset -= screenHeight-1;
        if(lineOffset < 0) {
            lineOffset = 0;
            lineScreen = 0;
        }
    }

    /** Show the next page of text. Selected line will move to the end of the file if needed. */
    void pageDown() {
        final int screenHeight = getMaxY();
        lineOffset +=screenHeight-1;
        if(lineOffset > linesInFile-screenHeight) {
            lineOffset = linesInFile-screenHeight;
            lineScreen = screenHeight-1;
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
            for(int i = currentLine+1; i<linesInFile; i++) {
                if(lineFlags.isSet(i,LINE_BOOKMARKED_FLAG)) {
                    scrollIntoView(i);
                    return;
                }
            }
        } else {
            for(int i=currentLine-1; i>=0; i--) {
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
        if(index >= lineOffset && index <= lineOffset+height)
        {
            //the found item is already visible
            lineScreen = index-lineOffset;
        }
        else
        {
            lineOffset = index;
            if(lineOffset >= linesInFile -height)
            {
                lineScreen = index-(linesInFile-height);
                lineOffset = linesInFile-height;
            }
            else
            {
                lineScreen = 0;
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
        console.timeout(TIMEOUT_BLOCK);
        console.getch();
        console.timeout(TIMEOUT_DELAY);
    }

    /** Toggle if we show line numbers. */
    void toggleShowLineNumbers() {
        showLineNumbers = !showLineNumbers;
        if(!showLineNumbers) {
            horzOffset -= lineNumDigitCount+2;
            if(horzOffset < 0) {
                horzOffset = 0;
            }
        }
    }

    /** Set or turn off follow mode. */
    void toggleFollowMode() {
        if(following) {
            following = false;
        } else {
            following = true;
            end();
        }
    }

    /**
     * Start a search.
     * @param useRegex true if you want to use regular expression, false to use a normal search.
     */
    private void search(boolean useRegex) {
        console.attron(MESSAGE_PAIR);
        final int x=0,y=getMaxY();
        console.move(x,y);
        fillLine(screenWidth,' ');
        console.move(x,y);
        String msg = "Find: ";
        if(useRegex) {
            msg = "Find Regex: ";
        }

        console.refresh();
        query = readLine(msg);
        queryWasRegex = useRegex;
        console.attroff(MESSAGE_PAIR);
        lineFlags.reset(LINE_FOUND_FLAG);   //clear the previous search results
        console.move(x,y);
        console.clrtoeol();
        console.printCenterX(y,"Searching");
        console.refresh();

        if(query.length() > 0) {
            boolean found = searchSetFlags(query,useRegex);
            if(found) {
                ArrayList<Integer> flaggedLines = new ArrayList<>(lineFlags.keySet());
                Collections.sort(flaggedLines);
                if(!scrollMatchedLineIntoView(flaggedLines,DIRECTION_FORWARD)) {
                    showMsg("No more matches.");
                }
            } else {
                showMsg("Not found");
            }
        }
    }

    /**
     * Search for the next/previous match.
     * @param direction 1 to move forward, -1 to go in reverse.
     */
    private void searchAgain(int direction) {
        ArrayList<Integer> flaggedLines = new ArrayList<>(lineFlags.keySet());  //all lines with ANY flag
        Collections.sort(flaggedLines);
        if(!scrollMatchedLineIntoView(flaggedLines,direction)) {
            showMsg("No more matches.");
        }
    }

    /**
     * If we find a matching line in the direction give, scroll that line info view. If we don't, nothing happens.
     *
     * @param list Line flag keys to look through.
     * @param direction 1 to go forward, -1 for reverse.
     * @return true if a match was found, false otherwise.
     */
    private boolean scrollMatchedLineIntoView(List<Integer> list,int direction) {
        final int currentLine = currentLineNum();

        if(direction == DIRECTION_FORWARD) {
            for(final int key : list) {
                if(lineFlags.isSet(key,LINE_FOUND_FLAG)) {  //looking for lines with the found flag on it
                    if(key > currentLine) {
                        scrollIntoView(key);
                        return true;
                    }
                }
            }
        } else {
            for(int i = list.size()-1; i >= 0; i--) {
                final int key = list.get(i);
                if(lineFlags.isSet(key,LINE_FOUND_FLAG)) {  //looking for lines with the found flag on it
                    if(key < currentLine) {
                        scrollIntoView(key);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean searchSetFlags(String query,boolean useRegex) {
        boolean found = false;
        if(query != null && query.length() > 0) {
            for(int i = 0; i < fileContents.size(); i++) {
                String row;
                String qry;
                if(ignoreCase) {
                    row = fileContents.get(i).toLowerCase();
                    qry = query.toLowerCase();
                } else {
                    row = fileContents.get(i);
                    qry = query;
                }
                if(useRegex) {
                    final Pattern pattern;
                    final Matcher m;
                    if(ignoreCase) {
                        pattern = Pattern.compile(qry);
                        m = pattern.matcher(row.toLowerCase());
                    } else {
                        pattern = Pattern.compile(qry);
                        m = pattern.matcher(row);
                    }
                    if(m.find()) {
                        lineFlags.set(i,LINE_FOUND_FLAG);
                        found = true;
                    }
                } else {
                    if(row.contains(qry)) {
                        lineFlags.set(i,LINE_FOUND_FLAG);
                        found = true;
                    }
                }
            }
        }
        return found;
    }

    /** Reads in a line of text, and returns the text string. Assumes you are using the bottom of the screen. */
    private String readLine(String msg) {
        final int x=0,y=getMaxY();
        StringBuilder buff = new StringBuilder();
        int ch;
        console.timeout(TIMEOUT_BLOCK); //wait for keypresses
        console.move(x,y);
        console.printw(msg);
        do {
            ch = console.getch();
            if(ch == KEY_BACKSPACE) {
                if(buff.length()>0) {
                    buff.deleteCharAt(buff.length()-1);
                    console.move(x,y);
                    fillLine(screenWidth,' ');
                    console.move(x,y);
                    console.printw(msg+buff.toString());
                }
            } else if(ch != KEY_ENTER) {
                String letter = String.format("%c",ch);
                buff.append(letter);
                console.printw(letter);
            }
        } while(ch != KEY_ENTER);
        console.timeout(TIMEOUT_DELAY); //back to normal
        return buff.toString();
    }

    /** Asks the user what line to put the cursor on, and scrolls that line into view. */
    private void gotoLine() {
        console.attron(MESSAGE_PAIR);
        final int x=0,y=getMaxY();
        console.move(x,y);
        fillLine(screenWidth,' ');
        console.refresh();
        String lineInput = readLine("Goto line: ");
        if(lineInput.length() > 0) {
            int lineNum;
            try {
                lineNum = Integer.parseInt(lineInput);
            } catch(NumberFormatException e) {
                lineNum = 0;
            }
            if(lineNum < 1 || lineNum > linesInFile) {
                showMsg("Line number out of range");
            } else {
                scrollIntoView(lineNum-1);
            }
        }
        console.attroff(MESSAGE_PAIR);
    }

    private void toggleIgnoreCase() {
        ignoreCase = !ignoreCase;
        lineFlags.reset(LINE_FOUND_FLAG);   //clear the previous search results
        searchSetFlags(query,queryWasRegex);
        console.clear();
    }
}
