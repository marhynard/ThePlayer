package com.fritzbang.theplayer;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView;

import java.util.ArrayList;

public class NewActivity extends ListActivity {
    private static final String DEBUG_TAG = "NewActivity";

    private String selectedEpisodeId = "-1";
    private String selectedEpisodeLink = "-1";

    // TODO refresh the screen once something has been changed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayList<String> podcastTitles = new ArrayList<String>();
        ArrayList<String> episodeTitle = new ArrayList<String>();
        ArrayList<String> pubDate = new ArrayList<String>();
        ArrayList<String> downloaded = new ArrayList<String>();
        ArrayList<String> listened = new ArrayList<String>();
        ArrayList<String> rssIDs = new ArrayList<String>();
        ArrayList<String> episodeIDs = new ArrayList<String>();
        ArrayList<String> episodeLinks = new ArrayList<String>();
        ArrayList<String> lengths = new ArrayList<String>();
        ArrayList<String> locations = new ArrayList<String>();

        DBAdapter db = new DBAdapter(this);
        db.open();
        Cursor cs = db.getAllPodcastsInfo();

        Log.d(DEBUG_TAG, "Count: " + cs.getCount());

        cs.moveToFirst();
        while (!cs.isAfterLast()) {

            podcastTitles.add(cs.getString(cs
                    .getColumnIndex(DBAdapter.KEY_PODCAST_TITLE)));
            episodeTitle.add(cs.getString(cs
                    .getColumnIndex(DBAdapter.KEY_EPISODE_TITLE)));
            pubDate.add(cs.getString(cs.getColumnIndex(DBAdapter.KEY_PUBDATE)));
            downloaded.add(cs.getString(cs
                    .getColumnIndex(DBAdapter.KEY_DOWNLOADED)));
            listened.add(cs.getString(cs.getColumnIndex(DBAdapter.KEY_LISTENED)));
            rssIDs.add(cs.getString(cs.getColumnIndex(DBAdapter.KEY_RSS_ID)));
            episodeIDs.add(cs.getString(cs
                    .getColumnIndex(DBAdapter.KEY_EPISODE_ID)));
            episodeLinks.add(cs.getString(cs
                    .getColumnIndex(DBAdapter.KEY_EPISODE_LINK)));
            lengths.add(cs.getString(cs.getColumnIndex(DBAdapter.KEY_LENGTH)));
            locations.add(cs.getString(cs
                    .getColumnIndex(DBAdapter.KEY_LOCATION)));

            cs.moveToNext();
        }

        db.close();

        setListAdapter(new EpisodeArrayAdapter(this,
                episodeTitle.toArray(new String[podcastTitles.size()]),
                podcastTitles.toArray(new String[podcastTitles.size()]),
                episodeIDs.toArray(new String[episodeIDs.size()]),
                episodeLinks.toArray(new String[episodeLinks.size()]),
                pubDate.toArray(new String[podcastTitles.size()]),
                downloaded.toArray(new String[podcastTitles.size()]),
                listened.toArray(new String[podcastTitles.size()])));

        this.getListView().setLongClickable(true);
        this.getListView().setClickable(true);
        this.getListView().setOnItemLongClickListener(
                new OnItemLongClickListener() {

                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent,
                                                   View view, int position, long id) {

                        Log.d(DEBUG_TAG, "Item is long clicked:" + id + " "
                                + position);
                        String textview1 = ((TextView) view
                                .findViewById(R.id.podcast_episode_title))
                                .getText().toString();
                        String episodeid = (String) ((TextView) view
                                .findViewById(R.id.podcast_episode_title))
                                .getTag(R.id.TAG_EPISODE_ID);
                        String episodeLink = (String) ((TextView) view
                                .findViewById(R.id.podcast_episode_title))
                                .getTag(R.id.TAG_EPISODE_LINK_ID);
                        Log.d(DEBUG_TAG, "Title: " + textview1 + " : "
                                + episodeid + " : " + episodeLink);
                        selectedEpisodeId = episodeid;
                        selectedEpisodeLink = episodeLink;
                        showPodcastAlert();

                        return true;
                    }
                });
        this.getListView().setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Log.d(DEBUG_TAG, "Item is clicked:" + id + " " + position);
                // TODO start activity that shows info for each episode

            }
        });

    }

    public void showPodcastAlert() {
        // TODO customize the dialog to give more options

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Podcast Dialog(find better name)");
        alertDialog.setMessage("How can I help you?");

        alertDialog.setPositiveButton("Download",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(DEBUG_TAG,
                                "Add to download task and start downloadtask");
                        // TODO check to see if it has already been downloaded
                        DownloadTask.downloadQueue.add(selectedEpisodeLink);
                        DownloadTask.downloadQueueEpisodeID
                                .add(selectedEpisodeId);
                        DownloadTask downloadTask = new DownloadTask();
                        downloadTask.execute(getApplicationContext());

                        dialog.cancel();
                    }
                });
        alertDialog.setNeutralButton("Mark As Listened",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO marks the episode as listened
                        Log.d(DEBUG_TAG, "Mark As Listened");
                        dialog.cancel();
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

}
