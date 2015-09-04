package com.fritzbang.theplayer;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PodcastPlaylistArrayAdapter extends ArrayAdapter<String> {

    private static final String DEBUG_TAG = "PlaylistArrayAdapter";
    private final Activity context;
    private final String[] episodeIds;
    private final String[] playlistLocation;
    private final String[] episodePosition;

    public PodcastPlaylistArrayAdapter(Activity context, String[] episodeIds,
                                String[] playlistLocation, String[] episodePosition) {
        super(context, R.layout.playlist_list_entry, episodeIds);
        this.context = context;
        this.episodeIds = episodeIds;
        this.playlistLocation = playlistLocation;
        this.episodePosition = episodePosition;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.playlist_podcast_list_entry, null);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.episodeTitle = (TextView) rowView
                    .findViewById(R.id.podcast_episode_title);
            viewHolder.podcastTitle = (TextView) rowView
                    .findViewById(R.id.podcast_title);
            viewHolder.playingPercent = (TextView) rowView
                    .findViewById(R.id.playing_percent);

            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();

        DBAdapter db = new DBAdapter(context);
        db.open();
        String[] episodeInfo = db.getEpisodeInfo(episodeIds[position]);
        db.close();

        String podcastTitle = episodeInfo[0];
        String episodeTitle = episodeInfo[1];
        String episodeLocation = episodeInfo[2];
        String playingPercent = episodeInfo[3];// this needs to be converted to
        // percent

        Log.d(DEBUG_TAG, "episodeTitle: " + episodeTitle);
        Log.d(DEBUG_TAG, "episodeLocation: " + episodeLocation);

        holder.episodeTitle.setText(episodeTitle);
        holder.episodeTitle.setTag(R.id.TAG_EPISODE_ID, episodeIds[position]);
        holder.episodeTitle.setTag(R.id.TAG_EPISODE_LOCATION, episodeLocation);
        holder.episodeTitle.setTag(R.id.TAG_PLAYLIST_POSITION,
                playlistLocation[position]);
        holder.episodeTitle.setTag(R.id.TAG_EPISODE_POSITION,
                episodePosition[position]);
        holder.podcastTitle.setText(podcastTitle);
        holder.playingPercent.setText(playingPercent);
        return rowView;
    }

    static class ViewHolder {
        public TextView episodeTitle;
        public TextView podcastTitle;
        public TextView playingPercent;
    }
}
