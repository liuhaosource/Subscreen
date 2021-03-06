package com.subscreenplus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ShowText extends FragmentActivity {
    static Button pauseButton;
    static Button backButton;
    static Button nextButton;
    static Button prevButton;
    static Button convertFramerateButton;
    static Button languageButton;
    static Button zoomOutButton;
    static Button zoomInButton;
    static String lastFileName = null;
    SubtitlePlayer playerInstance = null;
    ListView frameRateListView;
    ListView languageListView;
    ArrayList<String> validFrameRates;
    ArrayList<Integer> indices;
    static HashMap<String, SubtitlePlayer> cachedPlayers = new HashMap<String, SubtitlePlayer>();
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(com.subscreenplus.R.layout.activity_show_text);
		TextView t = (TextView)findViewById(com.subscreenplus.R.id.text);
        pauseButton = (Button) findViewById(com.subscreenplus.R.id.pauseButton);
        convertFramerateButton = (Button) findViewById(com.subscreenplus.R.id.setFrameButton);
        languageButton = (Button) findViewById(com.subscreenplus.R.id.languageButton);
        try {
            initMenu();
        } catch (Exception e) {
            return;
        }
        Bundle b = getIntent().getExtras();
        String fileName = b.getString("fileName");
        String zipFileName = b.getString("zipFileName");
        lastFileName = fileName + zipFileName;
        FileHelper.EncodingWrapper fileStream = FileHelper.readFile(fileName, zipFileName);
        BufferedInputStream fileData = new BufferedInputStream(fileStream.data);
        backButton = (Button) findViewById(com.subscreenplus.R.id.doLoginButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnToSelectScreen();
            }
        });
        prevButton = (Button) findViewById(com.subscreenplus.R.id.prevButton);
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerInstance.prevSubtitle();
            }
        });
        prevButton.setVisibility(View.GONE);
        nextButton = (Button) findViewById(com.subscreenplus.R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerInstance.nextSubtitle();
            }
        });
        zoomOutButton = (Button) findViewById(com.subscreenplus.R.id.zoomOut);
        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomOut();
            }
        });
        zoomInButton = (Button) findViewById(com.subscreenplus.R.id.zoomIn);
        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomIn();
            }
        });
        try {
            SubtitlePlayer player = null;
            if (cachedPlayers.containsKey(fileName))
                player = cachedPlayers.get(fileName);
            if (player == null || player.finished
                    || (!player.playbackStarted && player.subCount == 0))  {
                playerInstance = new SubtitlePlayer();
                cachedPlayers.put(fileName, playerInstance);
                playerInstance.main(t, this.getApplicationContext(), fileData, this, fileStream.encoding, fileName);
            } else {
                playerInstance = cachedPlayers.get(fileName);
                playerInstance.playbackStarted = false;
                playerInstance.resume(t, this.getApplicationContext(), fileData, this, fileStream.encoding);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void zoomIn() {
        playerInstance.outputTo.zoomIn();
    }
    private void zoomOut() {
        playerInstance.outputTo.zoomOut();
    }
    public void onBackPressed() {
        returnToSelectScreen();
    }
    public void updateButtons(final int count, final int maxCount){
                if (count == 0)
                    prevButton.setVisibility(View.GONE);
                else
                    prevButton.setVisibility(View.VISIBLE);
                if (count >= maxCount - 1)
                    nextButton.setVisibility(View.GONE);
                else
                    nextButton.setVisibility(View.VISIBLE);
    }
    void displayBackMessage(String message, String title)
    {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(this.getString(com.subscreenplus.R.string.exit_text), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        returnToSelectScreen();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }
    public void returnToSelectScreen(){
        Intent intent = new Intent(ShowText.this, SelectFile.class);
        playerInstance.cleanup();
        startActivity(intent);
        finish();
    }
    void initMenu()
    {
        validFrameRates = new ArrayList<String>(Arrays.asList(FrameBlock.frameRateStrings));
        final Dialog framerateDialog = new Dialog(this);
        framerateDialog.setTitle(this.getString(com.subscreenplus.R.string.framerate_dialog_title));
        framerateDialog.setContentView(com.subscreenplus.R.layout.menu_choice);
        frameRateListView = (ListView) framerateDialog.findViewById(com.subscreenplus.R.id.choices);
        frameRateListView.setAdapter(new ArrayAdapter(this, com.subscreenplus.R.layout.menu_encoding, validFrameRates));
        convertFramerateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFramerates(framerateDialog);
                framerateDialog.show();
            }
        });
        frameRateListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Take the value from the indices since otherwise positions will be off due
                double frameRate = FrameBlock.frameRates[indices.get(position)];
                playerInstance.convertFramerate(frameRate, indices.get(position));
                framerateDialog.hide();
            }
        });
        final Dialog languageDialog = new Dialog(this);
        languageDialog.setTitle("Select Language");
        languageDialog.setContentView(com.subscreenplus.R.layout.menu_choice);
        languageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseLanguage();
                languageDialog.show();
            }
        });

        languageListView = (ListView) languageDialog.findViewById(com.subscreenplus.R.id.choices);
        languageListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playerInstance.switchLanguage(id);
                languageDialog.hide();
            }
        });
    }
    public void chooseLanguage() {
        //Currently only SMI format supports multiple languages
        if (playerInstance.smiSub == null)
            return;
        languageListView.setAdapter(new ArrayAdapter(this, com.subscreenplus.R.layout.menu_encoding, playerInstance.languages));
        languageListView.invalidateViews();
    }
    public void chooseFramerates(Dialog framerateDialog) {
        FrameBlock currentBlock = (FrameBlock) playerInstance.blocks.get(playerInstance.subCount);
        double currentModifier = playerInstance.getCurrentFramerate();
        indices = new ArrayList<Integer>();
        long maxFrame = playerInstance.getLastFrame();
        validFrameRates.clear();
        for (int i = 0; i < FrameBlock.frameRateMultipliers.length; i++)
        {
            if (FrameBlock.frameRateMultipliers[i] == currentModifier)
                continue;
            //If playback has not been initialized already, we obviously haven't gone past the
            //time limit
            if (playerInstance.playbackStarted == true) {
                long currentFrame = currentBlock.checkFramerate(FrameBlock.frameRateMultipliers[i], i);
                if (currentFrame > maxFrame)
                    continue;
            }
            validFrameRates.add(FrameBlock.frameRateStrings[i]);
            //Since not all framerates will be added to the list of available framerates, we need
            //to make sure that we keep track of the true index by storing the corresponding index
            //of each frame rate
            indices.add(i);
        }
        frameRateListView.setAdapter(new ArrayAdapter(this, com.subscreenplus.R.layout.menu_encoding, validFrameRates));
        frameRateListView.invalidateViews();
    }
    public void pause(View v)
    {
        playerInstance.pause();
    }
    public static void setButton(String input){
        pauseButton.setText(input);
    }
}
