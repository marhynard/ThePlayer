package com.fritzbang.theplayer;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class MainDownLowActivity extends TabActivity {

    private static final String DEBUG_TAG = "MainDownLowActivity";

    // TODO clean up code and put in comments
    // TODO remove auto-generated TODO statements

    // TODO check to see if storage location is available

    // TODO add ability to search for podcasts
    // TODO check out gpodder service for searching for podcasts

    // TODO create view for downloads

    // TODO create view to manage podcasts(subscribe import and find)

    // TODO create activity to clear the rss feed cache

    // TODO create a settings menu

    // TODO fix the tab menu so the lettering fits
    // TODO eliminate the depricated functions ie TabActivity
    // TODO create the fragment handler and add the various tab activities as
    // fragments
    // TODO look at the fragment example in the coursera android course for
    // help.

    // TODO need to be able to update the views on database change

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_down_low);
        // setContentView(R.layout.main_down_low_tabs);

        // Resources resources = getResources();
        TabHost tabHost = getTabHost();

        Intent intentChannel = new Intent().setClass(this,
                ChannelActivity.class);
        TabSpec tabSpecChannel = tabHost.newTabSpec("Channel")
                .setIndicator("Channel").setContent(intentChannel);

        Intent intentNew = new Intent().setClass(this, NewActivity.class);
        TabSpec tabSpecNew = tabHost.newTabSpec("New").setIndicator("New")
                .setContent(intentNew);

        Intent intentPlayer = new Intent().setClass(this, PlayerActivity.class);
        TabSpec tabSpecPlayer = tabHost.newTabSpec("Player")
                .setIndicator("Player").setContent(intentPlayer);

        Intent intentDownload = new Intent().setClass(this,
                DownloadActivity.class);
        TabSpec tabSpecDownload = tabHost.newTabSpec("Download")
                .setIndicator("Download").setContent(intentDownload);

        tabHost.addTab(tabSpecChannel);
        tabHost.addTab(tabSpecNew);
        tabHost.addTab(tabSpecPlayer);
        tabHost.addTab(tabSpecDownload);
        tabHost.setCurrentTab(0);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_down_low, menu);
        return super.onCreateOptionsMenu(menu);
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
