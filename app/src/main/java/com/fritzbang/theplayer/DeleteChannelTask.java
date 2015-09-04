package com.fritzbang.theplayer;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class DeleteChannelTask extends AsyncTask<Object, Void, Void> {
    private static final String DEBUG_TAG = "DeleteChannelTask";
    Context appContext;

    @Override
    protected Void doInBackground(Object... object) {
        appContext = (Context) object[0];
        // Connects to the database and pulls out all the links updating the
        // urls
        String rssID = (String) object[1];
        Log.d(DEBUG_TAG, "rssId: " + rssID);
        long rssId = Long.parseLong(rssID);
        DBAdapter db = new DBAdapter(appContext);
        db.open();
        if (db.deleteRssLink(rssId))
            Log.d(DEBUG_TAG, "rssId: " + rssID
                    + " was deleted from rsslink table");
        if (db.deleteEpisodeInfo(rssId))
            Log.d(DEBUG_TAG, "rssId: " + rssID
                    + " was deleted from episodeinfo table");
        db.close();
        return null;
    }

    protected void onPostExecute(String result) {

    }

}
