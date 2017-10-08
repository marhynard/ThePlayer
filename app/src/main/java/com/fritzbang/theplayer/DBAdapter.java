package com.fritzbang.theplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DBAdapter {

	private static final String DEBUG_TAG = "DBAdapter";

	// These are the columns of the track_info table
    public static final String KEY_TRACK_LOCATION       = "track_location";
    public static final String KEY_TRACK_TITLE          = "track_title";
    public static final String KEY_TRACK_ARTIST         = "track_artist";
    public static final String KEY_TRACK_ALBUM          = "track_album";
    public static final String KEY_TRACK_STATUS         = "track_status";// finished playing - partial played - currently playing - not played
    public static final String KEY_TRACK_POSITION       = "track_position"; //current position for the track - possibly 0 if not played -1 if finished
    public static final String KEY_TRACK_TIMES_PLAYED   = "track_times_played"; //number of times the track has been played
    public static final String KEY_TRACK_RATING         = "track_rating"; //rating of the track 0-5 0=not rated 5 best

	public static final String TAG = "DBAdapter";
	public static final String DATABASE_NAME = "player";

	// table names
	public static final String DATABASE_TABLE_TRACK_INFO = "track_info";

	public static final int DATABASE_VERSION = 3;

	// Table creation statements
	public static final String CREATE_TRACK_INFO_DATABASE = "create table " + DATABASE_TABLE_TRACK_INFO + " ( "
        + KEY_TRACK_LOCATION + " text unique, " + KEY_TRACK_TITLE + " text, " + KEY_TRACK_ARTIST + " text, "
            + KEY_TRACK_ALBUM + " text, " + KEY_TRACK_STATUS + " int, " + KEY_TRACK_POSITION + " int, "
            + KEY_TRACK_TIMES_PLAYED + " int default 0, " + KEY_TRACK_RATING + " int default 0" +");";

	// Gets all information from table
	private final String TRACK_INFO_QUERY = "SELECT * FROM " + DATABASE_TABLE_TRACK_INFO;
    private final String TRACK_LOCATION_QUERY = "SELECT " + KEY_TRACK_LOCATION +" FROM " + DATABASE_TABLE_TRACK_INFO;

    // Gets the track currently playing
    private final String CURRENT_TRACK_QUERY = "SELECT * FROM " + DATABASE_TABLE_TRACK_INFO + " WHERE " + KEY_TRACK_STATUS + "=1";

	private final Context context;
	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;

	// TODO: Change statements so I don't get the warnings and i make sure that
	// everything is entered

	public DBAdapter(Context ctx) {
		this.context = ctx;
		DBHelper = new DatabaseHelper(context);
	}

    public TrackBean getCurrentTrack() {
        Cursor cs = db.rawQuery(TRACK_INFO_QUERY + " where track_status=" + TrackBean.TRACK_STATUS_CURRENT, new String[] {});
        if(cs.getCount() >= 1) {
            cs.moveToFirst();
            Log.d(DEBUG_TAG, "Number of Rows: " + cs.getCount());
            TrackBean returnBean = new TrackBean();
            returnBean.location = cs.getString(cs.getColumnIndex(KEY_TRACK_LOCATION));
            returnBean.position = cs.getInt(cs.getColumnIndex(KEY_TRACK_POSITION));
            returnBean.status = cs.getInt(cs.getColumnIndex(KEY_TRACK_STATUS));
            returnBean.album = cs.getString(cs.getColumnIndex(KEY_TRACK_ALBUM));
            returnBean.artist = cs.getString(cs.getColumnIndex(KEY_TRACK_ARTIST));
            returnBean.trackTitle = cs.getString(cs.getColumnIndex(KEY_TRACK_TITLE));
            return returnBean;
        }else
            return null;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TRACK_INFO_DATABASE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + " ,Which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_TRACK_INFO);
			onCreate(db);
		}
	}

	// ---opens the database---
	public DBAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}

	public boolean isOpen() {
		return db.isOpen();
	}

	// ---closes the database---
	public void close() {
		DBHelper.close();
	}

    public long insertTrackInfo(String trackTitle, String trackPath, String trackArtist, String trackAlbum, int trackStatus, int trackPosition){
        //Log.d(DEBUG_TAG,"Inserting: " + trackTitle);
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TRACK_TITLE, trackTitle);
        initialValues.put(KEY_TRACK_LOCATION, trackPath);
        initialValues.put(KEY_TRACK_ARTIST, trackArtist);
        initialValues.put(KEY_TRACK_ALBUM, trackAlbum);
        initialValues.put(KEY_TRACK_STATUS, trackStatus);
        initialValues.put(KEY_TRACK_POSITION, trackPosition);
        long returnValue = -1;
        try{
            returnValue = db.insertOrThrow(DATABASE_TABLE_TRACK_INFO, null, initialValues);
        }catch(Exception ex){
            returnValue = -1;
        }
        return returnValue;
    }
    public boolean delete(String path) {
        return db.delete(DATABASE_TABLE_TRACK_INFO, KEY_TRACK_LOCATION + "=\"" + path + "\"", null) > 0;
    }
    public void removeTrackInfo(){

    }
	public String[] getTrackInfo(String trackLocation) {

		Log.d(DEBUG_TAG, "Episode ID: " + trackLocation);

		Cursor cs = db.rawQuery(TRACK_INFO_QUERY, new String[] {});
		cs.moveToFirst();
		Log.d(DEBUG_TAG, "Number of Rows: " + cs.getCount());
		String[] results = new String[2];
		results[0] = cs.getString(cs.getColumnIndex(KEY_TRACK_POSITION));
		results[1] = cs.getString(cs.getColumnIndex(KEY_TRACK_LOCATION));

		return results;
	}
    public ArrayList<String> getLocations() {
        Cursor cs = db.rawQuery(TRACK_LOCATION_QUERY, null);
        ArrayList<String> returnList = new ArrayList<String>();
        cs.moveToFirst();
        while (!cs.isAfterLast()) {
            String location = cs.getString(cs.getColumnIndex(DBAdapter.KEY_TRACK_LOCATION));
            returnList.add(location);
            cs.moveToNext();
        }
        return returnList;
    }
    public Cursor getAllTrackInfo(){
        return db.rawQuery(TRACK_INFO_QUERY, null);
    }

    public boolean updateStatusInfo(TrackBean item) {
        ContentValues args = new ContentValues();
        args.put(KEY_TRACK_STATUS, item.status);
        args.put(KEY_TRACK_POSITION, item.position);

        return db.update(DATABASE_TABLE_TRACK_INFO, args, KEY_TRACK_LOCATION + "=\"" + item.location + "\"",
                null) > 0;
    }
}
