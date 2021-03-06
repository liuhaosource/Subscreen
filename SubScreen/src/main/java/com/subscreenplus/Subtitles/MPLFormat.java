package com.subscreenplus.Subtitles;

import com.subscreenplus.SubtitlePlayer;
import com.subscreenplus.TextBlock;
import com.subscreenplus.TimeBlock;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.ArrayList;

/**
 * Created by Nick on 12/27/2014.
 */
public class MPLFormat implements SubtitleFormat {
    SubtitlePlayer playerInstance = null;
    public MPLFormat(SubtitlePlayer tmpPlayer)
    {
        playerInstance = tmpPlayer;
    }
    public ArrayList<TextBlock> readFile(InputStream data, String srcCharset) {
        try {
            ArrayList<TextBlock> blocks = new ArrayList<>();
            blocks.add(new TimeBlock(SubtitlePlayer.playString,0,-1,playerInstance));
            BufferedReader br = new BufferedReader(new InputStreamReader(data, srcCharset));
            readLines(br, blocks);
            return blocks;
        }
        catch (Exception e)
        {
            return null;
        }
    }
    void readLines(BufferedReader in, ArrayList<TextBlock> blocks) {
        Pattern p = Pattern.compile("\\[(\\d*)\\]\\[(\\d*)\\](.*)");
        String buffer;
        Matcher m;
        String text;
        long startTime, endTime;
        try {
            while ((buffer = in.readLine()) != null) {
                m = p.matcher(buffer);
                if (m.find()) {
                    //Multiple by 100 because it is in tenths of a second;
                    //this is the same as multiplying by 1000 to convert to milliseconds
                    //and dividing by 10
                    startTime = Integer.parseInt(m.group(1))*100;
                    endTime = Integer.parseInt(m.group(2))*100;
                    text = MicroDVDFormat.buildOptions(parseText( m.group(3)));
                    blocks.add(new TimeBlock(text, startTime, endTime, playerInstance));
                }
            }
        }
        catch (Exception e)
        {
            return;
        }
    }
    String parseText(String text)
    {
        StringBuilder start = new StringBuilder();
        StringBuilder end = new StringBuilder();
        int startOff = 0;
        if (text.charAt(0) == '/')
        {
            start.append("<i>");
            end.insert(0,"</i>");
            startOff++;
        }
        int off = text.indexOf('|');
        if (off != -1)
        {
            end.append("<br>");
            end.append(parseText(text.substring(off+1)));
            start.append(text.substring(startOff,off));
        }
        else {
            start.append(text.substring(startOff));
        }
        start.append(end.toString());
        return start.toString();
    }
}
