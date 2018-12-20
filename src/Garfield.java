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
import org.omg.CosNaming.NamingContextOperations;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class Garfield {
    static final int DIRECTION_FORWARD = 1;
    static final int DIRECTION_REVERSE = -1;
    static final int LINE_SELECTED_FLAG=1;
    static final int LINE_FOUND_FLAG=2;
    static final int LINE_BOOKMARKED_FLAG=4;

    static final int CURRENT_LINE_PAIR = 1;
    static final int NORMAL_LINE_PAIR = 2;
    static final int STATUS_BAR_PAIR = 3;
    static final int BOOKMARK_PAIR = 4;
    static final int CURRENT_LINE_BOOKMARK_PAIR = 5;

    static final int KEY_LEFT = '[';
    static final int KEY_RIGHT = ']';
    static final int KEY_UP = 'k';
    static final int KEY_DOWN = 'j';
    static final int KEY_NPAGE = 'J';
    static final int KEY_PPAGE = 'K';
    static final int KEY_HOME = '<';
    static final int KEY_END = '>';

    private NConsole console;
    private String filename;
    private boolean running = true;
    private ArrayList<String> fileContents = new ArrayList<String>();
    private int[] lineFlags;
    private boolean screenDirty = true;
    private int selectedLine;
    private int maxLines;
    private int lineIndex;
    private int screenHeight,screenWidth;


    public static void usage() {
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

    public Garfield(String filename) {
        this.filename = filename;
        console = new NConsole();
        console.initscr();
        console.initPair(CURRENT_LINE_PAIR,NConsole.COLOR_BLACK,NConsole.COLOR_WHITE);
        console.initPair(STATUS_BAR_PAIR,NConsole.COLOR_YELLOW,NConsole.COLOR_BLUE);
        console.initPair(BOOKMARK_PAIR,NConsole.COLOR_WHITE, NConsole.COLOR_RED);
        console.initPair(CURRENT_LINE_BOOKMARK_PAIR,NConsole.COLOR_RED, NConsole.COLOR_WHITE);
//        showSplash();
    }

    public void showSplash() {
        console.clear();
        console.printCenterX(console.getHeight()/2,"Garfield the Log Viewer");
        console.getch();
    }

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
            final int maxy = getMaxY();

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

                case 'B': nextBookmark(DIRECTION_REVERSE); break;
                case 'b': nextBookmark(DIRECTION_FORWARD); break;
                case 'm': bookmark(currentLineNum()); break;
            }
        }
        console.endwin();
    }

    /** Load the file entirely into memory. Note that really large files might not be a good idea. */
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

    private void showLine(int lineNum,boolean selected,int x,int y,int maxWidth) {
        String row = fileContents.get(lineNum);
        if(row.length() > maxWidth) {
            row = row.substring(0,maxWidth);
        }

        console.move(x,y);
        int pair = -1;      //line color pair
        if(selected) {
            console.attron(CURRENT_LINE_PAIR);
            console.printw(row);
            fillLine(row.length(),maxWidth-row.length()-1,' ');
            console.attroff(CURRENT_LINE_PAIR);

            if(lineFlags[lineNum] != 0) {
                //show first char as flag color
                console.move(x,y);
                pair = setLineColor(lineNum);
                fillLine(0,1,' ');
                console.attroff(pair);
            }
        } else {
            pair = setLineColor(lineNum);
            console.printw(row);
            fillLine(row.length(),maxWidth-row.length()-1,' ');
            console.attroff(pair);
        }

    }

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



    private void showStatusBar() {
        console.attron(STATUS_BAR_PAIR);
        final int currentLine = selectedLine+lineIndex+1;   //when showing the user, first line is 1, not 0.
        console.move(0,getMaxY());
        fillLine(0,screenWidth,' ');
        console.move(0,getMaxY());
        console.printw("Type '?' for help | Line: "+currentLine+" of "+maxLines+" | File: "+filename+" | W:"+screenWidth+" H:"+screenHeight);
        console.attroff(STATUS_BAR_PAIR);
    }

    private void fillLine(int startx,int width,char ch) {
        for(int i=0; i<width; i++) {
            console.printw(""+ch);
        }
    }

    public void setDirty(boolean dirty) {
        screenDirty = dirty;
    }

    public int getMaxY() {
        return console.getHeight()-1;
    }

    private void cursorUp() {
        selectedLine--;
        if(selectedLine<0) {
            scrollUp();
            selectedLine=0;
        }
    }

    private void cursorDown() {
        if(selectedLine+lineIndex+1 < maxLines) {
            selectedLine++;
            if(selectedLine>=getMaxY()) {
                selectedLine = getMaxY()-1;
                scrollDown();
            }
        }
    }

    void home() {
        lineIndex=0;
        selectedLine=0;
    }

    void end() {
        if(maxLines<getMaxY()) {
            //we don't need to scroll
            selectedLine = maxLines-1;
        } else {
            lineIndex=maxLines-getMaxY();
            selectedLine=getMaxY()-1;
        }
    }

    private void bookmark(int lineNum) {
        if((lineFlags[lineNum] & LINE_BOOKMARKED_FLAG) == LINE_BOOKMARKED_FLAG)
            lineFlags[lineNum] &= 0xFF-LINE_BOOKMARKED_FLAG;
        else
            lineFlags[lineNum] |= LINE_BOOKMARKED_FLAG;
    }

    int nextBookmark(int dir) {
        int currentLine = currentLineNum();
        if(currentLine+dir < maxLines && currentLine+dir >= 0)
            currentLine+=dir;

        if(DIRECTION_FORWARD==dir) {
            for(int i=currentLine; i<maxLines; i++) {
                if((lineFlags[i]&LINE_BOOKMARKED_FLAG) == LINE_BOOKMARKED_FLAG) {
                    scrollIntoView(i);
                    return i;
                }
            }
        } else {
            for(int i=currentLine; i>=0; i--) {
                if((lineFlags[i]&LINE_BOOKMARKED_FLAG) == LINE_BOOKMARKED_FLAG) {
                    scrollIntoView(i);
                    return i;
                }
            }
        }
        showMsg("No more bookmarks found");
        return -1;
    }

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

    void scrollUp() {
        lineIndex--;
        if(lineIndex<0)
            lineIndex=0;
    }

    void scrollDown() {
        if(maxLines >= getMaxY()) {//make sure the file isn't less then a screen full
            lineIndex++;
            if(lineIndex>=maxLines-getMaxY()) {
                lineIndex = maxLines - getMaxY();
            }
        }
    }

    int currentLineNum() {
        return lineIndex+selectedLine;
    }

    void showMsg(String msg)
    {
        console.move(0,console.getHeight()-1);
        console.printw(msg+" - PRESS ENTER");
        console.refresh();
        console.getch();
    }
}
