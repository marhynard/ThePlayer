package com.fritzbang.theplayer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi")
public class ThePlayerActivity extends Activity {

    private static final String DEBUG_TAG = "ThePlayerActivity";

    private ThePlayerMediaService thePlayerMediaService;

    static final String STATE_TRACK_POSITION = "currentTrackPosition";
    static final String STATE_TRACK_NAME = "trackName";
    static final String STATE_TRACK_LOCATION = "trackLocation";
    static final String STATE_TRACK_LIST_POSITION = "trackListPosition";
    static final String STATE_LIST_SORT_TYPE = "sortType";

    public TextView textViewSongName, textViewAlbumName, textViewArtist, currentTrackPositionField, endTimeField, textViewSpace;
    private int currentTrackPosition = 0;
    private Handler myHandler = new Handler();
    private int forwardTime = 5000;
    private int backwardTime = 5000;
    private SeekBar seekBar;
    private ImageButton playButton;// , nextButton, ffButton, rewButton,
    // previousButton;
    private ListView playlistView;
    private TrackBean selectedTrackInfo = new TrackBean();
    PlaylistArrayAdapter plaAdapter;
    private int sortType = PlaylistArrayAdapter.TITLE_SORT;

    ArrayList<TrackBean> trackBeans = new ArrayList<>();

    private boolean isPlaying = false;
    private boolean gotFinish = false;
    int currentSongListIndex = -1;
    int streamVolume = 0;
    Context context = null;
    String directoryLocation;
    boolean restoreTrack = false;

    LoadListViewTask loadListViewTask = null;
    LoadingTrackTask loadingTrackTask = null;
    final CharSequence[] sortType_radio = {"Title", "Album", "Artist", "Track"};
    NoisyAudioStreamReceiver myNoisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

    // TODO update the files system within the app to have a podcast location and music location and book location
    // TODO standardize the lists between the Podcasts and Music tracks
    // TODO when loading the playlist load the correct files either podcasts,music, or both
    // TODO delete empty folders when deleting files
    // TODO organize the songs into folders from where they came(this will be in the copying software)
    // TODO fix the download service
    // TODO add a progress monitor to the downloads

    // TODO add pause and play from the headphones

    // TODO add album art (musicbrainz.org)

    // TODO add functionality to add files to the directory remotely
    // TODO add new files to the database and update view
    // TODO make sure app doesn't shutdown while copying files

    // TODO add visualization
    // TODO change icons for the app
    // TODO go through activities and code added and integrate Podcast functionality into theplayer
    // TODO activity for listing the podcast channels
    // TODO activity for listing the Available Episodes (highlighted and shaded for status)
    // TODO activity for listing the downloaded and status of them
    // TODO activity for subscribing to podcasts
    // TODO importing podcasts from opml
    // TODO finding podcasts
    // TODO recomendations
    // TODO check out gpodder service for searching for podcasts
    // TODO create activity to clear the rss feed cache
    // TODO create a settings menu
    // TODO add toggle to display podcasts with music

    // TODO add books feature?

    //Much later features to add
    // TODO add the chrome cast ability
    // TODO figure out the proper way to manage other files(bypass the Android 5 requirement to use app-directory)
    // TODO remove the debugging messages


    // /mnt/sdcard/Music /mnt/sdcard/NewMusic
    // "/storage/sdcard1/Android/data/com.fritzbang.theplayer/files"

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            ThePlayerMediaService.MyBinder b = (ThePlayerMediaService.MyBinder) binder;
            thePlayerMediaService = b.getService();
            thePlayerMediaService.setHandler(ThePlayerActivity.this.handler);
            Toast.makeText(ThePlayerActivity.this, "Connected mConnection",
                    Toast.LENGTH_SHORT).show();
            Log.d(DEBUG_TAG, "Connected mConnection");

            if (restoreTrack) {
                restorePlayer();
            }
        }


        @Override
        public void onServiceDisconnected(ComponentName className) {
            thePlayerMediaService = null;
        }

    };

    private boolean mIsBound = false;
    private boolean isRegistered = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Restore the previous state
        ActivityHelper.initialize(this);
        if (savedInstanceState != null) {

            currentTrackPosition = savedInstanceState.getInt(STATE_TRACK_POSITION);
            selectedTrackInfo.position = currentTrackPosition;
            selectedTrackInfo.trackTitle = savedInstanceState
                    .getString(STATE_TRACK_NAME);
            selectedTrackInfo.location = savedInstanceState
                    .getString(STATE_TRACK_LOCATION);
            currentSongListIndex = savedInstanceState
                    .getInt(STATE_TRACK_LIST_POSITION);
            sortType = savedInstanceState.getInt(STATE_LIST_SORT_TYPE);
            Log.d(DEBUG_TAG, "sortType: " + sortType);
            if (selectedTrackInfo.trackTitle != null)
                restoreTrack = true;
        }
        if (currentSongListIndex == -1) {
            DBAdapter dbAdapter = new DBAdapter(this);
            dbAdapter.open();
            TrackBean currentTrackBean = dbAdapter.getCurrentTrack();
            dbAdapter.close();
            if (currentTrackBean != null) {
                selectedTrackInfo = currentTrackBean;
            }
        }

        try {
            directoryLocation = getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
        } catch (NullPointerException ex) {
        }
        // Restore preferences
        //SharedPreferences settings = getPreferences(MODE_PRIVATE);

        // check to see if the directory is in the preferences if not
        // check to see if the external directory is available if not then
        // use the internal

        //directoryLocation = settings.getString("directoryLocation", "none");
        /*if (directoryLocation.equals("none")) {*/
        if (isExternalStorageReadable()) {
            Toast.makeText(this, "the sd card is available",
                    Toast.LENGTH_SHORT).show();
            try {
                File[] externalDirs = getApplicationContext().getExternalFilesDirs(null);
                Log.d(DEBUG_TAG, "externalDirsLength: " + externalDirs.length);
                if (externalDirs.length == 2) {
                    Log.d(DEBUG_TAG, "file1: " + externalDirs[1].toString());
                    directoryLocation = externalDirs[1].toString();
                }
            } catch (java.lang.NullPointerException ex) {
                Toast.makeText(this, "null pointer", Toast.LENGTH_SHORT)
                        .show();
                Log.d(DEBUG_TAG, "null pointer: " + directoryLocation);
            }
        } else {
            Toast.makeText(this, "the sd card is not available",
                    Toast.LENGTH_SHORT).show();
            Log.d(DEBUG_TAG, "sd card not available: " + directoryLocation);
        }
		/*}*/
        //File basePath = getApplicationContext().getExternalFilesDir(null);//new File(getApplicationContext().getExternalFilesDir(null), "com.fritzbang.theplayer");
//        if(!basePath.exists()){
//            basePath.mkdir();
//        }
        // directoryLocation = basePath.getAbsolutePath();

        Toast.makeText(this, directoryLocation, Toast.LENGTH_SHORT).show();
        Log.d(DEBUG_TAG, "Directory location: " + directoryLocation);

        // Sets the layout
        setContentView(R.layout.activity_the_player);

        // Stores the context
        context = this;

        // Initializes the views
        initializeViews();
        if (restoreTrack) {
            doBindService();
            // restorePlayer();
        }
        addPlayList();

        registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
        isRegistered = true;
    }

    @Override
    public void onPause() {
        super.onPause(); // Always call the superclass method first
        Log.d(DEBUG_TAG, "pausing activity");
        // stop checking the status
        myHandler.removeCallbacks(UpdateSongTime);
        updateDatabase();
    }


    @Override
    public void onResume() {
        super.onResume(); // Always call the superclass method first
        Log.d(DEBUG_TAG, "resuming activity");
        // reconnect to the service
        doBindService();
        // start checking the service again
        if (mIsBound) {
            //restorePlayer();
            Log.d(DEBUG_TAG,"mIsBound");
            seekBar.setClickable(true);
            myHandler.postDelayed(UpdateSongTime, 100);
            if(isPlaying) {
                Log.d(DEBUG_TAG, "is Playing");
            }else {
                Log.d(DEBUG_TAG, "is not Playing");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    private boolean isMyServiceRunning(){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if(ThePlayerMediaService.class.getName().equals(service.service.getClassName())){
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (!isPlaying) {
            this.stopApplication();
        } else {
            moveTaskToBack(true);
        }
    }

    private void doBindService() {
        Log.d(DEBUG_TAG, "binding service");
        Intent intent = new Intent(this, ThePlayerMediaService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    private void doUnbindService() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private void restorePlayer() {
        Log.d(DEBUG_TAG, "restore: " + selectedTrackInfo.trackTitle + " "
                + selectedTrackInfo.location);
        this.textViewSongName.setText(selectedTrackInfo.trackTitle);
        this.textViewAlbumName.setText(selectedTrackInfo.album);
        this.textViewArtist.setText(selectedTrackInfo.artist);
        currentTrackPosition = selectedTrackInfo.position;
//		String songTitle = selectedTrackInfo.trackTitle;
//		songName.setText(songTitle);
        thePlayerMediaService.restore(selectedTrackInfo, currentTrackPosition);
        gotFinish = false;
        playButton.setImageResource(android.R.drawable.ic_media_pause);
        seekBar.setClickable(true);
        myHandler.postDelayed(UpdateSongTime, 100);

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "savedInstanceState");
        // Save the user's current state
        savedInstanceState.putInt(STATE_TRACK_POSITION, currentTrackPosition);
        savedInstanceState.putString(STATE_TRACK_NAME,
                selectedTrackInfo.trackTitle);
        savedInstanceState.putString(STATE_TRACK_LOCATION,
                this.selectedTrackInfo.location);
        savedInstanceState.putInt(STATE_TRACK_LIST_POSITION,
                this.currentSongListIndex);
        savedInstanceState.putInt(STATE_LIST_SORT_TYPE, sortType);
        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    // Initializes the views
    public void initializeViews() {
        textViewSongName = (TextView) findViewById(R.id.textViewSongTitle);
        textViewAlbumName = (TextView) findViewById(R.id.textViewAlbumTitle);
        textViewArtist = (TextView) findViewById(R.id.textViewArtist);
        currentTrackPositionField = (TextView) findViewById(R.id.textViewCurrentTime);
        endTimeField = (TextView) findViewById(R.id.textViewTotalTime);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        playButton = (ImageButton) findViewById(R.id.buttonPlay);
        seekBar.setClickable(false);
        seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
        textViewSpace = (TextView) findViewById(R.id.textViewSpace);
    }

    // Adds the playlist to the main view
    private void addPlayList() {

        if (loadListViewTask != null)
            loadListViewTask.cancel(true);

        if (playlistView != null) {
            Log.d(DEBUG_TAG, "redoing the playlist");
            trackBeans.clear();
        }

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        sortType = settings.getInt(STATE_LIST_SORT_TYPE, PlaylistArrayAdapter.TRACK_SORT);

        playlistView = (ListView) findViewById(R.id.listViewPlaylist);

        plaAdapter = new PlaylistArrayAdapter(this, trackBeans);
        Log.d(DEBUG_TAG, "The sort type: " + sortType);
        plaAdapter.setSortType(sortType);
        loadListViewTask = new LoadListViewTask(context, plaAdapter,
                textViewSpace, directoryLocation);
        loadListViewTask.setSortType(sortType);
        // This is for debug only
        // directoryLocation =
        // "/sdcard/Android/data/com.fritzbang.theplayer/files";

        loadListViewTask.execute(directoryLocation);

        playlistView.setAdapter(plaAdapter);

        playlistView.setClickable(true);
        playlistView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);//.CHOICE_MODE_SINGLE);// MULTIPLE

        playlistView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.d(DEBUG_TAG, "Item is clicked:" + id + " " + position);
                // start new
                Log.d(DEBUG_TAG,
                        "click: Size of playlistView: "
                                + playlistView.getCount() + " " + position);
                // Sets the currentSongListIndex

                if (currentSongListIndex != position) {
                    if (currentSongListIndex != -1) {
                        Log.d(DEBUG_TAG, "Thepositionis: " + thePlayerMediaService.getCurrentPosition());
                        Log.d(DEBUG_TAG, "Thelocationis: " + currentSongListIndex);
                        ThePlayerActivity.this.updateTrackBeanState(currentSongListIndex, TrackBean.TRACK_STATUS_PARTIAL, thePlayerMediaService.getCurrentPosition());
                    }

                    currentSongListIndex = position;
                    getNextTrackToPlay(currentSongListIndex);
                } else {
                    if (currentSongListIndex == -1) {
                        currentSongListIndex = position;
                        getNextTrackToPlay(currentSongListIndex);
                    }

                }

            }

        });
//		playlistView.setOnItemLongClickListener(new OnItemLongClickListener() {
//
//			public boolean onItemLongClick(AdapterView<?> parent, View view,
//					int position, long id) {
//				Log.d(DEBUG_TAG, "Item is long clicked:" + id + " " + position);
//				showPodcastAlert(position);
//				return true;
//			}
//
//		});

        playlistView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                final int checkedCount = playlistView.getCheckedItemCount();
                mode.setTitle(checkedCount + " Selected");
                plaAdapter.toggleSelection(position);
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.activity_main, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                SparseBooleanArray selected = plaAdapter.getSelectedIds();
                switch (item.getItemId()) {
                    case R.id.delete:
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                TrackBean selectedItem = plaAdapter.getItem(selected.keyAt(i));
                                plaAdapter.remove(selectedItem);

                                File file = new File(selectedItem.location);
                                //File dir = file.getParentFile();
                                new DeleteTrackTask(context, textViewSpace,directoryLocation).execute(file);
                                currentSongListIndex = plaAdapter.getItem(selectedTrackInfo.location);
                                //updateSpaceAvailable(dir);

                            }
                        }
                        mode.finish();
                        return true;
                    case R.id.played:
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                TrackBean selectedItem = plaAdapter.getItem(selected.keyAt(i));
                                plaAdapter.updateStatus(selected.keyAt(i), TrackBean.TRACK_STATUS_FINISHED, selectedItem.position);

                            }
                        }
                        mode.finish();
                        return true;
                    case R.id.not_played:
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                TrackBean selectedItem = plaAdapter.getItem(selected.keyAt(i));
                                plaAdapter.updateStatus(selected.keyAt(i), TrackBean.TRACK_STATUS_UNPLAYED, 0);
                            }
                        }
                        mode.finish();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                plaAdapter.removeSelection();
            }
        });
        textViewSpace.setText(updateSpaceAvailable(new File(directoryLocation)));
    }

    // plays or pauses the track being played
    public void play(View view) {

        if (isPlaying) {
            thePlayerMediaService.pause();
            playButton.setImageResource(android.R.drawable.ic_media_play);
            updateTrackBeanState(currentSongListIndex, TrackBean.TRACK_STATUS_CURRENT, thePlayerMediaService.getCurrentPosition());
            isPlaying = false;

        } else {
            if (currentSongListIndex == -1) {
                if (this.plaAdapter.isEmpty()) {
                    Toast.makeText(context, "The Playlist is empty cannot start.", Toast.LENGTH_LONG);
                    return;
                }
                // Restore preferences
                //SharedPreferences settings = getPreferences(MODE_PRIVATE);
                String currentFile = selectedTrackInfo.location;//settings.getString(STATE_TRACK_LOCATION,"none");
                //currentTrackPosition = settings.getInt(STATE_TRACK_POSITION,0);
                if (currentFile != null && currentFile.equals("none")) {
                    currentSongListIndex = 0;

                } else {
                    Log.d(DEBUG_TAG, "The currentFile is: " + currentFile);
                    Log.d(DEBUG_TAG, "The currentPosition is: " + currentTrackPosition);
                    currentSongListIndex = this.plaAdapter.getItem(currentFile);
                }

            } else {
                Log.d(DEBUG_TAG, "It was probably paused");

            }

            getNextTrackToPlay(currentSongListIndex);
            isPlaying = true;
        }


    }

    private Runnable UpdateSongTime = new Runnable() {

        public void run() {
            try {
                if (thePlayerMediaService.isPlaying()) {
                    currentTrackPosition = thePlayerMediaService.getCurrentPosition();
                    currentTrackPositionField.setText(convertToTime(currentTrackPosition));
                    playButton
                            .setImageResource(android.R.drawable.ic_media_pause);
                    isPlaying = true;
                    if (!gotFinish) {
                        int finalTime = thePlayerMediaService.getFinishTime();
                        endTimeField.setText(convertToTime(finalTime));
                        seekBar.setMax(finalTime);
                        gotFinish = true;
                    }
                    // Log.d(DEBUG_TAG, "currentTrackPosition: " + currentTrackPosition + " : " +
                    // finalTime);
                    seekBar.setProgress((int) currentTrackPosition);
                    //int currpos = saveTrackState();
                    //updateTrackBeanState(currentSongListIndex, TrackBean.TRACK_STATUS_CURRENT, currpos);
                    myHandler.postDelayed(this, 100);
                }
            } catch (NullPointerException ex) {
                if (isPlaying)
                    myHandler.postDelayed(this, 100);
            }
        }
    };

    public void forward(View view) {
        thePlayerMediaService.forward(forwardTime);
        int currpos = saveTrackState();
        //updateTrackBeanState(currentSongListIndex, TrackBean.TRACK_STATUS_CURRENT, currpos);
    }

    public void rewind(View view) {
        thePlayerMediaService.rewind(backwardTime);
        int currpos = saveTrackState();
        //updateTrackBeanState(currentSongListIndex, TrackBean.TRACK_STATUS_CURRENT, currpos);
    }

    public void previous(View view) {
        Toast.makeText(getApplicationContext(), "Going to Previous Track",
                Toast.LENGTH_SHORT).show();
        if (!isPlaying) {
            getNextTrackToPlay(currentSongListIndex);
        }
        int currpos = saveTrackState();
        updateTrackBeanState(currentSongListIndex, TrackBean.TRACK_STATUS_PARTIAL, currpos);
        // load the previous track on the list
        currentSongListIndex--;
        if (currentSongListIndex < 0)
            currentSongListIndex = 0;
        //currentTrackPosition = 0;
        getNextTrackToPlay(currentSongListIndex);

    }

    public void next(View view) {
        Toast.makeText(getApplicationContext(), "Going to Next Track",
                Toast.LENGTH_SHORT).show();
        Log.d(DEBUG_TAG, "Going to Next Track is playing: " + isPlaying);
        if (!isPlaying) {
            getNextTrackToPlay(currentSongListIndex);
        }
        int currpos = saveTrackState();
        updateTrackBeanState(currentSongListIndex, TrackBean.TRACK_STATUS_PARTIAL, currpos);
        // updateTrackBeanState(currentSongListIndex,TrackBean.TRACK_STATUS_PARTIAL,ThePlayerActivity.this.thePlayerMediaService.getCurrentPosition());
        // load the next track on the list
        currentSongListIndex++;

        //currentTrackPosition = 0;
        getNextTrackToPlay(currentSongListIndex);
        // playlistView.setSelection(currentSongListIndex);
    }

    private void getNextTrackToPlay(int trackToPlay) {
        Log.d(DEBUG_TAG,
                "gnt: Size of playlistView: " + playlistView.getCount());
        try {

            selectedTrackInfo = (TrackBean) playlistView
                    .getItemAtPosition(trackToPlay);
            Log.d(DEBUG_TAG, "trackTitle: " + selectedTrackInfo.trackTitle);
            Log.d(DEBUG_TAG, "location: " + selectedTrackInfo.location);
            Log.d(DEBUG_TAG, "position: " + selectedTrackInfo.position);
            playlistView.setSelection(trackToPlay);
            textViewSongName.setText(selectedTrackInfo.trackTitle);
            textViewArtist.setText(selectedTrackInfo.artist);
            textViewAlbumName.setText(selectedTrackInfo.album);

            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int result = am.requestAudioFocus(afChangeListener,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN);

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Start playback.

                thePlayerMediaService.play(selectedTrackInfo);
                currentTrackPosition = selectedTrackInfo.position;
                updateTrackBeanState(trackToPlay, TrackBean.TRACK_STATUS_CURRENT, currentTrackPosition);
                thePlayerMediaService.seek(selectedTrackInfo.position);
                isPlaying = true;
                gotFinish = false;
                playButton.setImageResource(android.R.drawable.ic_media_pause);
                seekBar.setClickable(true);
                myHandler.postDelayed(UpdateSongTime, 100);
            }
        } catch (java.lang.IndexOutOfBoundsException ex) {
            Log.e(DEBUG_TAG, "Out of Bounds: " + trackToPlay + " " + ex);
        }
    }

    private int saveTrackState() {
        int currentPosition = thePlayerMediaService.getCurrentPosition();
        String currentFile = this.selectedTrackInfo.location;

        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(STATE_TRACK_LOCATION, currentFile);
        editor.putInt(STATE_TRACK_POSITION, currentPosition);
        editor.commit();
        return currentPosition;
    }

    private void updateTrackBeanState(int trackIndex, int status, int position) {
        Log.d(DEBUG_TAG, "Updating trackBean");
        this.plaAdapter.updateStatus(trackIndex, status, position);
//        DBAdapter db = new DBAdapter(context);
//        db.open();
//        db.updateStatusInfo(this.plaAdapter.getItem(trackIndex),status,position);
//        db.close();
    }

    private void updateDatabase() {
        DBAdapter db = new DBAdapter(context);
        db.open();
        for (int x = 0; x < this.plaAdapter.getCount(); x++) {
            db.updateStatusInfo(this.plaAdapter.getItem(x));
        }
        db.close();
    }


    protected void setTime(int currentPosition, int duration) {
        currentTrackPositionField.setText(convertToTime(currentPosition));
        endTimeField.setText(convertToTime(duration));

    }

    private String convertToTime(double duration) {
        double seconds = (double) duration / 1000;

        double minutes = seconds / 60;
        int sec = (int) ((minutes - ((int) minutes)) * 60);
        String convertedTime;

        if (sec < 10)
            convertedTime = (int) minutes + ":0" + sec;
        else
            convertedTime = (int) minutes + ":" + sec;
        return convertedTime;
    }

    private void showPodcastAlert(final int position) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final TrackBean trackToDelete = (TrackBean) playlistView
                .getItemAtPosition(position);

        alertDialog.setTitle("Delete Track");
        alertDialog.setMessage("Are you sure you want to delete: "
                + trackToDelete.trackTitle + "?");

        alertDialog.setPositiveButton("Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(DEBUG_TAG, "Call delete activity");
                        plaAdapter.delete(position);
                        plaAdapter.notifyDataSetChanged();
                        File file = new File(trackToDelete.location);
                        File dir = file.getParentFile();

                        new DeleteTrackTask(context, textViewSpace,directoryLocation).execute(file);
                        //updateSpaceAvailable(dir);

                    }
                });
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(DEBUG_TAG, "Do nothing");
                        dialog.cancel();
                    }
                });

        alertDialog.show();

    }

    public static String updateSpaceAvailable(File dir) {
        String returnString = "";
        long availableSpace = dir.getUsableSpace();
        long numFiles = dir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase(Locale.US);
                if (lowercaseName.endsWith(".mp3")) {
                    return true;
                } else {
                    return false;
                }
            }
        }).length;
        long fileSize = ThePlayerActivity.getDirectorySize(dir);
        returnString = "Used: " + formatSize(fileSize) + " Available: "
                + formatSize(availableSpace) + " #Tracks: " + numFiles;
        Log.d(DEBUG_TAG, returnString);
        return returnString;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.the_player, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_settings:
                openSettingsMenu();
                return true;
            case R.id.action_location:
                showLocationSelection();
                return true;
            case R.id.action_exit:
                stopApplication();
                return true;
            case R.id.action_sort:
                showSortSelection();
                return true;
            case R.id.action_add:
                addNewFiles();
                return true;
            case R.id.action_podcast:
                handlePodcasts();
                return true;
            case R.id.action_refresh:
                refreshDatabase();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void openSettingsMenu() {
        Log.d(DEBUG_TAG,"opening settings");
        Intent intent = new Intent(this,com.fritzbang.theplayer.SettingsActivity.class);
        startActivity(intent);
    }

    private void handlePodcasts() {
        Intent intent = new Intent(this,com.fritzbang.theplayer.MainDownLowActivity.class);
        startActivity(intent);
    }

    private void addNewFiles() {
        //Intent intent = new Intent(this, com.fritzbang.theplayer.SambaExplorer.class);
        Intent intent = new Intent(this, com.fritzbang.theplayer.SambaLogin.class);
        intent.putExtra("directoryLocation", directoryLocation);
        startActivity(intent);
    }

    private void refreshDatabase() {
        Log.d(DEBUG_TAG, "refreshing Database");
        if (loadingTrackTask != null)
            loadListViewTask.cancel(true);
        loadingTrackTask = new LoadingTrackTask(context, textViewSpace);
        loadingTrackTask.execute(directoryLocation);
        addPlayList();
    }

    private void showSortSelection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sort")
                .setSingleChoiceItems(sortType_radio, -1, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                Toast.makeText(getApplicationContext(),
//                        "The sort type is "+sortType_radio[which], Toast.LENGTH_LONG).show();
                        sortType = which;
                        ThePlayerActivity.this.plaAdapter.sortTracks(which);
                        ThePlayerActivity.this.plaAdapter.notifyDataSetChanged();
                        SharedPreferences settings = getPreferences(MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putInt(STATE_LIST_SORT_TYPE, sortType);
                        editor.commit();
                        //dismissing the dialog when the user makes a selection.
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void showLocationSelection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Location");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(directoryLocation);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                directoryLocation = input.getText().toString();
                addPlayList();
            }
        });
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        builder.show();

    }

    @Override
    protected void onStop() {
        super.onStop();
        int currpos = saveTrackState();
        if (currentSongListIndex != -1 && this.isFinishing()) {
            updateTrackBeanState(currentSongListIndex, TrackBean.TRACK_STATUS_PARTIAL, currpos);
        }
        // sets the preference for the next time the application starts up
        SharedPreferences settings = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("directoryLocation", directoryLocation);
        editor.commit();
        //doUnbindService();
        if(isRegistered) {
            unregisterReceiver(myNoisyAudioStreamReceiver);
            isRegistered = false;
        }/*
        Log.d(DEBUG_TAG, "destroying activity");
        Log.d(DEBUG_TAG, "stopped " + isMyServiceRunning());
        Log.d(DEBUG_TAG, "service stopped");*/

    }

    private OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            thePlayerMediaService.seek(seekBar.getProgress());
            currentTrackPositionField.setText(convertToTime(seekBar.getProgress()));
            int currpos = saveTrackState();
            updateTrackBeanState(currentSongListIndex, TrackBean.TRACK_STATUS_CURRENT, currpos);
        }
    };

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.arg1) {
                case ThePlayerMediaService.COMPLETE_MESSAGE:
//                    Toast.makeText(ThePlayerActivity.this, "Track Completed",
//                            Toast.LENGTH_LONG).show();
                    updateTrackBeanState(currentSongListIndex, TrackBean.TRACK_STATUS_FINISHED, 0);
                    currentSongListIndex++;
                    //currentTrackPosition = 0;
                    getNextTrackToPlay(currentSongListIndex);
                    break;
                case ThePlayerMediaService.SHUTDOWN_MESSAGE:
                    stopApplication();
                    break;
                case ThePlayerMediaService.PAUSE_MESSAGE:
                    updateTrackBeanState(currentSongListIndex, TrackBean.TRACK_STATUS_CURRENT, ThePlayerActivity.this.thePlayerMediaService.getCurrentPosition());
                    ThePlayerActivity.this.playButton.setImageResource(android.R.drawable.ic_media_play);
                    isPlaying = false;
                    break;
                case ThePlayerMediaService.PLAY_MESSAGE:
                    ThePlayerActivity.this.playButton.setImageResource(android.R.drawable.ic_media_pause);
                    isPlaying = true;
                    seekBar.setClickable(true);
                    myHandler.postDelayed(UpdateSongTime, 100);
                    break;
                default:
                    Log.d(DEBUG_TAG, "Unknown MessageID: " + msg.arg1);
            }
        }
    };

    private class NoisyAudioStreamReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // Pause the playback
                if (isPlaying) {
                    Log.d(DEBUG_TAG,"Pausing playback");
                    play(null);
                }
            }
        }
    }

    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            AudioManager audioManager =
                    (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            switch (focusChange) {

                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Log.d(DEBUG_TAG,"Audiofocus_loss_transient");
                    streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
                    //if (isPlaying)
                    //    play(null);
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.d(DEBUG_TAG,"Audiofocus_gain");
                    // Resume playback
                    //only resume playback if it is playing
                    if(!isPlaying){
                        Log.d(DEBUG_TAG,"it is not playing ignoring");
                    }else {
                        Log.d(DEBUG_TAG,"it is playing pausing");
                        //play(null);
                    }
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,streamVolume,AudioManager.AUDIOFOCUS_GAIN);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    Log.d(DEBUG_TAG,"Audiofocus_loss");
                    streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,AudioManager.AUDIOFOCUS_LOSS);
                    if (isPlaying)
                        play(null);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    Log.d(DEBUG_TAG,"Audiofocus_loss_transient_can_duck");
                    streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
                    break;
            }
        }
    };

    /**
     * Stops the Application
     */
    private void stopApplication() {

        thePlayerMediaService.pause();
        thePlayerMediaService.stopForeground(true);
        doUnbindService();
        //stopService(new Intent(this, ThePlayerMediaService.class));//.setAction("shutdown"),ThePlayerMediaService.SHUTDOWN_MESSAGE,0);
        updateDatabase();
        this.finish();
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static String formatSize(long size) {
        double tmpsize;
        String returnString = "0.0GB";
        if (size < 900000000) {
            tmpsize = (double) size / 1000000;
            returnString = String.format(Locale.getDefault(), "%3.2fMB",
                    tmpsize);
        } else {
            tmpsize = (double) size / 1000000000;
            returnString = String.format(Locale.getDefault(), "%3.2fGB",
                    tmpsize);
        }
        return returnString;
    }

    public static long getDirectorySize(File directory) {
        long directorySize = 0;
        File[] list = directory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase(Locale.US);
                if (lowercaseName.endsWith(".mp3")) {
                    return true;
                } else {
                    return false;
                }
            }
        });
        for (File file : list) {
            directorySize += file.length();
        }
        return directorySize;
    }

}
