package com.fritzbang.theplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class AddPodcastActivity extends Activity {
    private static final String DEBUG_TAG = "AddPodcastActivity";

    // TODO add a check to make sure the url doesnt already exist in the
    // database
    String rssURL = null;
    // private Button refreshButton;
    private Button channelButton;
    private Button subscribeButton;
    private Button opmlButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_podcast);

        subscribeButton = (Button) findViewById(R.id.buttonSubscribe);
        subscribeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                EditText rssurlEditTextView = (EditText) findViewById(R.id.editTextRSSURL);
                // rssurlEditTextView.setText("http://192.168.56.101/songs.htm");
                rssURL = rssurlEditTextView.getText().toString();
                // rssURL = "http://feeds.feedburner.com/Coverville";
                // rssURL = "http://leo.am/podcasts/sn";
                // rssURL = "http://192.168.56.101/Coverville.htm";
                // rssURL = "http://192.168.56.101/songs.htm";

                boolean goodConnection = false;
                // Check to see if there is a network connection
                goodConnection = NetworkHandler
                        .checkNetworkConnection(getApplicationContext());
                if (goodConnection) {
                    new FeedLinkValidationTask().execute(rssURL);

                } else {
                    // If there is no connection let the user know
                    // TODO add a popup to send the user to the settings to
                    // enable it
                    Toast.makeText(getApplicationContext(),
                            "Check Network Connection", Toast.LENGTH_SHORT)
                            .show();
                }

            }
        });

        opmlButton = (Button) findViewById(R.id.buttonImport);
        opmlButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO add file chooser to load file
                // TODO parse file
                // TODO check to see it urls are valid
                // TODO insert into database
                ArrayList<String> URLs = new ArrayList<String>();
                for (int x = 0; x < URLs.size(); x++) {
                    String rssURL = (String) URLs.get(x);
                    new FeedLinkValidationTask().execute(rssURL);
                }
            }

        });

        channelButton = (Button) findViewById(R.id.buttonChannels);
        channelButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent channelintent = new Intent();
                channelintent.setClassName("com.fritzbang.downlow",
                        "com.fritzbang.downlow.ChannelActivity");
                startActivity(channelintent);
            }

        });

        Button newButton = (Button) findViewById(R.id.buttonNew);
        newButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent newintent = new Intent();
                newintent.setClassName("com.fritzbang.downlow",
                        "com.fritzbang.downlow.NewActivity");
                startActivity(newintent);
            }

        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        //inflater.inflate(R.menu.main_down_low, menu);
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

    private class FeedLinkValidationTask extends
            AsyncTask<String, Void, String> {
        private int response = -1;

        @Override
        protected String doInBackground(String... urls) {
            try {
                return verifyUrl(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve.  Feed Link may be invalid.";
            }
        }

        // Given a URL, establishes an HttpUrlConnection and retrieves
        // the web page content as a InputStream, which it returns as
        // a string.
        private String verifyUrl(String myurl) throws IOException {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            response = conn.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);

            return "URL is good: " + response;
        }

        protected void onPostExecute(String result) {

            if (response == 200) {
                // If the page exists the url will be added to the db
                DBAdapter db = new DBAdapter(getApplicationContext());
                db.open();
                db.insertRssLink(rssURL, null, null, null, null, null);
                db.close();
                Log.d(DEBUG_TAG, "The feed has been added");
                Toast.makeText(getApplicationContext(),
                        "The feed has been added", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(DEBUG_TAG, "The page doesn't exist: " + response);
                Toast.makeText(getApplicationContext(),
                        "The page doesn't exist: " + response,
                        Toast.LENGTH_SHORT).show();
            }

        }

    }
}
