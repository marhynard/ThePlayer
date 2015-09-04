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
    public static final String KEY_TRACK_LOCATION = "track_location";
    public static final String KEY_TRACK_TITLE = "track_title";
    public static final String KEY_TRACK_ARTIST = "track_artist";
    public static final String KEY_TRACK_ALBUM = "track_album";
    public static final String KEY_TRACK_STATUS = "track_status";// finished playing - partial played - currently playing - not played
    public static final String KEY_TRACK_POSITION = "track_position"; //current position for the track - possibly 0 if not played -1 if finished

    // These are the columns of the rss_info table
    public static final String KEY_RSS_ID = "rss_id";
    public static final String KEY_RSS_LINK = "rss_link";
    public static final String KEY_PODCAST_TITLE = "podcast_title";
    public static final String KEY_IMAGE_LINK = "image_link";
    public static final String KEY_LOCAL_IMAGE_LINK = "local_image_link";
    public static final String KEY_IMAGE_WIDTH = "image_width";
    public static final String KEY_IMAGE_HEIGHT = "image_height";
    // These are the columns of the episode_info table
    // This also includes rss_id as a foreign key
    public static final String KEY_EPISODE_ID = "episode_id";
    public static final String KEY_EPISODE_TITLE = "episode_title";
    public static final String KEY_EPISODE_LINK = "episode_link";// Location
    // where the downloaded file is located
    public static final String KEY_LENGTH = "length";// Length of the Episode
    public static final String KEY_PUBDATE = "pubDate";
    public static final String KEY_DOWNLOADED = "downloaded";
    public static final String KEY_LISTENED = "listened";
    public static final String KEY_LOCATION = "location";
    // These are the columns of the playlist table
    // This also includes episode_id as a foreign key
    public static final String KEY_PLAYLIST_POSITION = "playlist_position";
    public static final String KEY_EPISODE_POSITION = "episode_position";





	public static final String TAG = "DBAdapter";
	public static final String DATABASE_NAME = "player";

	// table names
	public static final String DATABASE_TABLE_TRACK_INFO = "track_info";
    // table names
    public static final String DATABASE_TABLE_RSS_INFO = "rss_info";
    public static final String DATABASE_TABLE_EPISODE_INFO = "episode_info";
    public static final String DATABASE_TABLE_PLAYLIST = "playlist";

    public static final int DATABASE_VERSION = 3;

	// Table creation statements
	public static final String CREATE_TRACK_INFO_DATABASE = "create table " + DATABASE_TABLE_TRACK_INFO + " ( "
        + KEY_TRACK_LOCATION + " text unique, " + KEY_TRACK_TITLE + " text, " + KEY_TRACK_ARTIST + " text, "
            + KEY_TRACK_ALBUM + " text, " + KEY_TRACK_STATUS + " int, " + KEY_TRACK_POSITION + " int);";

    public static final String DATABASE_CREATE_RSS_INFO = "create table rss_info (rss_id integer primary key autoincrement, rss_link text not null, podcast_title text,image_link text,local_image_link text,image_width integer,image_height integer);";
    public static final String DATABASE_CREATE_EPISODE_INFO = "create table episode_info (episode_id integer primary key autoincrement, rss_id integer, episode_title text, episode_link text, length text, pubDate text,downloaded boolean,listened real,location text);";
    public static final String DATABASE_CREATE_PLAYLIST = "create table playlist (playlist_position integer primary key autoincrement,episode_position integer,episode_id integer not null unique);";



	// Gets all information from table
	private final String TRACK_INFO_QUERY = "SELECT * FROM " + DATABASE_TABLE_TRACK_INFO;
    private final String TRACK_LOCATION_QUERY = "SELECT " + KEY_TRACK_LOCATION +" FROM " + DATABASE_TABLE_TRACK_INFO;

    // Gets the track currently playing
    private final String CURRENT_TRACK_QUERY = "SELECT * FROM " + DATABASE_TABLE_TRACK_INFO + " WHERE " + KEY_TRACK_STATUS + "=1";

    // Join to get all the fields for the episodes
    private final String EPISODE_QUERY = "SELECT * FROM rss_info a INNER JOIN episode_info b ON a.rss_id=b.rss_id";
    private final String PLAYLIST_QUERY = "SELECT * FROM rss_info a INNER JOIN episode_info b ON a.rss_id=b.rss_id WHERE b.episode_id = ?";

    // Statement for getting the information for the next episode from the list
    private final String NEXT_EPISODE_QUERY = "SELECT min( playlist_position),a.episode_id,episode_title,episode_link FROM playlist a INNER JOIN episode_info b ON a.episode_id=b.episode_id WHERE playlist_position > ?";


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

    // ---insert a link into the database---
    public long insertRssLink(String rss_link, String podcastTitle,
                              String image_link, String local_image_link, String imageWidth,
                              String imageHeight) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_RSS_LINK, rss_link);
        initialValues.put(KEY_PODCAST_TITLE, podcastTitle);
        initialValues.put(KEY_IMAGE_LINK, image_link);
        initialValues.put(KEY_LOCAL_IMAGE_LINK, local_image_link);
        initialValues.put(KEY_IMAGE_WIDTH, imageWidth);
        initialValues.put(KEY_IMAGE_HEIGHT, imageHeight);

        return db.insert(DATABASE_TABLE_RSS_INFO, null, initialValues);
    }

    // ---deletes a particular link---
    public boolean deleteRssLink(long rssId) {
        return db.delete(DATABASE_TABLE_RSS_INFO, KEY_RSS_ID + "=" + rssId,
                null) > 0;
    }

    // ---deletes a particular link---
    public boolean deleteRSSLink(String condition) {
        return db.delete(DATABASE_TABLE_RSS_INFO, condition, null) > 0;
    }

    // ---retrieves all the links---
    public Cursor getAllRSSLinks() {
        return db.query(DATABASE_TABLE_RSS_INFO, new String[]{KEY_RSS_ID,
                KEY_RSS_LINK, KEY_PODCAST_TITLE, KEY_IMAGE_LINK,
                KEY_LOCAL_IMAGE_LINK}, null, null, null, null, KEY_RSS_ID);
    }

    // ---retrieves all the links that meet the condition---
    public Cursor getAllRSSLinks(String condition) {
        return db.query(DATABASE_TABLE_RSS_INFO, new String[]{KEY_RSS_ID,
                KEY_RSS_LINK}, condition, null, null, null, KEY_RSS_LINK);
    }

    // ---retrieves a particular link---
    public Cursor getRSSLink(long rssId) throws SQLException {
        Cursor mCursor = db.query(true, DATABASE_TABLE_RSS_INFO, new String[]{
                        KEY_RSS_ID, KEY_RSS_LINK}, KEY_RSS_ID + "=" + rssId, null,
                null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public boolean updateRSSFeedInfo(int rssID, String podcastTitle,
                                     String imageLink, String local_image_link, String height,
                                     String width) {

        ContentValues args = new ContentValues();
        args.put(KEY_PODCAST_TITLE, podcastTitle);
        args.put(KEY_IMAGE_LINK, imageLink);
        args.put(KEY_LOCAL_IMAGE_LINK, local_image_link);
        args.put(KEY_IMAGE_HEIGHT, height);
        args.put(KEY_IMAGE_WIDTH, width);
        return db.update(DATABASE_TABLE_RSS_INFO, args, KEY_RSS_ID + "="
                + rssID, null) > 0;

    }

    public boolean updateEpisodeInfoDownload(String episodeID,
                                             String episodeLink, boolean inserted) {

        ContentValues args = new ContentValues();
        args.put(KEY_EPISODE_LINK, episodeLink);
        args.put(KEY_DOWNLOADED, inserted);
        return db.update(DATABASE_TABLE_EPISODE_INFO, args, KEY_EPISODE_ID
                + "=" + episodeID, null) > 0;

    }

    public long insertEpisode(int rssID, String title, String link,
                              String length, String pubDate) {

        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_RSS_ID, rssID);
        initialValues.put(KEY_EPISODE_TITLE, title);
        initialValues.put(KEY_EPISODE_LINK, link);
        initialValues.put(KEY_LENGTH, length);
        initialValues.put(KEY_PUBDATE, pubDate);
        initialValues.put(KEY_DOWNLOADED, false);
        initialValues.put(KEY_LISTENED, 0);
        initialValues.put(KEY_LOCATION, "");

        return db.insert(DATABASE_TABLE_EPISODE_INFO, null, initialValues);

    }

    // ---retrieves all the podcasts---
    public Cursor getAllPodcastsInfo() {
        // return db.query(DATABASE_TABLE_EPISODE_INFO, new String[] {
        // KEY_EPISODE_ID, KEY_RSS_ID, KEY_PODCAST_TITLE,
        // KEY_EPISODE_TITLE, KEY_EPISODE_LINK, KEY_LENGTH, KEY_PUBDATE,
        // KEY_DOWNLOADED, KEY_LISTENED, KEY_LOCATION }, null, null, null,
        // null, KEY_PUBDATE);

        // private final String MY_QUERY =
        // "SELECT * FROM table_a a INNER JOIN table_b b ON a.id=b.other_id WHERE b.property_id=?";

        // db.rawQuery(MY_QUERY, new String[]{String.valueOf(propertyId)});

        return db.rawQuery(EPISODE_QUERY, null);

    }

    public boolean deleteEpisodeInfo(long rssId) {
        return db.delete(DATABASE_TABLE_EPISODE_INFO, KEY_RSS_ID + "=" + rssId,
                null) > 0;
    }

    public boolean updatePlaylistEntry() {
        // TODO add code for updatePlaylistEntry
        return true;
    }

    public boolean resetPlaylist() {
        // TODO add code for resetPlaylist
        return true;
    }

    public Cursor getPlaylist() {
        return db.query(DATABASE_TABLE_PLAYLIST, new String[]{
                        KEY_PLAYLIST_POSITION, KEY_EPISODE_POSITION, KEY_EPISODE_ID},
                null, null, null, null, KEY_PLAYLIST_POSITION);
    }

    // TODO check the playlist insertion
    public long insertPlaylistEntry(String playListPosition,
                                    String playPosition, String episodeID) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_EPISODE_ID, episodeID);
        if (playPosition != null)
            initialValues.put(KEY_EPISODE_POSITION, playPosition);

        return db.insert(DATABASE_TABLE_PLAYLIST, null, initialValues);

    }

    public boolean deletePlaylistEntry() {
        // TODO Auto-generated method stub
        return false;

    }

    public String[] getEpisodeInfo(String episodeID) {

        Log.d(DEBUG_TAG, "Episode ID: " + episodeID);

        Cursor cs = db.rawQuery(PLAYLIST_QUERY, new String[]{episodeID});
        cs.moveToFirst();
        Log.d(DEBUG_TAG, "Number of Rows: " + cs.getCount());
        String[] results = new String[4];
        results[0] = cs.getString(cs.getColumnIndex(KEY_PODCAST_TITLE));
        results[1] = cs.getString(cs.getColumnIndex(KEY_EPISODE_TITLE));
        results[2] = cs.getString(cs.getColumnIndex(KEY_EPISODE_LINK));
        results[3] = cs.getString(cs.getColumnIndex(KEY_LENGTH));

        return results;
    }

    public boolean isInPlaylist(String id) {
        Cursor cs = db.query(DATABASE_TABLE_PLAYLIST,
                new String[]{KEY_EPISODE_ID}, KEY_EPISODE_ID + "=" + id,
                null, null, null, null);
        if (cs.getCount() > 0)
            return true;

        return false;
    }

    public String[] deletePlaylistEntry(int currentPosition) {
        String[] nextInfo = new String[4];
        db.delete(DATABASE_TABLE_PLAYLIST, KEY_PLAYLIST_POSITION + "="
                + currentPosition, null);

        // Cursor cs = db.query(DATABASE_TABLE_PLAYLIST, new String[] {
        // "min(" + KEY_PLAYLIST_POSITION + ")", KEY_EPISODE_ID },
        // KEY_PLAYLIST_POSITION + ">" + currentPosition, null, null,
        // null, null);
        Cursor cs = db.rawQuery(NEXT_EPISODE_QUERY,
                new String[]{currentPosition + ""});
        cs.moveToFirst();
        Log.d(DEBUG_TAG, "number results: " + cs.getCount());

        if (cs.getCount() > 0) {

            nextInfo[0] = cs.getString(0);// min( playlist_position)
            nextInfo[1] = cs.getString(1);// episode_id
            nextInfo[2] = cs.getString(2);// episode_title
            nextInfo[3] = cs.getString(3);// episode_link

            Log.d(DEBUG_TAG, "0: " + nextInfo[0]);
            Log.d(DEBUG_TAG, "1: " + nextInfo[1]);
            Log.d(DEBUG_TAG, "2: " + nextInfo[2]);
            Log.d(DEBUG_TAG, "3: " + nextInfo[3]);
        }
        return nextInfo;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TRACK_INFO_DATABASE);
            db.execSQL(DATABASE_CREATE_RSS_INFO);
            db.execSQL(DATABASE_CREATE_EPISODE_INFO);
            db.execSQL(DATABASE_CREATE_PLAYLIST);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + " ,Which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_TRACK_INFO);
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_RSS_INFO);
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_EPISODE_INFO);
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE_PLAYLIST);
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
