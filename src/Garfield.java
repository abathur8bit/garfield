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

public class Garfield {
    static int CURRENT_LINE = 1;
    static int NORMAL_LINE = 2;
    static int STATUS_BAR = 3;

    private NConsole console;
    private String filename;
    private boolean running = true;
    private ArrayList<String> fileContents = new ArrayList<String>();
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
        console.initPair(CURRENT_LINE,NConsole.COLOR_BLACK,NConsole.COLOR_WHITE);
        console.initPair(STATUS_BAR,NConsole.COLOR_YELLOW,NConsole.COLOR_BLUE);
//        showSplash();
    }

    public void showSplash() {
        console.clear();
        console.printCenterX(console.getScreenHeight()/2,"Garfield the Log Viewer");
        console.getch();
    }

    public void view() {
        console.clear();
        while(running) {
            screenWidth = console.getScreenWidth();
            screenHeight = console.getScreenHeight();
            final int maxy = getMaxY();

            showFile();
            showStatusBar();


            int ch = console.getch();
            if (ch == 'q' || ch == 'Q') {
                running = false;
            } else if (ch == 'j') { //27 && console.getch() == 91 && console.getch() == 65) {
                cursorDown();   //down arrow
            } else if (ch == 'k') { //k'k27 && console.getch() == 91 && console.getch() == 66) {
                cursorUp();     //up arrow
            } else if(ch == 'J') {
                scrollDown();
            } else if(ch == 'K') {
                scrollUp();
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
    }

    private void showFile() {
        console.home();

        int maxx = screenWidth;
        int maxy = getMaxY();

        for(int i=lineIndex,y=0; i<maxLines && y<maxy; i++,y++) {
            if(i==lineIndex+selectedLine)
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
        if(selected) {
            console.attron(CURRENT_LINE);
            console.printw(row);
            fillLine(row.length(),maxWidth-row.length()-1,' ');
            console.attroff(CURRENT_LINE);
        } else {
            console.printw(row);
            fillLine(row.length(),maxWidth-row.length()-1,' ');
        }
    }

    private void showStatusBar() {
        console.attron(STATUS_BAR);
        final int currentLine = selectedLine+lineIndex+1;   //when showing the user, first line is 1, not 0.
        console.move(0,getMaxY());
        fillLine(0,screenWidth,' ');
        console.move(0,getMaxY());
        console.printw("Type 'h' for help | Line: "+currentLine+" of "+maxLines+" | File: "+filename+" | W:"+screenWidth+" H:"+screenHeight);
        console.attroff(STATUS_BAR);
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
        return console.getScreenHeight()-1;
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
}
