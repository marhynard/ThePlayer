package com.fritzbang.theplayer;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import org.farng.mp3.MP3File;
import org.farng.mp3.id3.ID3v1;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Locale;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LoadingTrackTask extends AsyncTask<String, Integer, Integer> {
    private static final String DEBUG_TAG = "LoadingTrackTask";
    ProgressDialog mDialog;
    TextView textViewSpace;
    Context context;
    int listSize = 0;
    int numFiles = 0; // number of loaded entities
    long fileSize = 0;
    long availableSpace = 0;
    DBAdapter dbAdapter = null;
    String title = "";
    String artist = "";
    String album = "";
    String path = "";

    public LoadingTrackTask(Context context, TextView textViewSpace) {
        mDialog = new ProgressDialog(context);
        this.context = context;
        this.textViewSpace = textViewSpace;
        mDialog.setMessage("Loading Files");
        mDialog.show();
    }

    @Override
    protected Integer doInBackground(String... params) {
        if(dbAdapter == null){
            dbAdapter = new DBAdapter(context);
            dbAdapter.open();
        }
        numFiles = 0; // number of loaded entities
        String directoryString = params[0];
        if (directoryString != null) {
            ArrayList<String> dbFiles = dbAdapter.getLocations();
            Log.d(DEBUG_TAG,"dbFIles: " + dbFiles.size());
            File directory = new File(directoryString);
            availableSpace = directory.getUsableSpace();
            Log.d(DEBUG_TAG,
                    "Space: " + directory.getFreeSpace() + " "
                            + directory.getUsableSpace());
            ArrayList<File> list = new ArrayList<>();
            if (directory.isDirectory()) {
                list.addAll(getFiles(directory));
            }
            Log.d(DEBUG_TAG, directory.getAbsolutePath());
            if (list.size() > 0) {
                Log.d(DEBUG_TAG, "Got the files " + list.size());
                // Toast.makeText(context, "Got the Files: " + list.length,
                // Toast.LENGTH_SHORT).show();
                this.listSize = list.size();

                int numberCores = Runtime.getRuntime().availableProcessors();

                //Get the ThreadFactory implementation to use
                ThreadFactory threadFactory = Executors.defaultThreadFactory();
                //creating the ThreadPoolExecutor
                ThreadPoolExecutor executorPool = new ThreadPoolExecutor(numberCores, numberCores, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(list.size()), threadFactory);
                for (File file : list) {
                    fileSize += file.length();
                    if (!dbFiles.contains(file.getPath())) {
                        Runnable worker = new MetaDataRetrieverThread(file);
                        executorPool.execute(worker);
                    } else {
                        dbFiles.remove(file.getPath());
                    }
                }
                executorPool.shutdown();
                while (!executorPool.isTerminated()) {
                }
                Log.d(DEBUG_TAG, "Filessize " + fileSize);
                Log.d(DEBUG_TAG, "extra in db " + dbFiles.size());

            }
            for(String location:dbFiles){
                dbAdapter.delete(location);
            }
        }
//        Cursor cursor = dbAdapter.getAllTrackInfo();
//        Log.d(DEBUG_TAG, "Number of Rows: " + cursor.getCount());
        dbAdapter.close();
        dbAdapter = null;
        return numFiles;
    }
    ArrayList<File> getFiles(File directory){
        ArrayList<File> returnList = new ArrayList<>();
        File[] files = directory.listFiles();
        for(File f: files){
            if(f.isDirectory()){
                returnList.addAll(getFiles(f));
            }else{
                if(f.getName().toLowerCase(Locale.US).endsWith(".mp3")){
                    Log.d(DEBUG_TAG,"filename: " + f.getName());
                    returnList.add(f);
                }
            }
        }
        return returnList;
    }

    @Override
    public void onProgressUpdate(Integer... ints) {
        mDialog.setProgress(ints[0]);
        mDialog.setMessage("Loading " + numFiles + "/" + listSize);
        textViewSpace.setText("Used: " + ThePlayerActivity.formatSize(fileSize)
                + " Available: " + ThePlayerActivity.formatSize(availableSpace)
                + " #Tracks: " + listSize);
    }

    public void onPostExecute(Integer result) {
        mDialog.dismiss();
        Log.d(DEBUG_TAG, "Loaded " + result + " Files");

    }

    public class MetaDataRetrieverThread implements Runnable {

        private File file;

        public MetaDataRetrieverThread(File file){
            this.file=file;
        }

        @Override
        public void run() {
            processCommand();
        }

        private void processCommand() {
            MP3File mp3file = null;
            ID3v1 tag = null;

            try {
                mp3file = new MP3File(file);
                tag = mp3file.getID3v1Tag();

            } catch (Exception ex) {
                Log.d(DEBUG_TAG,
                        "there is a problem file setup: " + file.getName() + " : " +ex.getMessage());
            }
                try {
                    title = tag.getSongTitle();
                } catch (NullPointerException ex) {

                    try {
                        title = mp3file.getID3v2Tag().getSongTitle();
                    } catch (NullPointerException es) {
                        Log.d(DEBUG_TAG, "title: " + ex.getMessage());
                        title = file.getName();
                    }
                    if (title.equals("") || title == null) {
                        title = file.getName();
                    }
                }
                try {
                    path = file.getPath();
                } catch (Exception ex) {
                    Log.d(DEBUG_TAG, "path: " + ex.getMessage());
                }
                try {
                    album = tag.getAlbumTitle();
                } catch (Exception ex) {
                    try {
                        album = mp3file.getID3v2Tag().getAlbumTitle();
                    } catch (Exception es) {
                        Log.d(DEBUG_TAG, "album: " + ex.getMessage());
                        album = file.getName();
                    }
                }
                try {
                    artist = tag.getArtist();
                } catch (Exception ex) {

                    try {
                        artist = mp3file.getID3v2Tag().getLeadArtist();
                    } catch (Exception es) {
                        Log.d(DEBUG_TAG, "artist: " + ex.getMessage());
                        artist = file.getName();
                    }
                }
//                Log.d(DEBUG_TAG,"Inserting: " + title);

                    long row = dbAdapter.insertTrackInfo(title, path, artist, album, 0, 0);
                    if (row >= 0)
                        Log.d(DEBUG_TAG, "It worked: " + title);
                    else
                        Log.d(DEBUG_TAG, "It failed: " + title);

                numFiles++;



            publishProgress(numFiles / listSize);
        }

        @Override
        public String toString(){
            return this.file.getName();
        }
    }
}