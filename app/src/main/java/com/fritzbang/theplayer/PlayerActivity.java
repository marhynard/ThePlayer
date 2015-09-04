package com.fritzbang.theplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class PlayerActivity extends Activity {

    // TODO add code to disable sound when disconnected
    // TODO add the ability to reorder the playlist

    // TODO speed up the update
    // TODO update the view to remove from the playlist

    private static final String DEBUG_TAG = "PlayerActivity";

    // This contains the filenames that will be played it may end up being
    // something else later on.
    public static ArrayList<String> playList = new ArrayList<String>();
    Context context;

    MediaPlayer mplayer = null;
    Button playButton;
    Button backButton;
    Button forwardButton;
    SeekBar seekBar;
    TextView episodeTitle;
    TextView timeView;

    int currentPosition = -1;
    String episodeTitleText;
    String episodeLink;

    boolean playing = false;

    ArrayList<String> playlistLocation = new ArrayList<String>();
    ArrayList<String> episodeIDs = new ArrayList<String>();
    ArrayList<String> episodePosition = new ArrayList<String>();
    // This will listen and handle the changes for the seek bar
    SeekBar.OnSeekBarChangeListener sListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            if (fromUser) {
                mplayer.seekTo(progress);
                seekBar.setProgress(progress);
            }
        }
    };
    private Runnable myThread = new Runnable() {

        @Override
        public void run() {
            boolean stillPlaying = true;
            while (stillPlaying) {

                try {
                    if (playing) {
                        seekBar.setProgress(mplayer.getCurrentPosition());
                        runOnUiThread(new Runnable() {
                            public void run() {
                                setTime(mplayer.getCurrentPosition(),
                                        mplayer.getDuration());
                            }
                        });

                    }
                } catch (Throwable t) {
                    Log.d(DEBUG_TAG, "got an exception: " + t.getMessage());
                }
                if (mplayer.getCurrentPosition() >= mplayer.getDuration()) {

                    DBAdapter db = new DBAdapter(context);
                    db.open();
                    String[] nextPositionInfo = db
                            .deletePlaylistEntry(currentPosition);
                    Log.d(DEBUG_TAG, "CurrentPosition: " + currentPosition
                            + " NextPosition: " + nextPositionInfo[0]
                            + " episodeid: " + nextPositionInfo[1]);
                    db.close();
                    if (nextPositionInfo[0] == null) {
                        Log.d(DEBUG_TAG, "done playing");
                        stillPlaying = false;
                    } else {
                        currentPosition = Integer.parseInt(nextPositionInfo[0]);
                        episodeTitleText = nextPositionInfo[2];
                        episodeLink = nextPositionInfo[3];
                        runOnUiThread(new Runnable() {
                            public void run() {
                                episodeTitle.setText(episodeTitleText);

                            }
                        });
                        prepareMediaPlayer(episodeLink);
                        mplayer.start();
                    }
                }
            }

            playing = false;
            // TODO reset the buttons to the original state
        }

    };
    // Handles the player buttons
    View.OnClickListener mListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.buttonPlay:
                    if (mplayer == null) {
                        Log.d(DEBUG_TAG, "Media is empty");
                        Toast.makeText(context, "No File has been selected",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        if (!playing) {
                            mplayer.start();
                            playing = true;
                            playButton.setText("Pause");
                        } else {
                            mplayer.pause();
                            playing = false;
                            playButton.setText("Play");
                        }
                        new Thread(myThread).start();
                    }
                    break;
                case R.id.buttonBack:
                    int dur = mplayer.getCurrentPosition();
                    int pos = (dur > 10000 ? dur - 5000 : 0);
                    mplayer.seekTo(pos);
                    break;
                case R.id.buttonForward:
                    int curpos = mplayer.getCurrentPosition();
                    int dur2 = mplayer.getDuration();
                    int pos2 = (curpos + 5000 > dur2 ? dur2 : curpos + 5000);
                    mplayer.seekTo(pos2);
                    break;
            }

        }
    };

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.player_view);
        context = getApplicationContext();
        Log.d(DEBUG_TAG, "before creation");

        episodeTitle = (TextView) findViewById(R.id.textViewEpisodeTitle);

        playButton = (Button) findViewById(R.id.buttonPlay);
        playButton.setOnClickListener(mListener);
        backButton = (Button) findViewById(R.id.buttonBack);
        backButton.setOnClickListener(mListener);
        forwardButton = (Button) findViewById(R.id.buttonForward);
        forwardButton.setOnClickListener(mListener);
        Log.d(DEBUG_TAG, "all set");

        seekBar = (SeekBar) findViewById(R.id.seekBar1);
        seekBar.setProgress(0);
        seekBar.setOnSeekBarChangeListener(sListener);

        Log.d(DEBUG_TAG, "progress started");

        timeView = (TextView) findViewById(R.id.textViewTime);

        ListView playlistView = (ListView) findViewById(R.id.listViewPlaylist);
        DBAdapter db = new DBAdapter(this);
        db.open();
        Cursor cs = db.getPlaylist();

        Log.d(DEBUG_TAG, "Count: " + cs.getCount());

        cs.moveToFirst();
        // loop through the cursor
        while (!cs.isAfterLast()) {

            episodeIDs.add(cs.getString(cs
                    .getColumnIndex(DBAdapter.KEY_EPISODE_ID)));
            playlistLocation.add(cs.getString(cs
                    .getColumnIndex(DBAdapter.KEY_PLAYLIST_POSITION)));
            episodePosition.add(cs.getString(cs
                    .getColumnIndex(DBAdapter.KEY_EPISODE_POSITION)));

            cs.moveToNext();
        }

        db.close();

        playlistView.setAdapter(new PodcastPlaylistArrayAdapter(this, episodeIDs
                .toArray(new String[episodeIDs.size()]), playlistLocation
                .toArray(new String[playlistLocation.size()]), episodePosition
                .toArray(new String[episodePosition.size()])));

        playlistView.setClickable(true);

        playlistView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.d(DEBUG_TAG, "Item is clicked:" + id + " " + position);

                // String filename = view.getTag(R.id.TAG_EPISODE_LOCATION);
                String epTitle = ((TextView) view
                        .findViewById(R.id.podcast_episode_title)).getText()
                        .toString();
                episodeTitle.setText(epTitle);

                String episodeLocation = (String) ((TextView) view
                        .findViewById(R.id.podcast_episode_title))
                        .getTag(R.id.TAG_EPISODE_LOCATION);
                currentPosition = Integer.parseInt((String) ((TextView) view
                        .findViewById(R.id.podcast_episode_title))
                        .getTag(R.id.TAG_PLAYLIST_POSITION));
                Log.d(DEBUG_TAG, "Location: " + episodeLocation);
                prepareMediaPlayer(episodeLocation);

            }
        });

    }

    private void prepareMediaPlayer(String filename) {
        Uri uri = Uri.parse(filename);
        mplayer = MediaPlayer.create(getApplicationContext(), uri);
        seekBar.setMax(mplayer.getDuration());
        setTime(mplayer.getCurrentPosition(), mplayer.getDuration());

    }

    private String convertToTime(int duration) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_down_low, menu);
        return super.onCreateOptionsMenu(menu);
    }

    protected void setTime(int currentPosition, int duration) {
        timeView.setText(convertToTime(currentPosition) + "/"
                + convertToTime(duration));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_add:
                Log.d(DEBUG_TAG, "add_action");

                Intent addpodcastintent = new Intent();
                addpodcastintent.setClassName("com.fritzbang.downlow",
                        "com.fritzbang.downlow.AddPodcastActivity");
                startActivity(addpodcastintent);

                return true;
            case R.id.action_refresh:
                Log.d(DEBUG_TAG, "refresh action");
                new FeedLinkInformationTask().execute(getApplicationContext());

                return true;
            case R.id.action_settings:
                Log.d(DEBUG_TAG, "settings action");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
