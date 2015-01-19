package com.fritzbang.theplayer;

public class TrackBean {
    public static final int TRACK_STATUS_UNPLAYED = 0;
    public static final int TRACK_STATUS_CURRENT = 1;
    public static final int TRACK_STATUS_FINISHED = 2;
    public static final int TRACK_STATUS_PARTIAL = 3;

	public String trackTitle;
	public String location;
	public String artist;
	public String album;
    public int status = 0;
    public int position = 0;

	TrackBean(String trackTitle, String location, String artist, String album,int status,int position) {
		this.trackTitle = trackTitle;
		this.location = location;
		this.artist = artist;
		this.album = album;
        this.status = status;
        this.position = position;
	}

	public TrackBean() {
	}
}