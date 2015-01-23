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

/**
 * Created by mrhynard on 12/30/2014.
 */
public class LoadListViewTask extends AsyncTask<String, Integer, Integer> {
    private static final String DEBUG_TAG = "LoadingTrackTask";
    ProgressDialog mDialog;
    PlaylistArrayAdapter mAdapter;
    TextView textViewSpace;
    Context context;
    int listSize = 0;
    int numFiles = 0; // number of loaded entities
    long fileSize = 0;
    long availableSpace = 0;
    private ArrayList<TrackBean> trackBeans = new ArrayList<>();
    DBAdapter dbAdapter = null;
    private int sortType = PlaylistArrayAdapter.TITLE_SORT;
    String directory = "";
    public LoadListViewTask(Context context, PlaylistArrayAdapter adapter,
                            TextView textViewSpace,String directory) {
        mDialog = new ProgressDialog(context);
        mAdapter = adapter;
        this.context = context;
        this.textViewSpace = textViewSpace;
        // Do your dialog stuff here
        mDialog.setMessage("Loading Files");
        mDialog.show();
        this.directory = directory;
    }

    @Override
    protected Integer doInBackground(String... params) {
        if(dbAdapter == null){
            dbAdapter = new DBAdapter(context);
            dbAdapter.open();
        }
        if(dbAdapter.isOpen()){
            Cursor cs = dbAdapter.getAllTrackInfo();
            listSize = cs.getCount();
            cs.moveToFirst();
            while (!cs.isAfterLast()) {

                String title    = cs.getString(cs.getColumnIndex(DBAdapter.KEY_TRACK_TITLE));
                String location = cs.getString(cs.getColumnIndex(DBAdapter.KEY_TRACK_LOCATION));
                String artist   = cs.getString(cs.getColumnIndex(DBAdapter.KEY_TRACK_ARTIST));
                String album    = cs.getString(cs.getColumnIndex(DBAdapter.KEY_TRACK_ALBUM));
                int status = cs.getInt(cs.getColumnIndex(DBAdapter.KEY_TRACK_STATUS));
                int position = cs.getInt(cs.getColumnIndex(DBAdapter.KEY_TRACK_POSITION));

                trackBeans.add(new TrackBean(title,location,artist,album,status,position));
                cs.moveToNext();
                publishProgress(trackBeans.size() / listSize);
            }
        }
       numFiles = trackBeans.size();
        dbAdapter.close();
        dbAdapter = null;
        return numFiles;
    }

    @Override
    public void onProgressUpdate(Integer... ints) {
        mDialog.setProgress(ints[0]);
        mDialog.setMessage("Loading " + numFiles + "/" + listSize);
        //textViewSpace.setText(ThePlayerActivity.updateSpaceAvailable(new File(directory)));
    }

    public void onPostExecute(Integer result) {
        mAdapter.addAll(trackBeans);
        mAdapter.sortTracks(sortType);
        mAdapter.notifyDataSetChanged();
        mDialog.dismiss();
        Log.d(DEBUG_TAG, "Loaded " + result + " Files");
    }

    public void setSortType(int sortType) {
        this.sortType = sortType;
    }
}
