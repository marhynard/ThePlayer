package com.fritzbang.theplayer;

import java.io.File;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class DeleteTrackTask extends AsyncTask<File, Void, Void> {
	private static final String DEBUG_TAG = "DeleteTrackTask";
    Context context;
	public DeleteTrackTask(Context context) {
        this.context = context;
	}

	@Override
	protected Void doInBackground(File... params) {
		File fileToDelete = (File) params[0];
		Log.d(DEBUG_TAG, "File to delete: " + fileToDelete);
        String errorMessage = null;
		if (!fileToDelete.delete()) {
			errorMessage = "failed to delete from device";
		}
        DBAdapter db = new DBAdapter(context);
        db.open();
        if(!db.delete(fileToDelete.getPath())){
           errorMessage += " and database";
        }
        db.close();

        if(errorMessage != null){
            Log.e(DEBUG_TAG,"There was an error: " + errorMessage);
        }else
            Log.i(DEBUG_TAG, "File was deleted : " + fileToDelete);
		return null;
	}

	protected void onPostExecute(String result) {
	}
}