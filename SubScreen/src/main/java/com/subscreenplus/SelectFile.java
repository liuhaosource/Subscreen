package com.subscreenplus;

import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import android.content.Intent;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class SelectFile extends FragmentActivity {
    ListView lv;
    ArrayList<String> fileNames = null;
    static final String dirPath =  System.getenv("EXTERNAL_STORAGE") + "/" + "Subtitles/";
    static String curPath = dirPath;
    String downloadString;
    String backString;
    ArrayAdapter adp;
    boolean zipOpened = false;
    boolean isMounted = true;
    FilenameFilter textFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            String lowercaseName = name.toLowerCase();
            if (lowercaseName.endsWith(".nfo")) {
                return false;
            } else {
                return true;
            }
        }
    };
    public void onBackPressed() {
        goBackDirectory();
        updateMenu();
    }
    private void updateMenu() {
        zipOpened = false;
        fileNames = loadFileNames(curPath);
        adp.clear();
        for (String fileName : fileNames)
            adp.add(fileName);
        adp.notifyDataSetChanged();
        lv.setSelection(0);
    }
    void displayExitMessage(String message, String title)
    {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(this.getString(com.subscreenplus.R.string.exit_text), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    System.exit(0);
                }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }
    void displayBackMessage(String message, String title) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(this.getString(com.subscreenplus.R.string.back_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        updateMenu();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    void displayMessage(String message, String title) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(this.getString(com.subscreenplus.R.string.ok_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    protected void onCreate(Bundle savedInstanceState) {
        backString =  this.getString(com.subscreenplus.R.string.back_folder);
        downloadString = "(Download)";
        final Intent intent = getIntent();
        final String action = intent.getAction();
            try {
            isMounted = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
            if (!isMounted) {
                displayExitMessage(this.getString(com.subscreenplus.R.string.storage_not_found),
                        this.getString(com.subscreenplus.R.string.storage_not_found_title));
            }
            File subDirectory = new File(dirPath);
            // have the object build the directory structure, if needed.
            if (!(Intent.ACTION_VIEW.equals(action)) && isMounted && subDirectory.mkdirs()) {
                //isMounted = false;
                writeHelpFiles();
                displayMessage(this.getString(com.subscreenplus.R.string.folder_created),this.getString(com.subscreenplus.R.string.folder_created_title));
            }
            super.onCreate(savedInstanceState);
            setContentView(com.subscreenplus.R.layout.activity_select_file);
            lv = (ListView) findViewById(com.subscreenplus.R.id.file_list);
            fileNames = loadFileNames(curPath);
            if (isMounted && (fileNames == null || fileNames.size() == 0))
                displayExitMessage(this.getString(com.subscreenplus.R.string.no_files_found).replace(
                                this.getString(com.subscreenplus.R.string.subtitle_replace_string),dirPath),
                        this.getString(com.subscreenplus.R.string.no_files_found_title));
            adp = new ArrayAdapter(this, android.R.layout.simple_list_item_1, fileNames);
            lv.setAdapter(adp);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    itemClicked(position);
                }
            });
            if(Intent.ACTION_VIEW.equals(action)) {
                //uri = intent.getStringExtra("URI");
                Uri uri = intent.getData();
                //Remove the first slash so this file is not interpreted as a directory
                handleFileSelected(URLDecoder.decode(uri.getEncodedPath(), "UTF-8").substring(1),true);
            }
        }
        catch (Exception e)
        {
            return;
        }
    }
    private void writeHelpFiles() {
        InputStream helpFileIn = null;
        FileOutputStream helpFileOut = null;
        try {
            helpFileIn = getResources().getAssets().open("BasicUsageTutorial.srt");
            helpFileOut = new FileOutputStream(curPath + "/Basic Usage.srt");
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = helpFileIn.read(buffer)) > 0)
                helpFileOut.write(buffer,0,length);
        } catch (IOException e) {
            return;
        } finally {
            try {
                if (helpFileIn != null)
                    helpFileIn.close();
                if (helpFileOut != null)
                    helpFileOut.close();
            } catch (IOException e) {
                return;
            }
        }
    }
    private void goBackDirectory() {
        //Don't allow for going back past the subtitles directory
        if (curPath.compareTo(dirPath) == 0) {
            return;
        }
        if (curPath.compareTo("/") == 0) {
            return;
        }
        int i;
        //Skip the last character, which will always be a directory symbol
        //Then go back to the previous directory symbol; we want only the part
        //of the string before this
        for (i = curPath.length() -2; i >= 0; i--)
        {
            if (curPath.charAt(i) == '/')
                break;
        }
        curPath = curPath.substring(0,i+1);
    }
    private void itemClicked(int position) {
        String fileName = fileNames.get(position);
        handleFileSelected(fileName, false);
    }
    private void handleFileSelected(String fileName, boolean ignorePath) {
        //If a zip file is loaded, this is the one to be used within it
        String zipFileName = null;
        if (fileName.equals(downloadString)) {
            Intent intent = new Intent(SelectFile.this, Search.class);
            Bundle b = new Bundle();
            b.putString("path", curPath);
            b.putString("username", "");
            b.putString("password", "");
            b.putSerializable("downloader", null);
            intent.putExtras(b);
            startActivity(intent);
            finish();
            return;
        }
        if (fileName.charAt(0) == '/' || fileName.equals(backString))
        {
            //Take all but the first character of the fileName and add a directory
            //slash at the end in order to have the directory symbol at the end
            //of the string
            if (!fileName.equals(backString)) {
                if (!ignorePath)
                    curPath = curPath + fileName.substring(1) + "/";
                else
                    curPath = fileName.substring(1) + "/";
            }
            else
                goBackDirectory();
            updateMenu();
            return;
        }
        if (fileName.endsWith(".zip")) {
            ArrayList<String> zipFileNames;
            if (!ignorePath)
               zipFileNames = FileHelper.readZipFile(curPath + fileName);
            else
                zipFileNames = FileHelper.readZipFile(fileName);
            if (zipFileNames == null) {
                displayBackMessage(getText(com.subscreenplus.R.string.bad_format_message).toString(), "Sorry");
                return;
            }
            else if (zipFileNames.size() == 1) {
                zipFileName = zipFileNames.get(0);
            }
            else {
                adp.clear();
                zipFileNames.add(0,backString);
                for (String tmpName : zipFileNames)
                    adp.add(tmpName);
                adp.notifyDataSetChanged();
                lv.setSelection(0);
                fileNames = zipFileNames;
                zipOpened = true;
                if (!ignorePath)
                    curPath = curPath + fileName;
                else
                    curPath = fileName;
                return;
            }
        }
        else if (zipOpened) {
            zipFileName = fileName;
            //Awkward hack, we're already storing the full path in the curPath variable, so
            //we don't want anything appended to the filename
            fileName = "";
        }
        launchMainActivity(fileName,zipFileName,ignorePath);
    }

    
    private void launchMainActivity(String fileName, String zipFileName, boolean ignorePath) {
        Intent intent = new Intent(SelectFile.this, ShowText.class);
        Bundle b = new Bundle();
        if (!ignorePath)
            b.putString("fileName", curPath + fileName);
        else
            b.putString("fileName",fileName);
        if (zipFileName != null)
            b.putString("zipFileName",zipFileName);
        intent.putExtras(b);
        startActivity(intent);
        finish();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(com.subscreenplus.R.menu.menu_select_file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == com.subscreenplus.R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public ArrayList<String> loadFileNames(String path) {
        ArrayList<String> out = new ArrayList<String>();
        if (path.endsWith(".zip")) {
            zipOpened = true;
            out.addAll(FileHelper.readZipFile(path));
            out.add(0,backString);
            return out;
        }
        File dir = new File(path);
        //If we're at the root directory, don't allow the user to go back
        if (!path.equals(dirPath))
            out.add(backString);
        out.add(downloadString);
        if (!isMounted || !dir.isDirectory() || !dir.canRead())
            return out;
        for (File f : dir.listFiles(textFilter)) {
            if (f.isDirectory())
                out.add("/" + f.getName());
            else
                out.add(f.getName());
        }
        return out;
    }
}
