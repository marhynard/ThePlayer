package com.fritzbang.theplayer;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FeedLinkInformationTask extends AsyncTask<Context, Void, Void> {
    private static final String DEBUG_TAG = "FeedLinkInformation";

    // TODO add an icon that shows that it is still refreshing
    // TODO investigate the status bar for refreshing and downloading

    @Override
    protected Void doInBackground(Context... context) {

        // Connects to the database and pulls out all the links updating the
        // urls
        DBAdapter db = new DBAdapter(context[0]);
        db.open();

        Cursor cs = db.getAllRSSLinks();
        cs.moveToFirst();
        while (!cs.isAfterLast()) {
            int currentRSSID = cs.getInt(0);
            String currentRSS = cs.getString(1);
            Log.d(DEBUG_TAG, currentRSS);
            try {
                parseUrl(currentRSSID, currentRSS, db);
            } catch (IOException e) {
                Log.d(DEBUG_TAG, "IOException: " + currentRSS);
            } catch (XmlPullParserException e) {
                Log.d(DEBUG_TAG, "XMLParsing Error: " + currentRSS);
            }

            cs.moveToNext();
        }
        db.close();
        return null;
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the rss feed content as an InputStream
    private void parseUrl(int myRSSID, String myurl, DBAdapter db)
            throws IOException, XmlPullParserException {
        InputStream is = null;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);
            is = conn.getInputStream();

            XMLHandler xmlHandler = new XMLHandler();
            xmlHandler.parse(is);
            Log.d(DEBUG_TAG, myRSSID + " Title: " + xmlHandler.podcastTitle);
            Log.d(DEBUG_TAG, "NumberItems: " + xmlHandler.podEntries.size());

            // TODO update the podcast information
            Log.d(DEBUG_TAG, xmlHandler.podcastTitle);
            Log.d(DEBUG_TAG, xmlHandler.imageLink);
            Log.d(DEBUG_TAG, xmlHandler.height);
            Log.d(DEBUG_TAG, xmlHandler.width);
            String localImageLink = getImage(xmlHandler.imageLink);
            db.updateRSSFeedInfo(myRSSID, xmlHandler.podcastTitle,
                    xmlHandler.imageLink, localImageLink, xmlHandler.height,
                    xmlHandler.width);
            // TODO add the new podcast files to the database only if they don't
            // exist already

            for (int x = 0; x < xmlHandler.podEntries.size(); x++) {
                PodcastEntry entry = (PodcastEntry) xmlHandler.podEntries
                        .get(x);
                db.insertEpisode(myRSSID, entry.title, entry.link,
                        entry.length, entry.pubDate);

            }

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }

    }

    private String getImage(String imageLink) {
        return DownloadManager.DownloadFromUrl(null, imageLink);

    }

    protected void onPostExecute(String result) {

    }

}
