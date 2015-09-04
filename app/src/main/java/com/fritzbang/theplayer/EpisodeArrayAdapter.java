package com.fritzbang.theplayer;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class EpisodeArrayAdapter extends ArrayAdapter<String> {
    private static final String DEBUG_TAG = "EpisodeArrayAdapter";
    private final Activity context;
    private final String[] episodeTitles;
    private final String[] podcastTitles;
    private final String[] episodeIds;
    private final String[] episodeLinks;
    private final String[] pubDates;
    private final String[] downloadeds;
    private final String[] listeneds;

    public EpisodeArrayAdapter(Activity context, String[] episodeTitles,
                               String[] podcastTitles, String[] episodeIds, String[] episodeLinks,
                               String[] pubDates, String[] downloadeds, String[] listeneds) {
        super(context, R.layout.rss_list_entry, episodeTitles);
        this.context = context;
        this.episodeTitles = episodeTitles;
        this.podcastTitles = podcastTitles;
        this.episodeIds = episodeIds;
        this.episodeLinks = episodeLinks;
        this.pubDates = pubDates;
        this.downloadeds = downloadeds;
        this.listeneds = listeneds;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.episode_list_entry, null);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.episodeTitle = (TextView) rowView
                    .findViewById(R.id.podcast_episode_title);
            viewHolder.podcastTitle = (TextView) rowView
                    .findViewById(R.id.podcast_title);
            viewHolder.pubDate = (TextView) rowView
                    .findViewById(R.id.publish_date);
            viewHolder.inDownloads = (TextView) rowView
                    .findViewById(R.id.in_downloads);
            viewHolder.percentListened = (TextView) rowView
                    .findViewById(R.id.completed);

            rowView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) rowView.getTag();
        String episodeTitle = episodeTitles[position];
        String podcastTitle = podcastTitles[position];
        String pubDate = pubDates[position];
        String downloaded = downloadeds[position];
        String listened = listeneds[position];

        Log.d(DEBUG_TAG, "episodeTitle: " + episodeTitle + " "
                + episodeIds[position]);

        holder.episodeTitle.setText(episodeTitle);
        holder.episodeTitle.setTag(R.id.TAG_EPISODE_ID, episodeIds[position]);
        holder.episodeTitle.setTag(R.id.TAG_EPISODE_LINK_ID,
                episodeLinks[position]);
        holder.podcastTitle.setText(podcastTitle);
        holder.pubDate.setText(pubDate);
        if (downloaded.equals("1"))
            holder.inDownloads.setText("in Downloads");
        else
            holder.inDownloads.setText("");

        holder.percentListened.setText(listened);

        return rowView;
    }

    static class ViewHolder {
        public TextView episodeTitle;
        public TextView podcastTitle;
        public TextView pubDate;
        public TextView inDownloads;
        public TextView percentListened;
    }
}
