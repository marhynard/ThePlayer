package com.fritzbang.theplayer;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class DownloadArrayAdapter extends ArrayAdapter<String> {
    // TODO add the functionality that puts the information from downloaded
    // podcasts into a list
    // TODO make the list selectable
    // TODO add ability to delete from list

    private static final String DEBUG_TAG = "DownloadArrayAdapter";
    private final Activity context;
    private final String[] episodeTitles;
    private final String[] podcastTitles;
    private final String[] rssIds;
    private final String[] episodeIds;
    private final String[] episodeLinks;
    // private final String[] pubDates;
    private final String[] downloadeds;
    private final String[] listeneds;

    public DownloadArrayAdapter(Activity context, String[] episodeTitles,
                                String[] podcastTitles, String[] rssIds, String[] episodeLinks,
                                String[] downloadeds, String[] listeneds, String[] episodeIds) {
        super(context, R.layout.download_list_entry, episodeTitles);
        this.context = context;
        this.episodeTitles = episodeTitles;
        this.podcastTitles = podcastTitles;
        this.rssIds = rssIds;
        this.episodeIds = episodeIds;
        this.episodeLinks = episodeLinks;
        // this.pubDates = pubDates;
        this.downloadeds = downloadeds;
        this.listeneds = listeneds;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.download_list_entry, null);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.episodeTitle = (TextView) rowView
                    .findViewById(R.id.podcast_episode_title);
            viewHolder.podcastTitle = (TextView) rowView
                    .findViewById(R.id.podcast_title);
            viewHolder.inPlaylist = (TextView) rowView
                    .findViewById(R.id.in_playlist);
            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();
        String episodeTitle = episodeTitles[position];
        String podcastTitle = podcastTitles[position];

        Log.d(DEBUG_TAG, "episodeTitle: " + episodeTitle + " "
                + episodeIds[position] + " " + position);

        holder.episodeTitle.setText(episodeTitle);
        holder.episodeTitle.setTag(R.id.TAG_EPISODE_ID, episodeIds[position]);
        holder.episodeTitle.setTag(R.id.TAG_EPISODE_LINK_ID,
                episodeLinks[position]);
        holder.podcastTitle.setText(podcastTitle);

        DBAdapter db = new DBAdapter(context);
        db.open();
        if (db.isInPlaylist(episodeIds[position])) {
            holder.inPlaylist.setText("in Playlist");
        }
        db.close();

        return rowView;
    }

    static class ViewHolder {
        public TextView episodeTitle;
        public TextView podcastTitle;
        public TextView completed;
        public TextView inPlaylist;
    }

}
