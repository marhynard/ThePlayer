package com.fritzbang.theplayer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

public class DeleteTrackTask extends AsyncTask<File, Void, Void> {
	private static final String DEBUG_TAG = "DeleteTrackTask";
    Context context;
    TextView textViewSpace;
    String spaceAvailable = "";
	public DeleteTrackTask(Context context,TextView textViewSpace) {
        this.context = context;
        this.textViewSpace = textViewSpace;

	}

	@Override
	protected Void doInBackground(File... params) {
		File fileToDelete = (File) params[0];
        File parentDir = fileToDelete.getParentFile();

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
        spaceAvailable = ThePlayerActivity.updateSpaceAvailable(parentDir);
        publishProgress();

		return null;
	}
    @Override
    public void onProgressUpdate(Void... ints) {
        textViewSpace.setText(spaceAvailable);
    }
	protected void onPostExecute(String result) {

	}

}